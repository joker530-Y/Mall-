param(
    [int]$RelationId = 1
)

$repo = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$redisPattern = "mall:seckill:*:$RelationId*"

Get-Content -Path (Join-Path $repo 'document\sql\seckill_redis_reset.sql') | mysql -uroot -proot -D mall

$keys = redis-cli --raw KEYS $redisPattern
foreach ($key in $keys) {
    if ($key) {
        redis-cli DEL $key | Out-Null
    }
}

Write-Output "Redis keys cleared by pattern: $redisPattern"

if (Test-Path 'D:\RabbitMQ\rabbitmq-server-4.3.2\sbin\rabbitmqctl.bat') {
    $rabbitProcess = Start-Process `
        -FilePath 'D:\RabbitMQ\rabbitmq-server-4.3.2\sbin\rabbitmqctl.bat' `
        -ArgumentList @('-q', 'purge_queue', '-p', '/mall', 'mall.seckill.order') `
        -WindowStyle Hidden `
        -PassThru

    if ($rabbitProcess.WaitForExit(15000)) {
        Write-Output "RabbitMQ queue purged: mall.seckill.order"
    } else {
        Stop-Process -Id $rabbitProcess.Id -Force
        Write-Warning "RabbitMQ queue purge timed out; continuing after database and Redis reset"
    }
}
