import { listProducts } from './product'
import { listOrders } from './order'
import { listReturnApplies } from './order'
import { listFlashPromotions } from './seckill'

export interface DashboardOverview {
  productTotal: number
  onSaleProducts: number
  pendingOrders: number
  pendingReturns: number
  activeFlashPromotions: number
  recentOrders: Awaited<ReturnType<typeof listOrders>>['list']
}

export async function fetchDashboardOverview(): Promise<DashboardOverview> {
  const [products, onSale, pendingOrders, pendingReturns, flash] = await Promise.all([
    listProducts({ pageNum: 1, pageSize: 1 }),
    listProducts({ pageNum: 1, pageSize: 1, publishStatus: 1 }),
    listOrders({ pageNum: 1, pageSize: 5, status: 1 }),
    listReturnApplies({ pageNum: 1, pageSize: 1, status: 0 }),
    listFlashPromotions({ pageNum: 1, pageSize: 10, keyword: '' })
  ])

  return {
    productTotal: products.total,
    onSaleProducts: onSale.total,
    pendingOrders: pendingOrders.total,
    pendingReturns: pendingReturns.total,
    activeFlashPromotions: flash.list.filter((item) => item.status === 1).length,
    recentOrders: pendingOrders.list
  }
}
