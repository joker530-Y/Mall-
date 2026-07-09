# Optimization Architecture

## Seckill Order Flow

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Portal as mall-portal
    participant Redis
    participant MQ as RabbitMQ
    participant Consumer as SeckillOrderReceiver
    participant DB as MySQL

    Client->>Portal: POST /seckill/redis/warmup/{relationId}
    Portal->>DB: read flash promotion relation
    Portal->>Redis: set stock and metadata

    Client->>Portal: POST /seckill/redis/order
    Portal->>Redis: Lua check stock, member limit, duplicate request
    alt Redis deduction succeeds
        Redis-->>Portal: success
        Portal->>MQ: send SeckillOrderMessage
        Portal-->>Client: QUEUING
        MQ->>Consumer: deliver message
        Consumer->>DB: idempotently create order and order item
        Consumer->>DB: decrement seckill DB stock
        Consumer->>Redis: write order result
    else stock or limit rejected
        Redis-->>Portal: failure code
        Portal-->>Client: SOLD_OUT or REPEAT
    end

    Client->>Portal: GET /seckill/redis/result
    Portal->>Redis: read async result
    Portal-->>Client: QUEUING, SUCCESS, or FAILED
```

## Hot Read Cache Flow

```mermaid
flowchart TD
    A["Client request"] --> B["Service method"]
    B --> C{"Caffeine hit?"}
    C -->|yes| D["Return local cache"]
    C -->|no| E{"Redis hit?"}
    E -->|yes| F["Backfill Caffeine and return"]
    E -->|no| G{"Acquire Redis rebuild lock?"}
    G -->|yes| H["Load from DB"]
    H --> I["Write Caffeine"]
    I --> J["Write Redis with randomized TTL"]
    J --> K["Return result"]
    G -->|no| L["Brief wait and retry Redis"]
    L --> M{"Redis hit after wait?"}
    M -->|yes| F
    M -->|no| H
```

## Cache Keys

| Data | Key pattern |
| --- | --- |
| Homepage content | `mall:hot:home:content:{minuteBucket}` |
| Recommended products | `mall:hot:home:recommend:{pageNum}:{pageSize}` |
| Hot products | `mall:hot:home:hot:{pageNum}:{pageSize}` |
| New products | `mall:hot:home:new:{pageNum}:{pageSize}` |
| Product detail | `mall:hot:product:detail:{productId}` |
| Product category tree | `mall:hot:product:category-tree` |

## Reliability Notes

- Redis failures in the hot-data cache are degraded to DB load instead of failing the request.
- Null-value cache TTL is short to reduce penetration from invalid IDs.
- Randomized Redis TTL lowers same-time expiration risk.
- The seckill hot path does not use a DB lock for stock deduction; Redis Lua owns the high-concurrency admission decision.
- DB remains the persistent truth after the async consumer creates orders.
