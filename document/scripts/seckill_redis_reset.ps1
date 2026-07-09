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
    & 'D:\RabbitMQ\rabbitmq-server-4.3.2\sbin\rabbitmqctl.bat' -q purge_queue -p /mall mall.seckill.order | Out-Null
    Write-Output "RabbitMQ queue purged: mall.seckill.order"
}
