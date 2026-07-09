SET @relation_id := 1;
SET @stock := 100;

CREATE TABLE IF NOT EXISTS `sms_seckill_order_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `request_id` varchar(64) DEFAULT NULL,
  `member_id` bigint NOT NULL,
  `relation_id` bigint NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 processing, 1 success, 2 failed',
  `fail_reason` varchar(255) DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_relation` (`member_id`,`relation_id`),
  KEY `idx_relation_status` (`relation_id`,`status`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Baseline seckill order log';

DELETE oi
FROM oms_order_item oi
JOIN oms_order o ON o.id = oi.order_id
WHERE o.order_type = 1
  AND oi.promotion_name = CONCAT('redis-seckill relationId=', @relation_id);

DELETE o
FROM oms_order o
LEFT JOIN oms_order_item oi ON oi.order_id = o.id
WHERE o.order_type = 1
  AND (
    o.promotion_info = CONCAT('redis-seckill relationId=', @relation_id)
    OR oi.promotion_name = CONCAT('redis-seckill relationId=', @relation_id)
  );

DELETE FROM sms_seckill_order_log WHERE relation_id = @relation_id;

UPDATE sms_flash_promotion_product_relation
SET flash_promotion_count = @stock,
    flash_promotion_limit = 1
WHERE id = @relation_id;

SELECT
  r.id AS relation_id,
  r.product_id,
  s.id AS sample_sku_id,
  r.flash_promotion_count AS ready_stock,
  r.flash_promotion_limit AS per_member_limit
FROM sms_flash_promotion_product_relation r
JOIN pms_sku_stock s ON s.product_id = r.product_id
WHERE r.id = @relation_id
ORDER BY s.id
LIMIT 1;
