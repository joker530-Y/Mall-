export const ORDER_STATUS_MAP: Record<number, { label: string; type: 'success' | 'warning' | 'danger' | 'info' }> = {
  0: { label: '待付款', type: 'warning' },
  1: { label: '待发货', type: 'warning' },
  2: { label: '已发货', type: 'info' },
  3: { label: '已完成', type: 'success' },
  4: { label: '已关闭', type: 'danger' }
}
