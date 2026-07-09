# Phase 1A Baseline Seckill

## Endpoint

`POST /seckill/baseline/order`

Authenticated member request body:

```json
{
  "relationId": 1,
  "productSkuId": 110,
  "memberReceiveAddressId": 1,
  "quantity": 1,
  "payType": 0
}
```

## Baseline Rules

- Stock model: `sms_flash_promotion_product_relation.flash_promotion_count`.
- Deduction: single SQL conditional update, `flash_promotion_count >= quantity`.
- No Redis stock, Lua script, RabbitMQ queue, or async worker in this endpoint.
- One order per member per `relationId` is enforced by `sms_seckill_order_log.uk_member_relation`.

## Local Reset

Run from the repository root:

```powershell
mysql -uroot -proot -D mall -e "SOURCE document/sql/seckill_baseline_reset.sql"
```

The default reset prepares `relationId=1`, stock `100`, sample `productSkuId=110`, and deletes previous baseline orders for that relation.
If the repository path contains spaces, run `Get-Content .\document\sql\seckill_baseline_reset.sql | mysql -uroot -proot -D mall` from the repository root.

## Verification

```powershell
mysql -uroot -proot -D mall -e "SOURCE document/sql/seckill_baseline_verify.sql"
```

Expected result after a valid run:

- `oversold = NO`
- `consistency = PASS`
- the duplicate-member query returns no rows

## JMeter

Use `document/jmeter/seckill-baseline.jmx`.

Default variables:

- `host=localhost`
- `port=8085`
- `username=test`
- `password=123456`
- `relationId=1`
- `productSkuId=110`
- `addressId=1`
- `threads=500`
- `loops=10`

For stock-100 baseline runs, use multiple authenticated test members or change the login sampler/user variable source; the default `test` account validates the limit path and should create only one successful order.

CLI example:

```powershell
D:\JMeter\bin\jmeter.bat -n -t .\document\jmeter\seckill-baseline.jmx -Jthreads=10 -Jloops=1 -JrampSeconds=1 -l .\target\seckill-baseline-smoke.jtl
```
