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

function isCatchAllMatch(to: { name?: string | symbol | null; matched: { name?: string | symbol | null }[] }) {
  return to.name === 'not-found' || to.matched.some((route) => route.name === 'not-found')
}

function shouldRetryNavigation(
  to: { fullPath: string; name?: string | symbol | null; matched: { name?: string | symbol | null }[] },
  router: Router
) {
  // 动态路由刚注册时，通配 404 可能已经抢先匹配；需强制重解析一次。
  if (!isCatchAllMatch(to) && to.matched.length > 0) return false
  const resolved = router.resolve(to.fullPath)
  return resolved.matched.length > 0 && resolved.name !== 'not-found'
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

    const permission = to.matched
      .map((route) => route.meta.permission as string | undefined)
      .filter(Boolean)
      .pop()
    if (permission && !auth.hasPermission(permission)) {
      return { name: 'forbidden' }
    }

    if ((to.matched.length === 0 || isCatchAllMatch(to)) && !WHITE_LIST.has(String(to.name))) {
      const resolved = router.resolve(to.fullPath)
      if (resolved.matched.length > 0 && resolved.name !== 'not-found') {
        return to.fullPath
      }
      return { name: 'not-found' }
    }

    return true
  })
}
