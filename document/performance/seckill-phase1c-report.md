# Phase 1C Seckill Comparison

Generated: 2026-07-09 11:41:42

Environment:
- JMeter, mall-portal, MySQL, Redis, RabbitMQ, MongoDB on same Windows host
- Threads: 500
- Loops: 10
- Total order requests per run: 5000
- Initial seckill stock: 100
- Relation ID: 1

| Version | Samples | JMeter Success | JMeter Errors | Avg ms | P95 ms | P99 ms | DB Success Orders |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Baseline DB sync | 5000 | 100 | 4900 | 442.1 | 929 | 976 | 100 |
| Redis Lua MQ | 5000 | 5000 | 0 | 6.75 | 15 | 28 | 100 |

Notes:
- JMeter Success means the seckill order API response matched the scenario assertion.
- Redis Lua MQ accepts sold-out and queued decisions with HTTP/API success; DB Success Orders is the final successful order count.

Redis Lua MQ assertions:

~~~text
[PASS] successOrderCount <= initialStock: 100 <= 100
[PASS] duplicateOrderCount = 0: 0
[PASS] oversold = 0: 0
[PASS] initialStock - redisStock = successOrderCount: 100 - 0 = 100
[PASS] dbStockDecrement = successOrderCount: 100 = 100
~~~

Artifacts:
- Baseline JTL: D:\AI coding project\mall-master\target\seckill-phase1c\20260709-113724\baseline.jtl
- Redis Lua MQ JTL: D:\AI coding project\mall-master\target\seckill-phase1c\20260709-113724\redis-lua-mq.jtl
