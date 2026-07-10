import { createRouter, createWebHistory } from 'vue-router'
import { resolveInternalRedirect } from '@/utils/navigation'
import { useAuthStore } from '@/stores/auth'

const AUTH_ROUTES = new Set(['cart', 'checkout', 'payment', 'orders', 'order-detail', 'account', 'addresses', 'seckill-detail'])

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/PortalLayout.vue'),
      children: [
        { path: '', name: 'home', component: () => import('@/views/home/HomeView.vue'), meta: { title: '首页' } },
        { path: 'search', name: 'search', component: () => import('@/views/product/SearchView.vue'), meta: { title: '搜索' } },
        { path: 'product/:id', name: 'product-detail', component: () => import('@/views/product/ProductDetailView.vue'), meta: { title: '商品详情' } },
        { path: 'cart', name: 'cart', component: () => import('@/views/cart/CartView.vue'), meta: { title: '购物车', auth: true } },
        { path: 'checkout', name: 'checkout', component: () => import('@/views/checkout/CheckoutView.vue'), meta: { title: '确认订单', auth: true } },
        { path: 'payment/:orderId', name: 'payment', component: () => import('@/views/checkout/PaymentView.vue'), meta: { title: '模拟支付', auth: true } },
        { path: 'orders', name: 'orders', component: () => import('@/views/order/OrderListView.vue'), meta: { title: '我的订单', auth: true } },
        { path: 'orders/:id', name: 'order-detail', component: () => import('@/views/order/OrderDetailView.vue'), meta: { title: '订单详情', auth: true } },
        { path: 'account', name: 'account', component: () => import('@/views/account/AccountView.vue'), meta: { title: '个人中心', auth: true } },
        { path: 'account/addresses', name: 'addresses', component: () => import('@/views/account/AddressListView.vue'), meta: { title: '收货地址', auth: true } },
        { path: 'seckill', name: 'seckill', component: () => import('@/views/seckill/SeckillListView.vue'), meta: { title: '秒杀专场' } },
        { path: 'seckill/:relationId', name: 'seckill-detail', component: () => import('@/views/seckill/SeckillDetailView.vue'), meta: { title: '秒杀抢购', auth: true } }
      ]
    },
    { path: '/login', name: 'login', component: () => import('@/views/account/LoginView.vue'), meta: { title: '登录' } },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/exception/NotFoundView.vue'), meta: { title: '页面不存在' } }
  ],
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.name === 'login') {
    if (auth.isAuthenticated) {
      return resolveInternalRedirect(typeof to.query.redirect === 'string' ? to.query.redirect : undefined)
    }
    return true
  }
  if ((to.meta.auth || (to.name && AUTH_ROUTES.has(String(to.name)))) && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
