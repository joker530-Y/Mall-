# mall-portal-vue3

Vue 3 + TypeScript consumer mall for the mall-portal backend.

## Scope

- Product discovery: home, search, category, product detail, SKU selection
- Cart and checkout with address and coupon selection
- Order list/detail, cancel, confirm receive
- Mock payment only (`POST /order/mock-pay/{orderId}`)
- Seckill list and purchase with result polling
- Member login and address management

## Local Run

```bash
npm install
npm run dev
```

Dev server: `http://localhost:5174`

Vite proxies `/api/portal/**` to `http://localhost:8085/**`, and strips the forwarded `Origin` header so Spring CORS does not reject same-origin proxy traffic.

Start `mall-portal` first for live data. Demo member account is documented in the root `README.md`.

Portal CORS must allow `http://localhost:5174` / `http://127.0.0.1:5174` (see `mall-portal` `cors.allowed-origins`).

## SKU Selection

Product SKUs use `spData` JSON (`[{"key":"颜色","value":"银色"}, ...]`), not legacy `sp1/sp2/sp3`.

The detail page builds option groups from `spData`, matches the selected combination to a SKU, and enables **加入购物车** / **立即购买** only when that SKU has stock. Helpers live in `src/utils/sku.ts`.

## Scripts

```bash
npm run build
npm run test
```

## Independence

This app does not share routes, session storage, or layout with `mall-admin-vue3`. Admin and consumer frontends run on separate ports with separate tokens.
