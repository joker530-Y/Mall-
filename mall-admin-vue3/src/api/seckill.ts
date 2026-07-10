import request, { PageResult, unwrap } from './request'

export interface FlashPromotion {
  id: number
  title: string
  startDate: string
  endDate: string
  status: number
  createTime: string
}

export interface FlashPromotionProduct {
  id: number
  flashPromotionId: number
  flashPromotionSessionId: number
  productId: number
  productName?: string
  productSn?: string
  productPic?: string
  flashPromotionPrice: number | null
  flashPromotionCount: number | null
  flashPromotionLimit: number | null
  sort: number | null
}

export interface SeckillSummary {
  relationId: number
  productId: number
  flashPromotionId: number
  flashPromotionSessionId: number
  dbRemainingStock: number
  redisStock: number | null
  limit: number | null
  processingCount: number
  successCount: number
  failedCount: number
  totalRequestCount: number
  duplicateMemberCount: number
  oversoldCount: number
  stockKey: string
  refreshedAt: string
}

export interface WarmupResult {
  relationId: number
  stock: number
  limit: number
  stockKey: string
  warmedAt: string
}

export interface SeckillOrderLog {
  id: number
  requestId: string
  memberId: number
  relationId: number
  orderId?: number
  orderSn?: string
  status: number
  statusText: string
  failReason?: string
  createTime: string
  updateTime: string
}

export interface FlashPromotionSession {
  id: number
  name: string
  startTime?: string
  endTime?: string
  productCount?: number
}

export function listFlashSessions(flashPromotionId: number) {
  return unwrap<FlashPromotionSession[]>(
    request.get('/flashSession/selectList', { params: { flashPromotionId } })
  )
}

export function listFlashPromotions(params: { keyword?: string; pageNum: number; pageSize: number }) {
  return unwrap<PageResult<FlashPromotion>>(request.get('/flash/list', { params }))
}

export function listFlashProducts(params: {
  flashPromotionId: number
  flashPromotionSessionId: number
  pageNum: number
  pageSize: number
}) {
  return unwrap<PageResult<FlashPromotionProduct>>(request.get('/flashProductRelation/list', { params }))
}

export function warmupSeckill(relationId: number) {
  return unwrap<WarmupResult>(request.post(`/seckill/manage/warmup/${relationId}`))
}

export function getSeckillSummary(relationId: number) {
  return unwrap<SeckillSummary>(request.get('/seckill/manage/summary', { params: { relationId } }))
}

export function listSeckillOrderLogs(params: { relationId: number; pageNum: number; pageSize: number }) {
  return unwrap<PageResult<SeckillOrderLog>>(request.get('/seckill/manage/orderLogs', { params }))
}
