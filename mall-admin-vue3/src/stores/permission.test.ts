import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { collectPermissionsFromMenus, usePermissionStore } from './permission'
import type { MenuItem } from '@/types/menu'

const sampleMenus: MenuItem[] = [
  { id: 12, parentId: 0, title: '营销', level: 0, sort: 0, name: 'sms', icon: 'sms', hidden: 0 },
  { id: 13, parentId: 12, title: '秒杀活动列表', level: 1, sort: 0, name: 'flash', icon: 'sms-flash', hidden: 0 }
]

describe('permission store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('maps backend flash menu to seckill route', () => {
    const permission = usePermissionStore()
    permission.initializeMenus(sampleMenus)
    const routes = permission.buildDynamicRoutes()
    expect(routes.some((route) => route.path === '/marketing/flash-promotions')).toBe(true)
  })

  it('always includes dashboard bootstrap route', () => {
    const permission = usePermissionStore()
    permission.initializeMenus([])
    const routes = permission.buildDynamicRoutes()
    expect(routes.some((route) => route.path === '/dashboard')).toBe(true)
    expect(routes.some((route) => route.path === '/seckill/dashboard')).toBe(false)
  })

  it('derives seckill read permission from flash menu', () => {
    const permissions = collectPermissionsFromMenus(sampleMenus)
    expect(permissions).toContain('seckill:manage:read')
    expect(permissions).not.toContain('seckill:manage:write')
  })

  it('derives product and order permissions from menus', () => {
    const menus: MenuItem[] = [
      { id: 1, parentId: 0, title: '商品', level: 0, sort: 0, name: 'pms', icon: 'product', hidden: 0 },
      { id: 2, parentId: 1, title: '商品列表', level: 1, sort: 0, name: 'product', icon: 'product-list', hidden: 0 },
      { id: 3, parentId: 0, title: '订单', level: 0, sort: 0, name: 'oms', icon: 'order', hidden: 0 },
      { id: 4, parentId: 3, title: '订单列表', level: 1, sort: 0, name: 'order', icon: 'order-list', hidden: 0 }
    ]
    const permissions = collectPermissionsFromMenus(menus)
    expect(permissions).toContain('product:edit')
    expect(permissions).toContain('order:read')
  })

  it('denies unknown permissions when not granted', () => {
    const permission = usePermissionStore()
    permission.initializeMenus([])
    expect(permission.can('seckill:manage:write')).toBe(false)
  })
})
