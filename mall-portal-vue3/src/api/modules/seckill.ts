import request, { unwrap } from '../http/request'

export interface SeckillOrderParam {
  relationId: number
  productSkuId: number
  memberReceiveAddressId: number
  quantity: number
  payType: number
}

export interface SeckillSubmitResult {
  requestId: string
  relationId: number
  status: string
  remainingStock?: number
}

export interface SeckillOrderResult {
  requestId?: string
  relationId: number
  orderId?: number
  orderSn?: string
  status: string
  reason?: string
}

export function submitSeckillOrder(param: SeckillOrderParam) {
  return unwrap<SeckillSubmitResult>(request.post('/seckill/redis/order', param))
}

export function getSeckillResult(relationId: number) {
  return unwrap<SeckillOrderResult>(request.get('/seckill/redis/result', { params: { relationId } }))
}
