# 用户端模拟下单演示

本链路用于证明项目不仅是秒杀接口集合，而是具备完整的会员、购物车、订单与支付状态流转。

## 前置条件

- `mall-portal` 已启动（默认 `http://localhost:8085`）
- MySQL / Redis / RabbitMQ 已按 `document/delivery/local-runbook.md` 启动
- 演示账号：`test` / `123456`（BCrypt 加密密码，客户端按原项目约定提交）

## 方式一：Postman 一键演示

1. 导入 `document/postman/mall-portal.postman_collection.json`
2. 设置变量 `portal.mall = http://localhost:8085`
3. 按顺序执行：
   - 会员登录
   - 加入购物车
   - 根据购物车信息生成确认单
   - 根据购物车信息生成订单（记录返回的 `order.id` 到变量 `orderId`）
   - 模拟支付订单（`POST /order/mock-pay/{{orderId}}`）
   - 根据 ID 获取订单详情
   - 用户取消订单 或 用户确认收货

## 方式二：PowerShell 脚本

```powershell
powershell -ExecutionPolicy Bypass -File document/scripts/user_order_demo.ps1
```

脚本会依次完成：登录 → 加购 → 下单 → 模拟支付 → 查询订单详情。

## 方式三：后台秒杀 + 用户秒杀组合演示

1. 管理员登录 Vue 控制台（`mall-admin-vue3`）
2. 在「活动与预热」执行库存预热
3. 在「订单结果」观察 Redis/MQ 异步落库日志
4. 用 Postman 或 `document/scripts/seckill_redis_verify.ps1` 提交秒杀请求
5. 切换回用户模拟下单链路，展示普通订单与秒杀订单可并存

## 演示注意点

- 支付为**模拟支付**，金额始终来自数据库订单，不接受前端传参
- 验证码为演示模式固定值 `123456`，且不在接口响应中返回随机码
- 注册/找回密码默认关闭；本地 `dev` profile 可开启用于演示注册流程
