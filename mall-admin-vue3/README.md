# mall-admin-vue3

Phase 3 Vue3 + TypeScript admin console for the seckill optimization project.

## Scope

- Admin login through `mall-admin` `/admin/login`.
- Flash promotion list through `/flash/list`.
- Flash promotion product relation list through `/flashProductRelation/list`.
- Seckill warmup through `/seckill/manage/warmup/{relationId}`.
- Seckill dashboard and order logs through `/seckill/manage/summary` and `/seckill/manage/orderLogs`.

## Local Run

```bash
npm install
npm run dev
```

The Vite dev server proxies `/api/admin/**` to `http://localhost:8080/**`.

Start `mall-admin` first if you want live data. The frontend still builds independently with `npm run build`.
