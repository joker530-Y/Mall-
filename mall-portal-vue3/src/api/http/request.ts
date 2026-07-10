import axios from 'axios'
import { ElMessage } from 'element-plus'
import { PortalApiError, toPortalApiError } from './errors'
import { useAuthStore } from '@/stores/auth'
import { resolveInternalRedirect } from '@/utils/navigation'

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

function handleUnauthorized(message: string) {
  const auth = useAuthStore()
  auth.clearSession()
  ElMessage.error(message || '登录已失效，请重新登录')
  const redirect = encodeURIComponent(window.location.pathname + window.location.search)
  if (!window.location.pathname.startsWith('/login')) {
    window.location.assign(`/login?redirect=${redirect}`)
  }
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/portal',
  timeout: 15000,
  paramsSerializer: {
    indexes: null
  }
})

request.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.authorization) {
    config.headers.Authorization = auth.authorization
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
      if (result.code === 401) {
        handleUnauthorized(message)
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
    if (apiError.kind === 'unauthorized') {
      handleUnauthorized(apiError.message)
      return Promise.reject(apiError)
    }
    if (apiError.kind !== 'forbidden') {
      ElMessage.error(apiError.message)
    }
    return Promise.reject(apiError)
  }
)

export async function unwrap<T>(promise: Promise<{ data: CommonResult<T> }>): Promise<T> {
  const response = await promise
  return response.data.data
}

export { PortalApiError }
export default request
