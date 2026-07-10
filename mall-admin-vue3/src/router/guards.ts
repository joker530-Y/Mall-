import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { usePermissionStore } from '@/stores/permission'
import { resolveInternalRedirect } from '@/utils/navigation'

const WHITE_LIST = new Set(['login', 'forbidden', 'not-found'])

export async function ensureDynamicRoutes(router: Router) {
  const auth = useAuthStore()
  const permission = usePermissionStore()
  if (!auth.isAuthenticated) return false
  if (permission.routesAdded) return true

  if (!auth.username) {
    try {
      await auth.bootstrap()
    } catch {
      auth.clearSession()
      return false
    }
  }

  const dynamicRoutes = permission.buildDynamicRoutes()
  dynamicRoutes.forEach((route) => router.addRoute('root', route))
  permission.markRoutesAdded()
  return true
}

function shouldRetryNavigation(to: { fullPath: string; matched: { path: string }[] }, router: Router) {
  return to.matched.length === 0 && router.resolve(to.fullPath).matched.length > 0
}

export function setupRouterGuards(router: Router) {
  router.beforeEach(async (to) => {
    const auth = useAuthStore()

    if (to.name === 'login') {
      if (auth.isAuthenticated) {
        await ensureDynamicRoutes(router)
        const redirect = resolveInternalRedirect(
          typeof to.query.redirect === 'string' ? to.query.redirect : undefined,
          '/dashboard'
        )
        return redirect
      }
      return true
    }

    if (!auth.isAuthenticated) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }

    const wasAdded = !usePermissionStore().routesAdded
    const ready = await ensureDynamicRoutes(router)
    if (!ready) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
    if (wasAdded && shouldRetryNavigation(to, router)) {
      return to.fullPath
    }

    const permission = to.meta.permission as string | undefined
    if (permission && !auth.hasPermission(permission)) {
      return { name: 'forbidden' }
    }

    if (to.matched.length === 0 && !WHITE_LIST.has(String(to.name))) {
      return { name: 'not-found' }
    }

    return true
  })
}
