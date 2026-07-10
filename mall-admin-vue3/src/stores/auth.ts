import { defineStore } from 'pinia'
import { getCurrentAdminInfo, login, logout } from '@/api/auth'
import { usePermissionStore } from '@/stores/permission'

const TOKEN_KEY = 'mall-admin-vue3-token'
const TOKEN_HEAD_KEY = 'mall-admin-vue3-token-head'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: sessionStorage.getItem(TOKEN_KEY) || '',
    tokenHead: sessionStorage.getItem(TOKEN_HEAD_KEY) || '',
    username: '',
    icon: '',
    roles: [] as string[]
  }),
  getters: {
    authorization: (state) =>
      state.token && state.tokenHead ? `${state.tokenHead.trim()} ${state.token}` : '',
    isAuthenticated: (state) => Boolean(state.token && state.tokenHead)
  },
  actions: {
    async signIn(username: string, password: string) {
      const result = await login({ username, password })
      this.token = result.token
      this.tokenHead = result.tokenHead
      sessionStorage.setItem(TOKEN_KEY, result.token)
      sessionStorage.setItem(TOKEN_HEAD_KEY, result.tokenHead)
      try {
        await this.bootstrap()
      } catch (error) {
        this.clearSession()
        throw error
      }
    },
    async bootstrap() {
      if (!this.authorization) return false
      const info = await getCurrentAdminInfo()
      this.username = info.username
      this.icon = info.icon || ''
      this.roles = info.roles || []
      const permission = usePermissionStore()
      permission.initializeMenus(info.menus || [])
      return true
    },
    hasPermission(permission: string) {
      if (!permission) return true
      if (this.roles.includes('超级管理员')) return true
      return usePermissionStore().can(permission)
    },
    async signOut() {
      try {
        await logout()
      } catch {
        // 后端不可用时仍应完成本地退出
      } finally {
        this.clearSession()
      }
    },
    clearSession() {
      this.token = ''
      this.tokenHead = ''
      this.username = ''
      this.icon = ''
      this.roles = []
      sessionStorage.removeItem(TOKEN_KEY)
      sessionStorage.removeItem(TOKEN_HEAD_KEY)
      usePermissionStore().reset()
    }
  }
})
