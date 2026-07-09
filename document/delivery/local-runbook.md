# Local Runbook

## Prerequisites

- JDK 21
- Maven
- MySQL with database `mall`
- Redis
- RabbitMQ virtual host `/mall`, username `mall`, password `mall`
- Optional: MongoDB for read-history features
- Optional: JMeter for pressure tests

## Initialize Database

```powershell
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS mall DEFAULT CHARACTER SET utf8mb4"
Get-Content .\document\sql\mall.sql | mysql -uroot -proot -D mall
Get-Content .\document\sql\seckill_baseline_schema.sql | mysql -uroot -proot -D mall
Get-Content .\document\sql\phase2_performance_indexes.sql | mysql -uroot -proot -D mall
```

If MySQL reports that an index already exists, skip that `ALTER TABLE`.

## Start Services

Start local middleware first:

```text
MySQL:    localhost:3306 / root / root
Redis:    localhost:6379
RabbitMQ: localhost:5672 / vhost /mall / mall / mall
```

Compile:

```powershell
mvn -pl mall-portal -am compile -DskipTests
```

Run `mall-portal`:

```powershell
mvn -pl mall-portal spring-boot:run
```

Open Swagger UI:

```text
http://localhost:8085/swagger-ui.html
```

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
