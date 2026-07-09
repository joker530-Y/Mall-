param(
    [int]$Threads = 500,
    [int]$Loops = 10,
    [int]$RampSeconds = 30,
    [int]$RelationId = 1,
    [int]$InitialStock = 100
)

$repo = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$totalRequests = $Threads * $Loops
$resultDir = Join-Path $repo ("target\seckill-phase1c\" + (Get-Date -Format "yyyyMMdd-HHmmss"))
$csvPath = Join-Path $repo "document\jmeter\seckill-members.csv"
New-Item -ItemType Directory -Path $resultDir -Force | Out-Null

& (Join-Path $PSScriptRoot 'prepare_seckill_perf_users.ps1') -Count $totalRequests -CsvPath "document\jmeter\seckill-members.csv"

function Invoke-JMeterPlan($Name, $JmxPath, $JtlPath) {
    & 'D:\JMeter\bin\jmeter.bat' -n `
        -t $JmxPath `
        "-Jthreads=$Threads" `
        "-Jloops=$Loops" `
        "-JrampSeconds=$RampSeconds" `
        "-JrelationId=$RelationId" `
        -JproductSkuId=110 `
        "-JcsvFile=$csvPath" `
        -l $JtlPath
    if ($LASTEXITCODE -ne 0) {
        throw "$Name JMeter run failed"
    }
}

function Get-JtlMetrics($JtlPath, $Label) {
    $rows = @(Import-Csv $JtlPath | Where-Object { $_.label -eq $Label })
    $elapsed = @($rows | ForEach-Object { [int]$_.elapsed } | Sort-Object)
    $count = $elapsed.Count
    $success = @($rows | Where-Object { $_.success -eq 'true' }).Count
    $errors = $count - $success
    $avg = [math]::Round(($elapsed | Measure-Object -Average).Average, 2)
    $p95 = $elapsed[[math]::Min($count - 1, [math]::Ceiling($count * 0.95) - 1)]
    $p99 = $elapsed[[math]::Min($count - 1, [math]::Ceiling($count * 0.99) - 1)]
    [pscustomobject]@{
        samples = $count
        success = $success
        errors = $errors
        avg_ms = $avg
        p95_ms = $p95
        p99_ms = $p99
    }
}

function Get-DbSuccessCount($Prefix) {
    $query = "SELECT COUNT(*) FROM oms_order WHERE order_type=1 AND delete_status=0 AND promotion_info=CONCAT('$Prefix relationId=', $RelationId)"
    [int](mysql -uroot -proot -D mall -N -B -e $query)
}

$baselineJtl = Join-Path $resultDir 'baseline.jtl'
$redisJtl = Join-Path $resultDir 'redis-lua-mq.jtl'

Get-Content -Path (Join-Path $repo 'document\sql\seckill_baseline_reset.sql') | mysql -uroot -proot -D mall
Invoke-JMeterPlan "baseline" (Join-Path $repo 'document\jmeter\seckill-baseline.jmx') $baselineJtl
$baselineMetrics = Get-JtlMetrics $baselineJtl 'baseline seckill order'
$baselineOrders = Get-DbSuccessCount 'baseline-seckill'

& (Join-Path $PSScriptRoot 'seckill_redis_reset.ps1') -RelationId $RelationId
$login = Invoke-RestMethod -Method Post -Uri 'http://localhost:8085/sso/login?username=test&password=123456'
$headers = @{ Authorization = "$($login.data.tokenHead)$($login.data.token)" }
Invoke-RestMethod -Method Post -Uri "http://localhost:8085/seckill/redis/warmup/$RelationId" -Headers $headers | Out-Null
Invoke-JMeterPlan "redis-lua-mq" (Join-Path $repo 'document\jmeter\seckill-redis-lua-mq.jmx') $redisJtl
Start-Sleep -Seconds 5
$redisMetrics = Get-JtlMetrics $redisJtl 'redis seckill order'
$redisOrders = Get-DbSuccessCount 'redis-seckill'
$redisVerify = & (Join-Path $PSScriptRoot 'seckill_redis_verify.ps1') -RelationId $RelationId -InitialStock $InitialStock

$reportPath = Join-Path $repo 'document\performance\seckill-phase1c-report.md'
New-Item -ItemType Directory -Path (Split-Path -Parent $reportPath) -Force | Out-Null

$report = @"
# Phase 1C Seckill Comparison

Generated: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

Environment:
- JMeter, mall-portal, MySQL, Redis, RabbitMQ, MongoDB on same Windows host
- Threads: $Threads
- Loops: $Loops
- Total order requests per run: $totalRequests
- Initial seckill stock: $InitialStock
- Relation ID: $RelationId

| Version | Samples | JMeter Success | JMeter Errors | Avg ms | P95 ms | P99 ms | DB Success Orders |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Baseline DB sync | $($baselineMetrics.samples) | $($baselineMetrics.success) | $($baselineMetrics.errors) | $($baselineMetrics.avg_ms) | $($baselineMetrics.p95_ms) | $($baselineMetrics.p99_ms) | $baselineOrders |
| Redis Lua MQ | $($redisMetrics.samples) | $($redisMetrics.success) | $($redisMetrics.errors) | $($redisMetrics.avg_ms) | $($redisMetrics.p95_ms) | $($redisMetrics.p99_ms) | $redisOrders |

Notes:
- JMeter Success means the seckill order API response matched the scenario assertion.
- Redis Lua MQ accepts sold-out and queued decisions with HTTP/API success; DB Success Orders is the final successful order count.

Redis Lua MQ assertions:

~~~text
$($redisVerify -join "`n")
~~~

Artifacts:
- Baseline JTL: $baselineJtl
- Redis Lua MQ JTL: $redisJtl
"@

$report | Set-Content -Path $reportPath -Encoding UTF8
Write-Output $report
Write-Output "Report written: $reportPath"
