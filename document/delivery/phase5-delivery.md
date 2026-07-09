# Phase 5 Delivery Pack

This document is the delivery entry point for the mall optimization project. It links the runnable code, reproducible scripts, SQL changes, API notes, verification commands, and review material that prove the seckill and performance work.

## Project Positioning

The project extends `macrozheng/mall` with two engineering tracks:

- High-concurrency seckill ordering: baseline DB path, Redis Lua stock deduction, RabbitMQ async order creation, and consistency verification.
- Hot endpoint performance optimization: Redis + Caffeine cache for homepage/product detail/recommendation reads, order-list assembly optimization, and index changes for frequently used query paths.

## Key Deliverables

| Area | File |
| --- | --- |
| Baseline seckill design | `document/seckill-baseline.md` |
| Redis Lua MQ seckill design | `document/seckill-redis-lua-mq.md` |
| Seckill comparison report | `document/performance/seckill-phase1c-report.md` |
| Phase 2 performance report template | `document/performance/phase2-performance-report.md` |
| API documentation | `document/api/optimization-api.md` |
| Architecture and sequence diagrams | `document/architecture/optimization-architecture.md` |
| Local runbook | `document/delivery/local-runbook.md` |
| Demo script | `document/delivery/demo-script.md` |
| Interview review notes | `document/delivery/review-notes.md` |
| Phase 2 SQL indexes | `document/sql/phase2_performance_indexes.sql` |
| Seckill schema/reset/verify SQL | `document/sql/seckill_baseline_schema.sql`, `document/sql/seckill_baseline_reset.sql`, `document/sql/seckill_baseline_verify.sql`, `document/sql/seckill_redis_reset.sql` |
| JMeter scripts | `document/jmeter/seckill-baseline.jmx`, `document/jmeter/seckill-redis-lua-mq.jmx` |
| PowerShell scripts | `document/scripts/*.ps1` |

## Verification Commands

Compile the portal and dependent modules:

```powershell
mvn -pl mall-portal -am compile -DskipTests
```

Verify the delivery files are present:

```powershell
powershell -ExecutionPolicy Bypass -File document/scripts/verify_phase5_delivery.ps1
```

Verify Redis Lua MQ seckill consistency after a pressure run:

```powershell
powershell -ExecutionPolicy Bypass -File document/scripts/seckill_redis_verify.ps1 -RelationId 1 -InitialStock 100
```

Run phase 2 endpoint benchmark after `mall-portal` is running:

```powershell
powershell -ExecutionPolicy Bypass -File document/scripts/run_phase2_performance.ps1 -BaseUrl http://localhost:8085 -Requests 500 -Concurrency 50 -ProductId 26
```

## Evidence Already Produced

`document/performance/seckill-phase1c-report.md` records a same-machine comparison run:

| Version | Samples | JMeter Success | JMeter Errors | Avg ms | P95 ms | P99 ms | DB Success Orders |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Baseline DB sync | 5000 | 100 | 4900 | 442.1 | 929 | 976 | 100 |
| Redis Lua MQ | 5000 | 5000 | 0 | 6.75 | 15 | 28 | 100 |

Consistency assertions from that run:

```text
[PASS] successOrderCount <= initialStock: 100 <= 100
[PASS] duplicateOrderCount = 0: 0
[PASS] oversold = 0: 0
[PASS] initialStock - redisStock = successOrderCount: 100 - 0 = 100
[PASS] dbStockDecrement = successOrderCount: 100 = 100
```

## Known Limits

- Phase 2 performance report is delivered as a runnable template; real QPS/P95/P99 must be collected on the target machine because local middleware placement and dataset size materially affect results.
- Screenshots are intentionally not committed without a real local run. Use the demo script and benchmark commands to capture current screenshots from the user's own environment.
- The AI/Vue3 add-on phases are outside this phase 5 delivery pass.
