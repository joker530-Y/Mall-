import request, { PageResult, unwrap } from './request'

export interface Order {
  id: number
  orderSn: string
  memberId?: number
  memberUsername?: string
  totalAmount?: number
  payAmount?: number
  status?: number
  orderType?: number
  sourceType?: number
  createTime?: string
  deliveryCompany?: string
  deliverySn?: string
  receiverName?: string
  receiverPhone?: string
  receiverProvince?: string
  receiverCity?: string
  receiverRegion?: string
  receiverDetailAddress?: string
  note?: string
}

export interface OrderItem {
  id: number
  productName?: string
  productPic?: string
  productPrice?: number
  productQuantity?: number
}

export interface OrderDetail extends Order {
  orderItemList?: OrderItem[]
  historyList?: { id: number; note?: string; createTime?: string; operateMan?: string }[]
}

export interface ReturnApply {
  id: number
  orderId?: number
  orderSn?: string
  memberUsername?: string
  productName?: string
  returnAmount?: number
  status?: number
  createTime?: string
  handleTime?: string
  handleMan?: string
  reason?: string
}

export interface ReturnReason {
  id: number
  name: string
  sort?: number
  status?: number
}

export interface OrderSetting {
  id: number
  flashOrderOvertime?: number
  normalOrderOvertime?: number
  confirmOvertime?: number
  finishOvertime?: number
  commentOvertime?: number
}

export interface OrderQuery {
  orderSn?: string
  receiverKeyword?: string
  status?: number
  orderType?: number
  pageNum: number
  pageSize: number
}

export function listOrders(params: OrderQuery) {
  return unwrap<PageResult<Order>>(request.get('/order/list', { params }))
}

export function getOrderDetail(id: number) {
  return unwrap<OrderDetail>(request.get(`/order/${id}`))
}

export function deliverOrders(items: { orderId: number; deliveryCompany: string; deliverySn: string }[]) {
  return unwrap<unknown>(request.post('/order/update/delivery', items))
}

export function closeOrders(ids: number[], note: string) {
  return unwrap<unknown>(request.post('/order/update/close', null, { params: { ids, note } }))
}

export function listReturnApplies(params: {
  status?: number
  pageNum: number
  pageSize: number
  receiverKeyword?: string
}) {
  return unwrap<PageResult<ReturnApply>>(request.get('/returnApply/list', { params }))
}

export function getReturnApply(id: number) {
  return unwrap<ReturnApply>(request.get(`/returnApply/${id}`))
}

export function updateReturnApplyStatus(id: number, data: { status: number; handleNote?: string; returnAmount?: number }) {
  return unwrap<unknown>(request.post(`/returnApply/update/status/${id}`, data))
}

export function listReturnReasons(pageNum: number, pageSize: number) {
  return unwrap<PageResult<ReturnReason>>(request.get('/returnReason/list', { params: { pageNum, pageSize } }))
}

export function updateReturnReasonStatus(ids: number[], status: number) {
  return unwrap<unknown>(request.post('/returnReason/update/status', null, { params: { ids, status } }))
}

export function getOrderSetting(id = 1) {
  return unwrap<OrderSetting>(request.get(`/orderSetting/${id}`))
}

export function updateOrderSetting(id: number, data: OrderSetting) {
  return unwrap<unknown>(request.post(`/orderSetting/update/${id}`, data))
}
