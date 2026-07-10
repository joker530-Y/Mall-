import { useAuthStore } from '@/stores/auth'

export function usePermission() {
  const auth = useAuthStore()

  function can(permission?: string) {
    return auth.hasPermission(permission || '')
  }

  return { can }
}
