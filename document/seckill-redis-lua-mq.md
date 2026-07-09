# Phase 1B Redis Lua MQ Seckill

## Endpoints

- `POST /seckill/redis/warmup/{relationId}`
- `POST /seckill/redis/order`
- `GET /seckill/redis/result?relationId=1`

Authenticated order body:

```json
{
  "relationId": 1,
  "productSkuId": 110,
  "memberReceiveAddressId": 1,
  "quantity": 1,
  "payType": 0
}
```

## Flow

1. Warmup copies `sms_flash_promotion_product_relation.flash_promotion_count` to `mall:seckill:stock:{relationId}`.
2. Order submit runs a Redis Lua script that checks stock and per-member limit, then decrements stock atomically.
3. A successful Lua result sends `SeckillOrderMessage` to RabbitMQ queue `mall.seckill.order`.
4. The consumer idempotently creates `oms_order` and `oms_order_item`, decrements DB seckill stock, and writes Redis result.
5. Clients poll `/seckill/redis/result`.

## Local Reset

```powershell
.\document\scripts\seckill_redis_reset.ps1 -RelationId 1
```

Then call:

```powershell
POST http://localhost:8085/seckill/redis/warmup/1
```

## Verification

```powershell
.\document\scripts\seckill_redis_verify.ps1 -RelationId 1 -InitialStock 100
```

Expected assertions:

- success order count is not greater than initial stock
- duplicate member orders are zero
- Redis stock decrement equals success order count
- DB stock decrement equals success order count

## Current Scope

This phase delivers the Redis + Lua + MQ order creation loop. Tokenized hidden paths and timeout cancel compensation are intentionally left for a later hardening pass.
