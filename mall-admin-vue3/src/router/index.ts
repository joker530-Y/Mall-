import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import LoginView from '@/views/LoginView.vue'
import LayoutView from '@/views/LayoutView.vue'
import DashboardView from '@/views/DashboardView.vue'
import FlashPromotionView from '@/views/FlashPromotionView.vue'
import SeckillOrdersView from '@/views/SeckillOrdersView.vue'
import NotFoundView from '@/views/NotFoundView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView },
    {
      path: '/',
      component: LayoutView,
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', name: 'dashboard', component: DashboardView },
        { path: 'flash-promotions', name: 'flash-promotions', component: FlashPromotionView },
        { path: 'seckill-orders', name: 'seckill-orders', component: SeckillOrdersView }
      ]
    },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.name !== 'login' && !auth.authorization) {
    return { name: 'login' }
  }
  if (to.name === 'login' && auth.authorization) {
    return { name: 'dashboard' }
  }
  return true
})

export default router
