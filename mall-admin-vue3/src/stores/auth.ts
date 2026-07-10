import { defineStore } from 'pinia'
import { login, logout } from '@/api/auth'

const TOKEN_KEY = 'mall-admin-vue3-token'
const TOKEN_HEAD_KEY = 'mall-admin-vue3-token-head'
const USERNAME_KEY = 'mall-admin-vue3-username'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    tokenHead: localStorage.getItem(TOKEN_HEAD_KEY) || '',
    username: localStorage.getItem(USERNAME_KEY) || ''
  }),
  getters: {
    authorization: (state) => state.token && state.tokenHead ? `${state.tokenHead.trim()} ${state.token}` : ''
  },
  actions: {
    async signIn(username: string, password: string) {
      const result = await login({ username, password })
      this.token = result.token
      this.tokenHead = result.tokenHead
      this.username = username
      localStorage.setItem(TOKEN_KEY, result.token)
      localStorage.setItem(TOKEN_HEAD_KEY, result.tokenHead)
      localStorage.setItem(USERNAME_KEY, username)
    },
    async signOut() {
      try {
        await logout()
      } catch {
        // Local logout must still complete if the backend is unavailable.
      } finally {
        this.clearSession()
      }
    },
    clearSession() {
      this.token = ''
      this.tokenHead = ''
      this.username = ''
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(TOKEN_HEAD_KEY)
      localStorage.removeItem(USERNAME_KEY)
    }
  }
})
