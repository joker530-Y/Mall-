import request, { unwrap } from './request'

export interface LoginParam {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  tokenHead: string
}

export function login(data: LoginParam) {
  return unwrap<LoginResult>(request.post('/admin/login', data))
}

export function logout() {
  return unwrap<null>(request.post('/admin/logout'))
}
