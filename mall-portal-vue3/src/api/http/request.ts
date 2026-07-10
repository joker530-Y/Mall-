import axios from 'axios'
import { ElMessage } from 'element-plus'
import { PortalApiError, toPortalApiError } from './errors'
import { useAuthStore } from '@/stores/auth'

export interface CommonResult<T> {
  code: number
  message: string
  data: T
}

export type PageResult<T> = {
  pageNum: number
  pageSize: number
  totalPage?: number
  total: number
  list: T[]
}

const TOKEN_KEY = 'mall-portal-vue3-token'
const TOKEN_HEAD_KEY = 'mall-portal-vue3-token-head'
const AUTH_WHITELIST = ['/sso/login', '/sso/register', '/sso/getAuthCode']

function readAuthorization() {
  try {
    const auth = useAuthStore()
    if (auth.authorization) return auth.authorization
  } catch {
    // Pinia 尚未就绪时回退到 sessionStorage
  }
  const token = sessionStorage.getItem(TOKEN_KEY)
  if (!token) return ''
  const tokenHead = (sessionStorage.getItem(TOKEN_HEAD_KEY) || 'Bearer ').trim()
  return `${tokenHead} ${token}`
}

function handleUnauthorized(message: string, requestAuthorization?: string) {
  const currentAuthorization = readAuthorization()
  // 忽略过期请求：旧 token 的 401 不应清掉刚登录的新会话
  if (
    requestAuthorization &&
    currentAuthorization &&
    requestAuthorization !== currentAuthorization
  ) {
    return
  }
  try {
    useAuthStore().clearSession()
  } catch {
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(TOKEN_HEAD_KEY)
  }
  if (window.location.pathname.startsWith('/login')) {
    return
  }
  ElMessage.error(message || '登录已失效，请重新登录')
  const redirect = encodeURIComponent(window.location.pathname + window.location.search)
  window.location.assign(`/login?redirect=${redirect}`)
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/portal',
  timeout: 15000,
  paramsSerializer: {
    indexes: null
  }
})

request.interceptors.request.use((config) => {
  const url = config.url || ''
  const skipAuth = AUTH_WHITELIST.some((path) => url.includes(path))
  if (!skipAuth) {
    const authorization = readAuthorization()
    if (authorization) {
      config.headers.Authorization = authorization
    }
  } else if (config.headers) {
    delete config.headers.Authorization
  }
  if (!config.headers['X-Request-Id']) {
    config.headers['X-Request-Id'] = crypto.randomUUID()
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const result = response.data as CommonResult<unknown>
    if (result && typeof result.code === 'number' && result.code !== 200) {
      const message = result.message || '请求失败'
      const requestAuthorization = String(response.config.headers?.Authorization || '')
      if (result.code === 401) {
        handleUnauthorized(message, requestAuthorization)
        return Promise.reject(new PortalApiError('unauthorized', message, result.code))
      }
      if (result.code === 403) {
        const error = new PortalApiError('forbidden', message, result.code)
        ElMessage.error(error.message)
        return Promise.reject(error)
      }
      const error = new PortalApiError('business', message, result.code)
      ElMessage.error(error.message)
      return Promise.reject(error)
    }
    return response
  },
  (error) => {
    const apiError = toPortalApiError(error)
    const requestAuthorization = String(error?.config?.headers?.Authorization || '')
    if (apiError.kind === 'unauthorized') {
      handleUnauthorized(apiError.message, requestAuthorization)
      return Promise.reject(apiError)
    }
    ElMessage.error(apiError.message)
    return Promise.reject(apiError)
  }
)

export async function unwrap<T>(promise: Promise<{ data: CommonResult<T> }>): Promise<T> {
  const response = await promise
  return response.data.data
}

export { PortalApiError }
export default request
