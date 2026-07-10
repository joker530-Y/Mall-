import axios from 'axios'
import { ElMessage } from 'element-plus'
import { ApiError, toApiError } from './http/errors'
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

let requestSequence = 0

function readAuthorization() {
  try {
    const auth = useAuthStore()
    if (auth.authorization) return auth.authorization
  } catch {
    // Pinia 尚未就绪
  }
  return ''
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
  const auth = useAuthStore()
  auth.clearSession()
  ElMessage.error(message || '登录已失效，请重新登录')
  const redirect = encodeURIComponent(window.location.pathname + window.location.search)
  if (!window.location.pathname.startsWith('/login')) {
    window.location.assign(`/login?redirect=${redirect}`)
  }
}

function handleForbidden(message: string) {
  ElMessage.error(message || '无权限访问')
  if (!window.location.pathname.startsWith('/403')) {
    window.location.assign('/403')
  }
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/admin',
  timeout: 12000,
  paramsSerializer: {
    indexes: null
  }
})

request.interceptors.request.use((config) => {
  const authorization = readAuthorization()
  if (authorization) {
    config.headers.Authorization = authorization
  }
  const seq = ++requestSequence
  config.headers['X-Request-Seq'] = String(seq)
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
        return Promise.reject(new ApiError('unauthorized', message, result.code))
      }
      if (result.code === 403) {
        handleForbidden(message)
        return Promise.reject(new ApiError('forbidden', message, result.code))
      }
      const error = new ApiError('business', message, result.code)
      ElMessage.error(error.message)
      return Promise.reject(error)
    }
    return response
  },
  (error) => {
    const apiError = toApiError(error)
    const requestAuthorization = String(error?.config?.headers?.Authorization || '')
    if (apiError.kind === 'unauthorized') {
      handleUnauthorized(apiError.message, requestAuthorization)
      return Promise.reject(apiError)
    }
    if (apiError.kind === 'forbidden') {
      handleForbidden(apiError.message)
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

export function createAbortConfig(signal?: AbortSignal) {
  return signal ? { signal } : {}
}

export { ApiError }
export default request
