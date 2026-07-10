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

  it('derives seckill permissions from flash menu', () => {
    const permissions = collectPermissionsFromMenus(sampleMenus)
    expect(permissions).toContain('seckill:manage:read')
    expect(permissions).toContain('seckill:manage:write')
  })

  it('denies unknown permissions when not granted', () => {
    const permission = usePermissionStore()
    permission.initializeMenus([])
    expect(permission.can('seckill:manage:write')).toBe(false)
  })
})
