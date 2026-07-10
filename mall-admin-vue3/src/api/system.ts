import request, { PageResult, unwrap } from './request'
import type { MenuItem } from '@/types/menu'

export interface AdminUser {
  id: number
  username: string
  nickName?: string
  email?: string
  note?: string
  createTime?: string
  loginTime?: string
  status?: number
  icon?: string
}

export interface Role {
  id: number
  name: string
  description?: string
  adminCount?: number
  status?: number
  sort?: number
  createTime?: string
}

export interface Resource {
  id: number
  name: string
  url?: string
  description?: string
  categoryId?: number
  createTime?: string
}

export interface ResourceCategory {
  id: number
  name: string
  createTime?: string
}

export function listAdmins(params: { keyword?: string; pageNum: number; pageSize: number }) {
  return unwrap<PageResult<AdminUser>>(request.get('/admin/list', { params }))
}

export function updateAdminStatus(id: number, status: number) {
  return unwrap<unknown>(request.post(`/admin/updateStatus/${id}`, null, { params: { status } }))
}

export function listRoles(params: { keyword?: string; pageNum: number; pageSize: number }) {
  return unwrap<PageResult<Role>>(request.get('/role/list', { params }))
}

export function listAllRoles() {
  return unwrap<Role[]>(request.get('/role/listAll'))
}

export function updateRoleStatus(id: number, status: number) {
  return unwrap<unknown>(request.post(`/role/updateStatus/${id}`, null, { params: { status } }))
}

export function listRoleMenus(roleId: number) {
  return unwrap<MenuItem[]>(request.get(`/role/listMenu/${roleId}`))
}

export function allocRoleMenus(roleId: number, menuIds: number[]) {
  return unwrap<unknown>(request.post('/role/allocMenu', null, { params: { roleId, menuIds } }))
}

export function listMenuTree() {
  return unwrap<MenuItem[]>(request.get('/menu/treeList'))
}

export function updateMenuHidden(id: number, hidden: number) {
  return unwrap<unknown>(request.post(`/menu/updateHidden/${id}`, null, { params: { hidden } }))
}

export function listResources(params: {
  categoryId?: number
  nameKeyword?: string
  urlKeyword?: string
  pageNum: number
  pageSize: number
}) {
  return unwrap<PageResult<Resource>>(request.get('/resource/list', { params }))
}

export function listResourceCategories() {
  return unwrap<ResourceCategory[]>(request.get('/resourceCategory/listAll'))
}

export function deleteResource(id: number) {
  return unwrap<unknown>(request.post(`/resource/delete/${id}`))
}
