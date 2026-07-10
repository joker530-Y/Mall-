-- Register seckill management APIs for dynamic authorization.
INSERT INTO `ums_resource` (`id`, `create_time`, `name`, `url`, `description`, `category_id`)
SELECT 33, NOW(), '秒杀运营管理', '/seckill/manage/**', '秒杀预热、看板与订单日志', 3
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ums_resource` WHERE `id` = 33 OR `url` = '/seckill/manage/**');

INSERT INTO `ums_role_resource_relation` (`role_id`, `resource_id`)
SELECT 5, 33
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `ums_role_resource_relation` WHERE `role_id` = 5 AND `resource_id` = 33
);
