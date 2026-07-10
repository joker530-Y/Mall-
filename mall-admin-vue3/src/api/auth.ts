import request, { unwrap } from './request'
import type { MenuItem } from '@/types/menu'

export interface LoginParam {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  tokenHead: string
}

export interface AdminInfo {
  username: string
  icon?: string
  roles: string[]
  menus: MenuItem[]
}

export function login(data: LoginParam) {
  return unwrap<LoginResult>(request.post('/admin/login', data))
}

export function logout() {
  return unwrap<null>(request.post('/admin/logout'))
}

export function getCurrentAdminInfo() {
  return unwrap<AdminInfo>(request.get('/admin/info'))
}
