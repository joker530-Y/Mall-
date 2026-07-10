import request, { unwrap } from '../http/request'
import type { CartItem } from './cart'
import type { MemberAddress } from './member'
import { listAddresses, addAddress, updateAddress, deleteAddress } from './member'

export type { MemberAddress }
export { listAddresses, addAddress, updateAddress, deleteAddress }

export interface CouponHistoryDetail {
  id: number
  coupon?: { id: number; name: string; amount?: number; minPoint?: number }
}

export interface ConfirmOrderParam {
  cartIds: number[]
  couponId?: number
  useIntegration?: number
}

export interface ConfirmOrderResult {
  cartPromotionItemList: CartItem[]
  memberReceiveAddressList: MemberAddress[]
  couponHistoryDetailList: CouponHistoryDetail[]
  memberIntegration?: number
  calcAmount?: {
    totalAmount?: number
    freightAmount?: number
    promotionAmount?: number
    couponAmount?: number
    integrationAmount?: number
    payAmount?: number
  }
}

export interface OrderParam {
  memberReceiveAddressId: number
  couponId?: number
  useIntegration?: number
  payType: number
  cartIds: number[]
}

export interface CreateOrderResult {
  order: { id: number; orderSn: string; payAmount?: number; status?: number }
  orderItemList?: unknown[]
}

export function generateConfirmOrder(param: ConfirmOrderParam) {
  return unwrap<ConfirmOrderResult>(request.post('/order/generateConfirmOrder', param))
}

export function generateOrder(param: OrderParam) {
  return unwrap<CreateOrderResult>(request.post('/order/generateOrder', param))
}

export function mockPay(orderId: number, payType = 1) {
  return unwrap<number>(request.post(`/order/mock-pay/${orderId}`, null, { params: { payType } }))
}
