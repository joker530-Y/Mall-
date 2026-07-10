# Local Runbook

## Prerequisites

- JDK 21
- Maven
- MySQL 8.0 with database `mall`
- Redis 7
- RabbitMQ 3.13+ virtual host `/mall`, username `mall`, password `mall`
- Optional: Elasticsearch 8.x for `mall-search` (Spring Boot 3.5 Java API Client)
- Optional: MongoDB 6 for read-history features
- Optional: JMeter for pressure tests

## Initialize Database

```powershell
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS mall DEFAULT CHARACTER SET utf8mb4"
Get-Content .\document\sql\mall.sql | mysql -uroot -proot -D mall
Get-Content .\document\sql\seckill_baseline_schema.sql | mysql -uroot -proot -D mall
Get-Content .\document\sql\phase2_performance_indexes.sql | mysql -uroot -proot -D mall
Get-Content .\document\sql\migrations\add_order_sn_unique_index.sql | mysql -uroot -proot -D mall
Get-Content .\document\sql\migrations\add_order_request_idempotency.sql | mysql -uroot -proot -D mall
Get-Content .\document\sql\migrations\add_seckill_manage_resource.sql | mysql -uroot -proot -D mall
```

If MySQL reports that an index already exists, skip that `ALTER TABLE`.

## Start Services

Start local middleware first (or `document/docker/docker-compose-env.yml`):

```text
MySQL:         localhost:3306 / root / root
Redis:         localhost:6379
RabbitMQ:      localhost:5672 / vhost /mall / mall / mall
Elasticsearch: localhost:9200 (8.x, security disabled in compose)
```

Compile:

```powershell
mvn -pl mall-portal -am compile -DskipTests
```

Run backends:

```powershell
mvn -pl mall-admin spring-boot:run    # http://localhost:8080
mvn -pl mall-portal spring-boot:run   # http://localhost:8085
# optional search (set MALL_SEARCH_MANAGE_TOKEN first)
$env:MALL_SEARCH_MANAGE_TOKEN="change-me-search-manage-token"
mvn -pl mall-search spring-boot:run   # http://localhost:8081
```

Run frontends:

```powershell
cd mall-admin-vue3
npm install
npm run dev                           # http://localhost:5173

cd ../mall-portal-vue3
npm install
npm run dev                           # http://localhost:5174
```

Open Swagger UI:

```text
http://localhost:8085/swagger-ui.html
```

## Search write APIs

`mall-search` 公开检索接口无需登录。导入/创建/删除索引需请求头：

```http
X-Manage-Token: <MALL_SEARCH_MANAGE_TOKEN>
```

示例：

```powershell
curl -X POST "http://localhost:8081/esProduct/importAll" -H "X-Manage-Token: change-me-search-manage-token"
```

未配置 `MALL_SEARCH_MANAGE_TOKEN` 时写接口一律拒绝。

## Docker (optional)

Use `document/docker/docker-compose-env.yml` for middleware and `document/docker/docker-compose-app.yml` for app images. Copy the root `.env.example` to `document/docker/.env` and fill in secrets before `docker compose up`.

Compose 已与本地/CI 对齐：MySQL 8.0、RabbitMQ `/mall`+`mall`/`mall`、Elasticsearch 8.15、Mongo 6。

## Seckill Reset And Warmup

Baseline reset:

```powershell
Get-Content .\document\sql\seckill_baseline_reset.sql | mysql -uroot -proot -D mall
```

Redis Lua MQ reset:

```powershell
powershell -ExecutionPolicy Bypass -File document/scripts/seckill_redis_reset.ps1 -RelationId 1
```

Warmup:

```http
POST http://localhost:8085/seckill/redis/warmup/1
```

## Pressure Test

Run phase 1C comparison:

```powershell
powershell -ExecutionPolicy Bypass -File document/scripts/run_seckill_phase1c.ps1
```

Run phase 2 endpoint benchmark:

```powershell
powershell -ExecutionPolicy Bypass -File document/scripts/run_phase2_performance.ps1 -BaseUrl http://localhost:8085 -Requests 500 -Concurrency 50 -ProductId 26
```

## Consistency Check

```powershell
powershell -ExecutionPolicy Bypass -File document/scripts/seckill_redis_verify.ps1 -RelationId 1 -InitialStock 100
```

Expected output should contain only `[PASS]` lines.

## Troubleshooting

### Portal login shows `Request failed with status code 403`

Usually Spring CORS rejecting `Origin: http://localhost:5174`. Confirm:

1. `mall-portal` `cors.allowed-origins` includes `http://localhost:5174` and `http://127.0.0.1:5174`
2. Vite proxy for `/api/portal` strips the forwarded `Origin` header
3. Restart `mall-portal` and `mall-portal-vue3` after changing CORS/proxy config
4. Hard-refresh the browser (Ctrl+F5)

PowerShell calls without an `Origin` header may still return 200 while the browser fails—always verify with the browser Network tab.

### Product detail: add-to-cart / buy buttons stay disabled

SKU specs are stored in `pms_sku_stock.sp_data` JSON. The portal UI must parse `spData` to build color/capacity options and select a stocked SKU. If buttons stay disabled, check that the product has SKUs with stock and that the frontend build includes the `spData` matching logic (`mall-portal-vue3/src/utils/sku.ts`).
