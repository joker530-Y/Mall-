# Phase 4 AI Customer Service RAG

阶段 4 在 `mall-portal` 中落地一个轻量智能客服 RAG 场景，目标不是复杂向量平台，而是能真实接入商城业务数据并可切换模型 provider。

## API

### Provider Status

```http
GET /ai/customer/status
Authorization: Bearer <member-jwt>
```

返回当前 provider、模型、API key 是否已配置，以及客服能力是否 ready。

### Customer Chat

```http
POST /ai/customer/chat
Authorization: Bearer <member-jwt>
Content-Type: application/json

{
  "question": "我的订单什么时候发货？",
  "orderSn": "202607090001",
  "productId": 26,
  "maxContextCount": 6
}
```

`orderSn` 和 `productId` 都是可选字段。订单召回会强制限定为当前登录会员自己的订单，避免跨用户查询。

## RAG Knowledge Sources

- 商品：`pms_product`，支持 `productId` 精确召回和商品名/关键字模糊召回。
- 订单：`oms_order` + `oms_order_item`，支持订单号召回和最近订单兜底召回。
- FAQ：`cms_help`，读取已展示的帮助内容。
- 售后规则：`mall-portal/src/main/resources/ai/after-sales-rules.md`。

接口响应会返回模型回答和 `contexts`，用于演示“回答基于哪些知识片段”。

## Provider Configuration

默认配置在 `mall-portal/src/main/resources/application.yml`：

```yaml
mall:
  ai:
    enabled: true
    provider: mock
    api-key: ${AI_API_KEY:}
    base-url:
    model:
    timeout-millis: 5000
    max-context-count: 6
    max-tokens: 800
```

支持 provider：

- `mock`：本地无模型环境可完整演示 RAG 召回和接口返回。
- `openai`：OpenAI compatible `/chat/completions`，默认模型 `gpt-4.1-mini`。
- `qwen`：DashScope compatible mode，默认模型 `qwen-turbo`。
- `deepseek`：DeepSeek compatible API，默认模型 `deepseek-chat`。
- `ollama`：本地 Ollama `/api/chat`，默认模型 `qwen2.5:7b`。

无 API key 或本地 Ollama 不可用时，接口不会崩溃，会返回 `enabled=false`、`degraded=true`，并带上已召回的知识片段，便于排查配置。

## Ollama Local Smoke Test

```powershell
ollama pull qwen2.5:7b
ollama serve
```

配置：

```yaml
mall:
  ai:
    provider: ollama
    base-url: http://localhost:11434
    model: qwen2.5:7b
```

启动 `mall-portal` 后，使用会员 JWT 调用 `/ai/customer/chat`。如果本机资源不足，可以换更小的 Ollama 模型，并同步更新 `mall.ai.model`。
