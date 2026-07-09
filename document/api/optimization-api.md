# Optimization API Documentation

Base URL for local development: `http://localhost:8085`

Most order-related endpoints require a member JWT:

```http
Authorization: Bearer <member-jwt>
```

## Seckill Baseline

### Create Baseline Seckill Order

```http
POST /seckill/baseline/order
Content-Type: application/json
Authorization: Bearer <member-jwt>
```

Request:

```json
{
  "relationId": 1,
  "productSkuId": 110,
  "memberReceiveAddressId": 1,
  "quantity": 1,
  "payType": 0
}
```

Purpose:

- Direct DB comparison path for pressure testing.
- Uses `sms_flash_promotion_product_relation.flash_promotion_count`.
- Does not use Redis stock, Lua, RabbitMQ, or async queueing.

## Redis Lua MQ Seckill

### Warm Up Seckill Stock

```http
POST /seckill/redis/warmup/{relationId}
```

Effect:

- Loads seckill metadata and stock into Redis.
- Main stock key: `mall:seckill:stock:{relationId}`.

### Submit Seckill Order

```http
POST /seckill/redis/order
Content-Type: application/json
Authorization: Bearer <member-jwt>
```

Request:

```json
{
  "relationId": 1,
  "productSkuId": 110,
  "memberReceiveAddressId": 1,
  "quantity": 1,
  "payType": 0
}
```

Response meaning:

- `QUEUING`: Redis deduction succeeded and message was sent to RabbitMQ.
- `SOLD_OUT`: Redis stock was insufficient.
- `REPEAT`: member already ordered this seckill relation.
- `FAILED`: validation, queueing, or consumer path failed.

### Query Seckill Result

```http
GET /seckill/redis/result?relationId=1
Authorization: Bearer <member-jwt>
```

Purpose:

- Polls async order creation result from Redis.
- Returns order result after the consumer creates the DB order.

## Performance-Optimized Portal Reads

### Homepage Content

```http
GET /home/content
```

Optimization:

- Redis + Caffeine two-level cache.
- Minute-bucketed key to avoid stale seckill session display after session changes.

### Recommended Products

```http
GET /home/recommendProductList?pageNum=1&pageSize=20
```

Optimization:

- Redis + Caffeine page cache.
- Restored `PageHelper.startPage` call that was previously hidden by a malformed comment.

### Product Detail

```http
GET /product/detail/{id}
```

Optimization:

- Redis + Caffeine aggregate cache.
- Caches empty result briefly for invalid product IDs.
- Restored product attribute and coupon queries previously hidden by malformed comments.

### Order List

```http
GET /order/list?status=-1&pageNum=1&pageSize=10
Authorization: Bearer <member-jwt>
```

Optimization:

- Keeps DB paging semantics.
- Queries order items once for the current page.
- Groups items by `orderId` instead of repeatedly scanning the full item list for every order.
