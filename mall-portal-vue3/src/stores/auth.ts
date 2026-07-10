import { defineStore } from 'pinia'
import { getMemberInfo, login } from '@/api/modules/auth'
import { listCartItems } from '@/api/modules/cart'

const TOKEN_KEY = 'mall-portal-vue3-token'
const TOKEN_HEAD_KEY = 'mall-portal-vue3-token-head'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: sessionStorage.getItem(TOKEN_KEY) || '',
    tokenHead: sessionStorage.getItem(TOKEN_HEAD_KEY) || 'Bearer ',
    username: '',
    nickname: '',
    cartCount: 0
  }),
  getters: {
    authorization: (state) =>
      state.token ? `${state.tokenHead.trim()} ${state.token}` : '',
    isAuthenticated: (state) => Boolean(state.token)
  },
  actions: {
    async signIn(username: string, password: string) {
      const result = await login(username, password)
      this.token = result.token
      this.tokenHead = result.tokenHead || 'Bearer '
      sessionStorage.setItem(TOKEN_KEY, result.token)
      sessionStorage.setItem(TOKEN_HEAD_KEY, this.tokenHead)
      try {
        await this.bootstrap()
      } catch (error) {
        this.clearSession()
        throw error
      }
    },
    async bootstrap() {
      if (!this.token) return false
      const member = await getMemberInfo()
      this.username = member.username
      this.nickname = member.nickname || member.username
      await this.refreshCartCount()
      return true
    },
    async refreshCartCount() {
      if (!this.token) {
        this.cartCount = 0
        return
      }
      try {
        const items = await listCartItems()
        this.cartCount = items.reduce((sum, item) => sum + (item.quantity || 0), 0)
      } catch {
        this.cartCount = 0
      }
    },
    clearSession() {
      this.token = ''
      this.tokenHead = 'Bearer '
      this.username = ''
      this.nickname = ''
      this.cartCount = 0
      sessionStorage.removeItem(TOKEN_KEY)
      sessionStorage.removeItem(TOKEN_HEAD_KEY)
    }
  }
})
