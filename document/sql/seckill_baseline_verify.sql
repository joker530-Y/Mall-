SET @relation_id := 1;
SET @initial_stock := 100;

SELECT
  r.id AS relation_id,
  r.flash_promotion_count AS remaining_stock,
  @initial_stock - r.flash_promotion_count AS deducted_stock,
  COUNT(DISTINCT l.id) AS success_logs,
  COUNT(DISTINCT o.id) AS created_orders,
  COUNT(DISTINCT o.member_id) AS unique_members,
  CASE WHEN r.flash_promotion_count < 0 THEN 'YES' ELSE 'NO' END AS oversold,
  CASE
    WHEN (@initial_stock - r.flash_promotion_count) = COUNT(DISTINCT l.id)
     AND COUNT(DISTINCT l.id) = COUNT(DISTINCT o.id)
    THEN 'PASS'
    ELSE 'CHECK'
  END AS consistency
FROM sms_flash_promotion_product_relation r
LEFT JOIN sms_seckill_order_log l ON l.relation_id = r.id AND l.status = 1
LEFT JOIN oms_order o ON o.id = l.order_id AND o.order_type = 1 AND o.delete_status = 0
WHERE r.id = @relation_id
GROUP BY r.id, r.flash_promotion_count;

SELECT
  member_id,
  relation_id,
  COUNT(*) AS order_count
FROM sms_seckill_order_log
WHERE relation_id = @relation_id
GROUP BY member_id, relation_id
HAVING COUNT(*) > 1;
