param(
    [string]$BaseUrl = "http://localhost:8085",
    [int]$Requests = 200,
    [int]$Concurrency = 20,
    [long]$ProductId = 26,
    [string]$BearerToken = ""
)

$ErrorActionPreference = "Stop"

function Invoke-EndpointBenchmark {
    param(
        [string]$Name,
        [string]$Url,
        [hashtable]$Headers
    )

    $latencies = [System.Collections.Concurrent.ConcurrentBag[double]]::new()
    $errors = [System.Collections.Concurrent.ConcurrentBag[string]]::new()
    $queue = [System.Collections.Concurrent.ConcurrentQueue[int]]::new()
    1..$Requests | ForEach-Object { $queue.Enqueue($_) }

    $started = Get-Date
    $workers = 1..$Concurrency | ForEach-Object {
        Start-ThreadJob -ScriptBlock {
            param($Queue, $Latencies, $Errors, $Url, $Headers)
            $item = 0
            while ($Queue.TryDequeue([ref]$item)) {
                $sw = [System.Diagnostics.Stopwatch]::StartNew()
                try {
                    Invoke-WebRequest -Method Get -Uri $Url -Headers $Headers -UseBasicParsing | Out-Null
                    $sw.Stop()
                    $Latencies.Add($sw.Elapsed.TotalMilliseconds)
                } catch {
                    $sw.Stop()
                    $Latencies.Add($sw.Elapsed.TotalMilliseconds)
                    $Errors.Add($_.Exception.Message)
                }
            }
        } -ArgumentList $queue, $latencies, $errors, $Url, $Headers
    }

    $workers | Wait-Job | Receive-Job | Out-Null
    $workers | Remove-Job
    $elapsed = ((Get-Date) - $started).TotalSeconds
    $values = @($latencies.ToArray() | Sort-Object)
    if ($values.Count -eq 0) {
        throw "No latency samples collected for $Name"
    }

    $avg = ($values | Measure-Object -Average).Average
    $p95Index = [Math]::Min($values.Count - 1, [Math]::Ceiling($values.Count * 0.95) - 1)
    $p99Index = [Math]::Min($values.Count - 1, [Math]::Ceiling($values.Count * 0.99) - 1)
    [PSCustomObject]@{
        Name = $Name
        Requests = $Requests
        Concurrency = $Concurrency
        Qps = [Math]::Round($Requests / $elapsed, 2)
        AvgMs = [Math]::Round($avg, 2)
        P95Ms = [Math]::Round($values[$p95Index], 2)
        P99Ms = [Math]::Round($values[$p99Index], 2)
        ErrorCount = $errors.Count
    }
}

$headers = @{}
if ($BearerToken) {
    $headers["Authorization"] = "Bearer $BearerToken"
}

$targets = @(
    @{ Name = "home-content"; Url = "$BaseUrl/home/content" },
    @{ Name = "product-detail"; Url = "$BaseUrl/product/detail/$ProductId" },
    @{ Name = "recommend-products"; Url = "$BaseUrl/home/recommendProductList?pageNum=1&pageSize=20" }
)

if ($BearerToken) {
    $targets += @{ Name = "order-list"; Url = "$BaseUrl/order/list?status=-1&pageNum=1&pageSize=10" }
}

$results = foreach ($target in $targets) {
    Invoke-EndpointBenchmark -Name $target.Name -Url $target.Url -Headers $headers
}

$results | Format-Table -AutoSize
