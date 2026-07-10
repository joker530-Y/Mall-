-- Phase 2 performance indexes (idempotent).
-- Safe to re-run: existing indexes are skipped via information_schema checks.

-- sms_home_brand
SET @exist := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sms_home_brand' AND index_name = 'idx_phase2_recommend_sort');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE sms_home_brand ADD INDEX idx_phase2_recommend_sort (recommend_status, sort, brand_id)', 'SELECT 1');
PREPARE stmt FROM @sqlstmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sms_home_new_product
SET @exist := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sms_home_new_product' AND index_name = 'idx_phase2_recommend_sort');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE sms_home_new_product ADD INDEX idx_phase2_recommend_sort (recommend_status, sort, product_id)', 'SELECT 1');
PREPARE stmt FROM @sqlstmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sms_home_recommend_product
SET @exist := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sms_home_recommend_product' AND index_name = 'idx_phase2_recommend_sort');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE sms_home_recommend_product ADD INDEX idx_phase2_recommend_sort (recommend_status, sort, product_id)', 'SELECT 1');
PREPARE stmt FROM @sqlstmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sms_home_recommend_subject
SET @exist := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sms_home_recommend_subject' AND index_name = 'idx_phase2_recommend_sort');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE sms_home_recommend_subject ADD INDEX idx_phase2_recommend_sort (recommend_status, sort, subject_id)', 'SELECT 1');
PREPARE stmt FROM @sqlstmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sms_home_advertise
SET @exist := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sms_home_advertise' AND index_name = 'idx_phase2_type_status_sort');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE sms_home_advertise ADD INDEX idx_phase2_type_status_sort (type, status, sort)', 'SELECT 1');
PREPARE stmt FROM @sqlstmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sms_flash_promotion
SET @exist := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sms_flash_promotion' AND index_name = 'idx_phase2_status_date');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE sms_flash_promotion ADD INDEX idx_phase2_status_date (status, start_date, end_date)', 'SELECT 1');
PREPARE stmt FROM @sqlstmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sms_flash_promotion_session
SET @exist := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sms_flash_promotion_session' AND index_name = 'idx_phase2_time_range');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE sms_flash_promotion_session ADD INDEX idx_phase2_time_range (start_time, end_time)', 'SELECT 1');
PREPARE stmt FROM @sqlstmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sms_flash_promotion_product_relation
SET @exist := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sms_flash_promotion_product_relation' AND index_name = 'idx_phase2_flash_session');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE sms_flash_promotion_product_relation ADD INDEX idx_phase2_flash_session (flash_promotion_id, flash_promotion_session_id, product_id)', 'SELECT 1');
PREPARE stmt FROM @sqlstmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- pms_sku_stock
SET @exist := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'pms_sku_stock' AND index_name = 'idx_phase2_product_id');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE pms_sku_stock ADD INDEX idx_phase2_product_id (product_id)', 'SELECT 1');
PREPARE stmt FROM @sqlstmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- pms_product_attribute
SET @exist := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'pms_product_attribute' AND index_name = 'idx_phase2_category_type_sort');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE pms_product_attribute ADD INDEX idx_phase2_category_type_sort (product_attribute_category_id, type, sort)', 'SELECT 1');
PREPARE stmt FROM @sqlstmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- pms_product_attribute_value
SET @exist := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'pms_product_attribute_value' AND index_name = 'idx_phase2_product_attr');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE pms_product_attribute_value ADD INDEX idx_phase2_product_attr (product_id, product_attribute_id)', 'SELECT 1');
PREPARE stmt FROM @sqlstmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- pms_product_ladder
SET @exist := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'pms_product_ladder' AND index_name = 'idx_phase2_product_id');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE pms_product_ladder ADD INDEX idx_phase2_product_id (product_id)', 'SELECT 1');
PREPARE stmt FROM @sqlstmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- pms_product_full_reduction
SET @exist := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'pms_product_full_reduction' AND index_name = 'idx_phase2_product_id');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE pms_product_full_reduction ADD INDEX idx_phase2_product_id (product_id)', 'SELECT 1');
PREPARE stmt FROM @sqlstmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sms_coupon_product_relation
SET @exist := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sms_coupon_product_relation' AND index_name = 'idx_phase2_product_coupon');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE sms_coupon_product_relation ADD INDEX idx_phase2_product_coupon (product_id, coupon_id)', 'SELECT 1');
PREPARE stmt FROM @sqlstmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sms_coupon_product_category_relation
SET @exist := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sms_coupon_product_category_relation' AND index_name = 'idx_phase2_category_coupon');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE sms_coupon_product_category_relation ADD INDEX idx_phase2_category_coupon (product_category_id, coupon_id)', 'SELECT 1');
PREPARE stmt FROM @sqlstmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- oms_order member/status paging
SET @exist := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'oms_order' AND index_name = 'idx_phase2_member_status_delete_time');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE oms_order ADD INDEX idx_phase2_member_status_delete_time (member_id, status, delete_status, create_time, id)', 'SELECT 1');
PREPARE stmt FROM @sqlstmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- oms_order member/delete paging
SET @exist := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'oms_order' AND index_name = 'idx_phase2_member_delete_time');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE oms_order ADD INDEX idx_phase2_member_delete_time (member_id, delete_status, create_time, id)', 'SELECT 1');
PREPARE stmt FROM @sqlstmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- oms_order_item
SET @exist := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'oms_order_item' AND index_name = 'idx_phase2_order_id_id');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE oms_order_item ADD INDEX idx_phase2_order_id_id (order_id, id)', 'SELECT 1');
PREPARE stmt FROM @sqlstmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;
