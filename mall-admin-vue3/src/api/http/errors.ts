export type ApiErrorKind = 'network' | 'unauthorized' | 'forbidden' | 'business' | 'unknown'

export class ApiError extends Error {
  constructor(
    readonly kind: ApiErrorKind,
    message: string,
    readonly code?: number,
  ) {
    super(message)
    this.name = 'ApiError'
  }

  get retryable() {
    return this.kind === 'network'
  }
}

export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error

  const axiosError = error as {
    response?: { status?: number; data?: { message?: string; code?: number } }
    message?: string
    code?: string
  }

  const status = axiosError.response?.status
  const message =
    axiosError.response?.data?.message ||
    axiosError.message ||
    '请求失败'

  if (status === 401) return new ApiError('unauthorized', message, status)
  if (status === 403) return new ApiError('forbidden', message, status)
  if (axiosError.code === 'ERR_NETWORK' || !axiosError.response) {
    return new ApiError('network', '网络异常，请稍后重试')
  }
  const businessCode = axiosError.response?.data?.code
  if (businessCode === 401) return new ApiError('unauthorized', message, businessCode)
  if (businessCode === 403) return new ApiError('forbidden', message, businessCode)
  if (businessCode && businessCode !== 200) {
    return new ApiError('business', message, businessCode)
  }
  return new ApiError('unknown', message, status)
}
