-- Add unique index on oms_order.order_sn to prevent duplicate order numbers.
-- Idempotent: safe to run multiple times.

SET @exist := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'oms_order'
    AND index_name = 'uk_order_sn'
);
SET @sqlstmt := IF(
  @exist = 0,
  'ALTER TABLE `oms_order` ADD UNIQUE INDEX `uk_order_sn` (`order_sn`)',
  'SELECT ''uk_order_sn already exists'''
);
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
