# Phase 2 Performance Optimization Report

## Scope

- `/home/content`
- `/product/detail/{id}`
- `/home/recommendProductList`
- `/order/list`

## Code Changes

- Added Redis + Caffeine two-level hot-data cache for homepage, recommendation list, product category tree, and product detail.
- Added randomized Redis TTL and short null-value cache to reduce cache avalanche and penetration.
- Added Redis rebuild lock for hot cache misses; if Redis is unavailable, the service falls back to local cache or database load.
- Optimized `/order/list` assembly from repeated item-list scans to one `orderId -> items` grouping map.
- Restored previously commented-out product detail coupon and attribute query lines caused by malformed comments.

## SQL Changes

Apply:

```powershell
mysql -uroot -proot mall < document/sql/phase2_performance_indexes.sql
```

Main index targets:

- Homepage recommendation relation filters and `sort` ordering.
- Product detail fan-out tables by `product_id`.
- Order list paging by `member_id`, `status`, `delete_status`, and `create_time`.
- Order item lookup by `order_id`.

## Benchmark Command

Start `mall-portal`, then run:

```powershell
powershell -ExecutionPolicy Bypass -File document/scripts/run_phase2_performance.ps1 `
  -BaseUrl http://localhost:8085 `
  -Requests 500 `
  -Concurrency 50 `
  -ProductId 26
```

For `/order/list`, pass a member token:

```powershell
powershell -ExecutionPolicy Bypass -File document/scripts/run_phase2_performance.ps1 `
  -BaseUrl http://localhost:8085 `
  -Requests 500 `
  -Concurrency 50 `
  -ProductId 26 `
  -BearerToken "<member-jwt>"
```

## Result Table

Record real output here after running against a fixed dataset and machine.

| Endpoint | Version | Requests | Concurrency | QPS | Avg ms | P95 ms | P99 ms | Errors |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `/home/content` | before |  |  |  |  |  |  |  |
| `/home/content` | after |  |  |  |  |  |  |  |
| `/product/detail/26` | before |  |  |  |  |  |  |  |
| `/product/detail/26` | after |  |  |  |  |  |  |  |
| `/home/recommendProductList` | before |  |  |  |  |  |  |  |
| `/home/recommendProductList` | after |  |  |  |  |  |  |  |
| `/order/list` | before |  |  |  |  |  |  |  |
| `/order/list` | after |  |  |  |  |  |  |  |

## Notes

- Do not compare results across different machines or different middleware placement.
- Warm caches before measuring the cached version; clear `mall:hot:*` keys before measuring cold-cache behavior.
- For `/order/list`, use the same member account, page parameters, and data volume before and after.
