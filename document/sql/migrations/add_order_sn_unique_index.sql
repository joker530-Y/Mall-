-- Add unique index on oms_order.order_sn to prevent duplicate order numbers.
-- Run once on existing databases: mysql -u root -p mall < document/sql/migrations/add_order_sn_unique_index.sql

ALTER TABLE `oms_order`
    ADD UNIQUE INDEX `uk_order_sn` (`order_sn`);
