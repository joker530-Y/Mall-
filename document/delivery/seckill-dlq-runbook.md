# Seckill DLQ Runbook

秒杀异步下单队列：`mall.seckill.order`  
死信队列：`mall.seckill.order.dlq`（exchange `mall.seckill.direct.dlx`）

## 何时进入 DLQ

消费 `mall.seckill.order` 失败时，门户会：

1. 调用 `markConsumeFailed` 写入 Redis 失败状态（用户侧可感知 FAILED）
2. 抛出 `AmqpRejectAndDontRequeueException`，消息进入 DLQ（不重回主队列）

发送侧已启用 publisher confirm；confirm/nack/超时会回补 Redis 库存，不会把“未入队”消息丢进 DLQ。

## 升级注意

若本地已存在旧版 `mall.seckill.order`（无 DLX 参数），Spring 启动可能因队列参数不一致失败。处理：

1. 打开 RabbitMQ Management：`http://localhost:15672`（`mall` / `mall`，vhost `/mall`）
2. 删除队列 `mall.seckill.order`（确认无积压或已备份）
3. 重启 `mall-portal`，由 `RabbitMqConfig` 重建主队列与 DLQ

## 排查

Management UI → Queues → `mall.seckill.order.dlq`：

- 查看 Ready 消息数与 payload（`SeckillOrderMessage` JSON）
- 对照 Redis 结果 key / 日志中的 `requestId`

## 重放（按需）

消费逻辑幂等：同一 `requestId` 重复消费不会重复建单。重放前确认失败原因已修复（DB、库存、地址等）。

### 方式 A：Management UI

1. 打开 `mall.seckill.order.dlq` → Get messages / Move messages
2. 将消息移动到 `mall.seckill.order`（routing key `mall.seckill.order`）
3. 观察门户日志与 Redis/DB 订单结果

### 方式 B：rabbitmqadmin（示例）

```bash
# 在 vhost /mall 下，将 DLQ 头一条发布回主交换（具体参数按环境调整）
rabbitmqadmin -V /mall -u mall -p mall get queue=mall.seckill.order.dlq count=1 ackmode=ack_requeue_false
# 再 publish 到 mall.seckill.direct / routing_key=mall.seckill.order
```

生产环境建议：先落审计日志，再批量重放；不要在未定位根因时盲目清空 DLQ。
