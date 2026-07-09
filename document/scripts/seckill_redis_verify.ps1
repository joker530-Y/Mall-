param(
    [int]$RelationId = 1,
    [int]$InitialStock = 100
)

$redisStockRaw = redis-cli --raw GET "mall:seckill:stock:$RelationId"
if ([string]::IsNullOrWhiteSpace($redisStockRaw)) {
    $redisStock = -1
} else {
    $redisStock = [int]$redisStockRaw
}

$query = @"
SELECT
  r.flash_promotion_count,
  COUNT(DISTINCT o.id) AS success_orders,
  (
    SELECT COUNT(*)
    FROM (
      SELECT o2.member_id
      FROM oms_order o2
      WHERE o2.order_type = 1
        AND o2.delete_status = 0
        AND o2.promotion_info = CONCAT('redis-seckill relationId=', $RelationId)
      GROUP BY o2.member_id
      HAVING COUNT(*) > 1
    ) dup
  ) AS duplicate_members
FROM sms_flash_promotion_product_relation r
LEFT JOIN oms_order o
  ON o.order_type = 1
 AND o.delete_status = 0
 AND o.promotion_info = CONCAT('redis-seckill relationId=', r.id)
WHERE r.id = $RelationId
GROUP BY r.flash_promotion_count;
"@

$row = mysql -uroot -proot -D mall -N -B -e $query
$parts = $row -split "`t"
$dbRemaining = [int]$parts[0]
$successOrders = [int]$parts[1]
$duplicateMembers = [int]$parts[2]
$dbDecrement = $InitialStock - $dbRemaining
$oversold = [Math]::Max($successOrders - $InitialStock, 0)
$redisDecrement = $InitialStock - $redisStock

function Assert-Line($Name, $Passed, $Detail) {
    if ($Passed) {
        "[PASS] $Name`: $Detail"
    } else {
        "[FAIL] $Name`: $Detail"
    }
}

Assert-Line "successOrderCount <= initialStock" ($successOrders -le $InitialStock) "$successOrders <= $InitialStock"
Assert-Line "duplicateOrderCount = 0" ($duplicateMembers -eq 0) "$duplicateMembers"
Assert-Line "oversold = 0" ($oversold -eq 0) "$oversold"
Assert-Line "initialStock - redisStock = successOrderCount" ($redisDecrement -eq $successOrders) "$InitialStock - $redisStock = $successOrders"
Assert-Line "dbStockDecrement = successOrderCount" ($dbDecrement -eq $successOrders) "$dbDecrement = $successOrders"
