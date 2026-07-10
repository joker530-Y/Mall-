# Review Notes

## What Changed

- Added a baseline seckill endpoint for DB-only comparison.
- Added Redis Lua MQ seckill flow for high-concurrency ordering.
- Added JMeter scripts and PowerShell reset/verify scripts.
- Added a phase 1C pressure-test report with real local output.
- Added Redis + Caffeine two-level cache for read-heavy portal endpoints.
- Added SQL indexes for homepage, product detail, seckill metadata, and order-list queries.
- Added phase 2 benchmark script and report template.

## Important Correctness Points

- `successOrderCount <= initialStock` is the primary oversell check.
- `duplicateOrderCount = 0` proves member-level duplicate protection.
- `initialStock - redisStock = successOrderCount` checks Redis admission consistency.
- `dbStockDecrement = successOrderCount` checks DB persistence consistency.
- Product detail / homepage hot caches are invalidated from admin write paths by deleting shared Redis keys (`{redis.database}:hot:...`). Portal local Caffeine still expires within ~30s.
- RabbitMQ consumer logic must remain idempotent because delivery can be retried.
- Seckill publisher uses correlated confirms + mandatory returns; send failure triggers Redis compensation in `SeckillRedisServiceImpl`.
- Seckill consume failures are marked in Redis and rejected to `mall.seckill.order.dlq` (see `document/delivery/seckill-dlq-runbook.md`).

## Tradeoffs

- The seckill API returns queue status, not final order details, because DB order creation is asynchronous.
- Redis is the hot-path admission source during the activity; MySQL is the durable source after consumer persistence.
- Homepage content uses a minute-bucketed cache key because flash promotion sessions are time sensitive.
- Product detail cache can become stale after admin product edits; production hardening should add explicit invalidation from admin write paths.
- Phase 2 order-list optimization does not cache user order lists because order state changes frequently and correctness matters more than cache hit rate.

## Follow-up Hardening

- Optionally publish Redis pub/sub so portal instances evict local Caffeine immediately (today Redis delete + 30s local TTL).
- Optionally reuse admin JWT for `mall-search` write APIs (currently `X-Manage-Token` / `MALL_SEARCH_MANAGE_TOKEN`).
- Add dashboard screenshots after running the benchmark on a fixed local machine.
- Add CI smoke checks for compile and delivery-file presence.

## Resume Bullet Template

Use only numbers that have been measured locally:

```text
Based on Spring Boot 3.5 and JDK 21 mall system, implemented a high-concurrency seckill ordering flow with Redis Lua atomic stock deduction, member limit checks, RabbitMQ async order creation, idempotent DB persistence, and consistency verification scripts. Under a same-machine JMeter run with stock 100 and 5000 requests, the optimized path created 100 successful orders with zero oversell and zero duplicate member orders; average API latency was measured at 6.75 ms with P99 28 ms in the local test environment.
```
