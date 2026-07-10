import type { RouteRecordRaw } from 'vue-router'
import type { MenuItem, SidebarItem } from '@/types/menu'

export interface RouteModuleMeta {
  title: string
  icon?: string
  permission?: string
  keepAlive?: boolean
  hidden?: boolean
  [key: string]: unknown
}

type RouteModule = {
  path: string
  name: string
  component: () => Promise<unknown>
  meta: RouteModuleMeta
}

export const MENU_ROUTE_REGISTRY: Record<string, RouteModule> = {
  dashboard: {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('@/views/dashboard/DashboardOverviewView.vue'),
    meta: { title: '经营概览', icon: 'Odometer' }
  },
  product: {
    path: '/product/list',
    name: 'product-list',
    component: () => import('@/views/product/ProductListView.vue'),
    meta: { title: '商品列表', icon: 'Goods', permission: 'product:edit' }
  },
  addProduct: {
    path: '/product/list',
    name: 'product-add-alias',
    component: () => import('@/views/product/ProductListView.vue'),
    meta: { title: '添加商品', hidden: true, permission: 'product:edit' }
  },
  productCate: {
    path: '/product/categories',
    name: 'product-categories',
    component: () => import('@/views/product/CategoryListView.vue'),
    meta: { title: '商品分类', icon: 'Menu' }
  },
  productAttr: {
    path: '/product/attributes',
    name: 'product-attributes',
    component: () => import('@/views/product/AttributeListView.vue'),
    meta: { title: '商品类型', icon: 'Collection' }
  },
  brand: {
    path: '/product/brands',
    name: 'product-brands',
    component: () => import('@/views/product/BrandListView.vue'),
    meta: { title: '品牌管理', icon: 'Shop' }
  },
  order: {
    path: '/order/list',
    name: 'order-list',
    component: () => import('@/views/order/OrderListView.vue'),
    meta: { title: '订单列表', icon: 'List', permission: 'order:read' }
  },
  orderSetting: {
    path: '/after-sales/settings',
    name: 'order-setting',
    component: () => import('@/views/order/OrderSettingView.vue'),
    meta: { title: '订单设置', icon: 'Setting' }
  },
  returnApply: {
    path: '/after-sales/returns',
    name: 'return-apply',
    component: () => import('@/views/order/ReturnApplyListView.vue'),
    meta: { title: '退货申请', icon: 'RefreshLeft' }
  },
  returnReason: {
    path: '/after-sales/reasons',
    name: 'return-reason',
    component: () => import('@/views/order/ReturnReasonListView.vue'),
    meta: { title: '退货原因', icon: 'Document' }
  },
  flash: {
    path: '/marketing/flash-promotions',
    name: 'flash-promotions',
    component: () => import('@/views/marketing/FlashPromotionListView.vue'),
    meta: { title: '秒杀活动', icon: 'Lightning', permission: 'seckill:manage:read' }
  },
  seckillDashboard: {
    path: '/seckill/dashboard',
    name: 'seckill-dashboard',
    component: () => import('@/views/marketing/SeckillMonitorView.vue'),
    meta: { title: '秒杀运行看板', icon: 'DataLine', permission: 'seckill:manage:read' }
  },
  seckillOrders: {
    path: '/marketing/seckill-orders',
    name: 'seckill-orders',
    component: () => import('@/views/marketing/SeckillOrdersView.vue'),
    meta: { title: '秒杀订单日志', icon: 'Tickets', permission: 'seckill:manage:read' }
  },
  coupon: {
    path: '/marketing/coupons',
    name: 'coupon-list',
    component: () => import('@/views/marketing/CouponListView.vue'),
    meta: { title: '优惠券', icon: 'Ticket' }
  },
  homeBrand: {
    path: '/marketing/home/brands',
    name: 'home-brand',
    component: () => import('@/views/marketing/HomeBrandListView.vue'),
    meta: { title: '品牌推荐', icon: 'Shop' }
  },
  homeNew: {
    path: '/marketing/home/new-products',
    name: 'home-new',
    component: () => import('@/views/marketing/HomeNewProductListView.vue'),
    meta: { title: '新品推荐', icon: 'Star' }
  },
  homeHot: {
    path: '/marketing/home/recommend-products',
    name: 'home-hot',
    component: () => import('@/views/marketing/HomeRecommendProductListView.vue'),
    meta: { title: '人气推荐', icon: 'TrendCharts' }
  },
  homeSubject: {
    path: '/marketing/home/subjects',
    name: 'home-subject',
    component: () => import('@/views/marketing/HomeRecommendSubjectListView.vue'),
    meta: { title: '专题推荐', icon: 'Reading' }
  },
  homeAdvertise: {
    path: '/marketing/home/advertises',
    name: 'home-advertise',
    component: () => import('@/views/marketing/HomeAdvertiseListView.vue'),
    meta: { title: '广告列表', icon: 'Picture' }
  },
  memberLevel: {
    path: '/member/levels',
    name: 'member-levels',
    component: () => import('@/views/member/MemberLevelListView.vue'),
    meta: { title: '会员等级', icon: 'User' }
  },
  admin: {
    path: '/system/admins',
    name: 'system-admins',
    component: () => import('@/views/system/AdminListView.vue'),
    meta: { title: '管理员', icon: 'UserFilled' }
  },
  role: {
    path: '/system/roles',
    name: 'system-roles',
    component: () => import('@/views/system/RoleListView.vue'),
    meta: { title: '角色管理', icon: 'Key' }
  },
  menu: {
    path: '/system/menus',
    name: 'system-menus',
    component: () => import('@/views/system/MenuListView.vue'),
    meta: { title: '菜单管理', icon: 'Menu' }
  },
  resource: {
    path: '/system/resources',
    name: 'system-resources',
    component: () => import('@/views/system/ResourceListView.vue'),
    meta: { title: '资源管理', icon: 'Link' }
  }
}

const HIDDEN_ROUTES: RouteRecordRaw[] = [
  {
    path: '/product/:id',
    name: 'product-detail',
    component: () => import('@/views/product/ProductDetailView.vue'),
    meta: { title: '编辑商品', hidden: true, permission: 'product:edit' }
  },
  {
    path: '/order/:id',
    name: 'order-detail',
    component: () => import('@/views/order/OrderDetailView.vue'),
    meta: { title: '订单详情', hidden: true, permission: 'order:read' }
  },
  {
    path: '/marketing/flash-promotions/:id',
    name: 'flash-promotion-detail',
    component: () => import('@/views/marketing/SeckillPromotionLayout.vue'),
    redirect: (to) => `/marketing/flash-promotions/${to.params.id}/products`,
    meta: { title: '活动详情', hidden: true },
    children: [
      {
        path: 'products',
        name: 'flash-promotion-products',
        component: () => import('@/views/marketing/FlashPromotionProductsView.vue'),
        meta: { title: '场次与商品', permission: 'seckill:manage:read' }
      },
      {
        path: 'warmup',
        name: 'flash-promotion-warmup',
        component: () => import('@/views/marketing/FlashPromotionWarmupView.vue'),
        meta: { title: '预热与库存', permission: 'seckill:manage:write' }
      },
      {
        path: 'monitor',
        name: 'flash-promotion-monitor',
        component: () => import('@/views/marketing/SeckillMonitorView.vue'),
        meta: { title: '实时运行', permission: 'seckill:manage:read' }
      },
      {
        path: 'logs',
        name: 'flash-promotion-logs',
        component: () => import('@/views/marketing/SeckillOrdersView.vue'),
        meta: { title: '订单日志', permission: 'seckill:manage:read' }
      }
    ]
  }
]

function moduleToRoute(module: RouteModule): RouteRecordRaw {
  return {
    path: module.path,
    name: module.name,
    component: module.component,
    meta: module.meta as unknown as RouteRecordRaw['meta']
  }
}

export const BOOTSTRAP_ROUTES: RouteRecordRaw[] = [
  moduleToRoute(MENU_ROUTE_REGISTRY.dashboard),
  ...HIDDEN_ROUTES,
  { path: '/product/create', redirect: '/product/list' },
  { path: '/flash-promotions', redirect: '/marketing/flash-promotions' },
  { path: '/seckill-orders', redirect: '/marketing/seckill-orders' }
]

const unmappedMenus = new Set<string>()

export function getUnmappedMenus() {
  return [...unmappedMenus]
}

export function buildRoutesFromMenus(menus: MenuItem[]): RouteRecordRaw[] {
  unmappedMenus.clear()
  const routes: RouteRecordRaw[] = []
  const added = new Set(BOOTSTRAP_ROUTES.map((route) => route.path))

  for (const menu of menus) {
    if (menu.hidden === 1) continue
    const module = MENU_ROUTE_REGISTRY[menu.name]
    if (!module) {
      if (menu.level > 0) unmappedMenus.add(`${menu.name}:${menu.title}`)
      continue
    }
    if (added.has(module.path)) continue
    routes.push(moduleToRoute(module))
    added.add(module.path)
  }

  if (unmappedMenus.size > 0) {
    console.warn('[permission] 未映射的后端菜单:', [...unmappedMenus])
  }
  return routes
}

function buildMenuTree(flat: MenuItem[]): MenuItem[] {
  const nodes = new Map<number, MenuItem>()
  flat.forEach((menu) => nodes.set(menu.id, { ...menu, children: [] }))
  const roots: MenuItem[] = []
  nodes.forEach((menu) => {
    if (menu.parentId === 0) {
      roots.push(menu)
      return
    }
    const parent = nodes.get(menu.parentId)
    if (parent) parent.children!.push(menu)
  })
  const sortMenus = (items: MenuItem[]) => {
    items.sort((a, b) => a.sort - b.sort)
    items.forEach((item) => item.children && sortMenus(item.children))
  }
  sortMenus(roots)
  return roots
}

function menuToSidebar(menu: MenuItem): SidebarItem | null {
  const module = MENU_ROUTE_REGISTRY[menu.name]
  if (module && !module.meta.hidden) {
    return { path: module.path, title: menu.title || module.meta.title, icon: menu.icon || module.meta.icon }
  }
  if (menu.children?.length) {
    const children = menu.children.map(menuToSidebar).filter(Boolean) as SidebarItem[]
    if (children.length === 0) return null
    return { path: children[0].path, title: menu.title, icon: menu.icon, children }
  }
  return null
}

export function buildSidebarFromMenus(menus: MenuItem[]): SidebarItem[] {
  return buildMenuTree(menus)
    .map(menuToSidebar)
    .filter(Boolean) as SidebarItem[]
}
