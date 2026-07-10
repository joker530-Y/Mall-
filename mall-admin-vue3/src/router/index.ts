import { createRouter, createWebHistory } from 'vue-router'
import { setupRouterGuards } from './guards'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/',
      name: 'root',
      component: () => import('@/layouts/AdminLayout.vue'),
      redirect: '/dashboard',
      children: []
    },
    {
      path: '/403',
      name: 'forbidden',
      component: () => import('@/views/exception/ForbiddenView.vue'),
      meta: { title: '无权访问' }
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { title: '页面不存在' }
    }
  ]
})

setupRouterGuards(router)

export default router
