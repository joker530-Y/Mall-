export const ORDER_STATUS_MAP: Record<number, { label: string; type: 'success' | 'warning' | 'danger' | 'info' }> = {
  0: { label: '待付款', type: 'warning' },
  1: { label: '待发货', type: 'warning' },
  2: { label: '已发货', type: 'info' },
  3: { label: '已完成', type: 'success' },
  4: { label: '已关闭', type: 'danger' },
  5: { label: '无效订单', type: 'danger' }
}

export const PUBLISH_STATUS_MAP: Record<number, { label: string; type: 'success' | 'info' }> = {
  0: { label: '下架', type: 'info' },
  1: { label: '上架', type: 'success' }
}

export const RETURN_APPLY_STATUS_MAP: Record<number, { label: string; type: 'success' | 'warning' | 'danger' | 'info' }> = {
  0: { label: '待处理', type: 'warning' },
  1: { label: '退货中', type: 'info' },
  2: { label: '已完成', type: 'success' },
  3: { label: '已拒绝', type: 'danger' }
}

export const COUPON_TYPE_MAP: Record<number, string> = {
  0: '全场赠券',
  1: '会员赠券',
  2: '购物赠券',
  3: '注册赠券'
}
