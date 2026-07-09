# Demo Script

Use this sequence for a 3 to 5 minute local demo.

## 1. Show Project Scope

Open:

- `document/delivery/phase5-delivery.md`
- `document/architecture/optimization-architecture.md`

Explain:

- The project keeps the original mall business modules.
- The optimization work focuses on seckill order correctness and hot endpoint performance.

## 2. Show Seckill Baseline

Open:

- `document/seckill-baseline.md`
- `document/jmeter/seckill-baseline.jmx`

Explain:

- Baseline is intentionally direct DB synchronous deduction.
- It exists to make the before/after comparison fair.

## 3. Show Redis Lua MQ Flow

Open:

- `document/seckill-redis-lua-mq.md`
- `mall-portal/src/main/java/com/macro/mall/portal/service/impl/SeckillRedisServiceImpl.java`
- `mall-portal/src/main/java/com/macro/mall/portal/component/SeckillOrderReceiver.java`

Run or show:

```powershell
powershell -ExecutionPolicy Bypass -File document/scripts/seckill_redis_verify.ps1 -RelationId 1 -InitialStock 100
```

Key point:

- Redis Lua decides admission atomically.
- RabbitMQ decouples DB order creation.
- The verification script proves zero oversell and no duplicate member orders.

## 4. Show Performance Optimization

Open:

- `mall-portal/src/main/java/com/macro/mall/portal/component/HotDataCache.java`
- `mall-portal/src/main/java/com/macro/mall/portal/service/impl/HomeServiceImpl.java`
- `mall-portal/src/main/java/com/macro/mall/portal/service/impl/PmsPortalProductServiceImpl.java`
- `mall-portal/src/main/java/com/macro/mall/portal/service/impl/OmsPortalOrderServiceImpl.java`
- `document/sql/phase2_performance_indexes.sql`

Explain:

- Homepage and product detail are read-heavy and cacheable.
- Order list remains DB-backed but avoids unnecessary Java-side repeated scans.
- SQL indexes target actual predicates and sort columns used by the endpoints.

## 5. Show Evidence

Open:

- `document/performance/seckill-phase1c-report.md`
- `document/performance/phase2-performance-report.md`

Explain:

- Phase 1C has recorded local output.
- Phase 2 report is prepared for real measurement on the current machine; do not invent QPS numbers before running it.

## 6. Final Talking Points

- Why Redis Lua instead of DB lock: faster admission control and less DB contention under flash-sale traffic.
- How duplicate orders are prevented: Redis limit path plus DB idempotency log.
- How Redis and DB consistency is checked: verification script compares Redis stock, DB stock decrement, order count, and duplicate member count.
- How cache risks are handled: local cache, Redis cache, randomized TTL, null cache, rebuild lock, and Redis failure fallback.
