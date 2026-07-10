import request, { PageResult, unwrap } from '../http/request'

export interface OrderItem {
  id: number
  productName?: string
  productPic?: string
  productPrice?: number
  productQuantity?: number
}

export interface OrderDetail {
  id: number
  orderSn: string
  status?: number
  payType?: number
  totalAmount?: number
  payAmount?: number
  freightAmount?: number
  receiverName?: string
  receiverPhone?: string
  receiverProvince?: string
  receiverCity?: string
  receiverRegion?: string
  receiverDetailAddress?: string
  deliveryCompany?: string
  deliverySn?: string
  createTime?: string
  paymentTime?: string
  orderItemList?: OrderItem[]
}

export function listOrders(status: number, pageNum: number, pageSize: number) {
  return unwrap<PageResult<OrderDetail>>(request.get('/order/list', { params: { status, pageNum, pageSize } }))
}

export function getOrderDetail(orderId: number) {
  return unwrap<OrderDetail>(request.get(`/order/detail/${orderId}`))
}

export function cancelUserOrder(orderId: number) {
  return unwrap<null>(request.post('/order/cancelUserOrder', null, { params: { orderId } }))
}

export function confirmReceiveOrder(orderId: number) {
  return unwrap<null>(request.post('/order/confirmReceiveOrder', null, { params: { orderId } }))
}

export function deleteOrder(orderId: number) {
  return unwrap<null>(request.post('/order/deleteOrder', null, { params: { orderId } }))
}
