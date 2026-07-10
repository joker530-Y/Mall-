import request, { unwrap } from '../http/request'

export interface LoginResult {
  token: string
  tokenHead: string
}

export interface MemberInfo {
  id: number
  username: string
  nickname?: string
  phone?: string
  icon?: string
  integration?: number
  growth?: number
}

export function login(username: string, password: string) {
  const body = new URLSearchParams({ username, password })
  return unwrap<LoginResult>(
    request.post('/sso/login', body, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    })
  )
}

export function getMemberInfo() {
  const token = sessionStorage.getItem('mall-portal-vue3-token')
  const tokenHead = (sessionStorage.getItem('mall-portal-vue3-token-head') || 'Bearer ').trim()
  const headers = token ? { Authorization: `${tokenHead} ${token}` } : undefined
  return unwrap<MemberInfo>(request.get('/sso/info', { headers }))
}
