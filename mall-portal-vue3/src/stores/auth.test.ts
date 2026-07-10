import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth'

function createStorageMock() {
  const storage = new Map<string, string>()
  return {
    getItem: (key: string) => storage.get(key) ?? null,
    setItem: (key: string, value: string) => storage.set(key, value),
    removeItem: (key: string) => storage.delete(key),
    clear: () => storage.clear()
  }
}

describe('auth store', () => {
  beforeEach(() => {
    vi.stubGlobal('sessionStorage', createStorageMock())
    setActivePinia(createPinia())
  })

  it('builds authorization header from token parts', () => {
    const auth = useAuthStore()
    auth.$patch({ token: 'abc', tokenHead: 'Bearer' })
    expect(auth.authorization).toBe('Bearer abc')
  })

  it('clears local session state', () => {
    const auth = useAuthStore()
    auth.$patch({ token: 'abc', tokenHead: 'Bearer', username: 'test' })
    sessionStorage.setItem('mall-portal-vue3-token', 'abc')
    auth.clearSession()
    expect(auth.authorization).toBe('')
    expect(auth.username).toBe('')
    expect(sessionStorage.getItem('mall-portal-vue3-token')).toBeNull()
  })

  it('reports authenticated when token exists', () => {
    const auth = useAuthStore()
    auth.$patch({ token: 'abc' })
    expect(auth.isAuthenticated).toBe(true)
  })
})
