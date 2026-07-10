import { defineStore } from 'pinia'
import type { RouteRecordRaw } from 'vue-router'
import type { MenuItem, SidebarItem } from '@/types/menu'
import { BOOTSTRAP_ROUTES, buildRoutesFromMenus, buildSidebarFromMenus, MENU_ROUTE_REGISTRY } from '@/router/route-map'

function collectMenuNames(menus: MenuItem[]): Set<string> {
  const names = new Set<string>()
  const walk = (items: MenuItem[]) => {
    for (const item of items) {
      names.add(item.name)
      if (item.children?.length) walk(item.children)
    }
  }
  walk(menus)
  return names
}

export function collectPermissionsFromMenus(menus: MenuItem[]): string[] {
  const names = collectMenuNames(menus)
  const permissions = new Set<string>()
  for (const name of names) {
    const module = MENU_ROUTE_REGISTRY[name]
    if (module?.meta.permission) permissions.add(module.meta.permission)
  }
  if (names.has('flash')) {
    permissions.add('seckill:manage:read')
  }
  if (names.has('product') || names.has('addProduct')) {
    permissions.add('product:edit')
  }
  if (names.has('order')) {
    permissions.add('order:read')
  }
  return [...permissions]
}

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    menus: [] as MenuItem[],
    permissions: [] as string[],
    routesAdded: false
  }),
  getters: {
    sidebarMenus(state): SidebarItem[] {
      const backend = buildSidebarFromMenus(state.menus)
      const bootstrap = BOOTSTRAP_ROUTES
        .filter(
          (route) =>
            !route.redirect &&
            !route.meta?.hidden &&
            Boolean(route.meta?.title) &&
            typeof route.path === 'string' &&
            !route.path.includes(':')
        )
        .map((route) => ({
          path: route.path,
          title: String(route.meta?.title || route.name),
          icon: String(route.meta?.icon || '')
        }))
      const seen = new Set<string>()
      return [...bootstrap, ...backend].filter((item) => {
        if (seen.has(item.path)) return false
        seen.add(item.path)
        return true
      })
    }
  },
  actions: {
    initializeMenus(menus: MenuItem[]) {
      this.menus = menus
      this.permissions = collectPermissionsFromMenus(menus)
    },
    buildDynamicRoutes(): RouteRecordRaw[] {
      return [...BOOTSTRAP_ROUTES, ...buildRoutesFromMenus(this.menus)]
    },
    markRoutesAdded() {
      this.routesAdded = true
    },
    can(permission: string) {
      if (!permission) return true
      return this.permissions.includes(permission)
    },
    reset() {
      this.menus = []
      this.permissions = []
      this.routesAdded = false
    }
  }
})
