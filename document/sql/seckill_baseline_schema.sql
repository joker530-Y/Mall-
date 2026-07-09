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
