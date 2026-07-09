import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

export interface CommonResult<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  pageNum: number
  pageSize: number
  totalPage?: number
  total: number
  list: T[]
}

const request = axios.create({
  baseURL: '/api/admin',
  timeout: 12000
})

request.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.authorization) {
    config.headers.Authorization = auth.authorization
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const result = response.data as CommonResult<unknown>
    if (result && typeof result.code === 'number' && result.code !== 200) {
      ElMessage.error(result.message || '请求失败')
      return Promise.reject(new Error(result.message || '请求失败'))
    }
    return response
  },
  (error) => {
    ElMessage.error(error?.message || '网络异常')
    return Promise.reject(error)
  }
)

export async function unwrap<T>(promise: Promise<{ data: CommonResult<T> }>): Promise<T> {
  const response = await promise
  return response.data.data
}

export default request
