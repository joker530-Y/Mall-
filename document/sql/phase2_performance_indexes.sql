-- Phase 2 performance indexes.
-- Run once after importing document/sql/mall.sql. If an index already exists,
-- skip that ALTER statement or drop the duplicate manually first.

-- /home/content recommendation joins and sort.
ALTER TABLE sms_home_brand
    ADD INDEX idx_phase2_recommend_sort (recommend_status, sort, brand_id);

ALTER TABLE sms_home_new_product
    ADD INDEX idx_phase2_recommend_sort (recommend_status, sort, product_id);

ALTER TABLE sms_home_recommend_product
    ADD INDEX idx_phase2_recommend_sort (recommend_status, sort, product_id);

ALTER TABLE sms_home_recommend_subject
    ADD INDEX idx_phase2_recommend_sort (recommend_status, sort, subject_id);

ALTER TABLE sms_home_advertise
    ADD INDEX idx_phase2_type_status_sort (type, status, sort);

-- Flash promotion metadata used by /home/content and seckill warm data.
ALTER TABLE sms_flash_promotion
    ADD INDEX idx_phase2_status_date (status, start_date, end_date);

ALTER TABLE sms_flash_promotion_session
    ADD INDEX idx_phase2_time_range (start_time, end_time);

ALTER TABLE sms_flash_promotion_product_relation
    ADD INDEX idx_phase2_flash_session (flash_promotion_id, flash_promotion_session_id, product_id);

-- /product/detail/{id} aggregate queries.
ALTER TABLE pms_sku_stock
    ADD INDEX idx_phase2_product_id (product_id);

ALTER TABLE pms_product_attribute
    ADD INDEX idx_phase2_category_type_sort (product_attribute_category_id, type, sort);

ALTER TABLE pms_product_attribute_value
    ADD INDEX idx_phase2_product_attr (product_id, product_attribute_id);

ALTER TABLE pms_product_ladder
    ADD INDEX idx_phase2_product_id (product_id);

ALTER TABLE pms_product_full_reduction
    ADD INDEX idx_phase2_product_id (product_id);

ALTER TABLE sms_coupon_product_relation
    ADD INDEX idx_phase2_product_coupon (product_id, coupon_id);

ALTER TABLE sms_coupon_product_category_relation
    ADD INDEX idx_phase2_category_coupon (product_category_id, coupon_id);

-- /order/list paging and order item fan-out.
ALTER TABLE oms_order
    ADD INDEX idx_phase2_member_status_delete_time (member_id, status, delete_status, create_time, id);

ALTER TABLE oms_order
    ADD INDEX idx_phase2_member_delete_time (member_id, delete_status, create_time, id);

ALTER TABLE oms_order_item
    ADD INDEX idx_phase2_order_id_id (order_id, id);
