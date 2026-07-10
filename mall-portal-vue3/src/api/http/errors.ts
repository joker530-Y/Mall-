export type PortalApiErrorKind = 'network' | 'unauthorized' | 'forbidden' | 'business' | 'unknown'

export class PortalApiError extends Error {
  constructor(
    readonly kind: PortalApiErrorKind,
    message: string,
    readonly code?: number
  ) {
    super(message)
    this.name = 'PortalApiError'
  }

  get retryable() {
    return this.kind === 'network'
  }
}

export function toPortalApiError(error: unknown): PortalApiError {
  if (error instanceof PortalApiError) return error
  const axiosError = error as {
    response?: { status?: number; data?: { message?: string; code?: number } }
    message?: string
    code?: string
  }
  const status = axiosError.response?.status
  const message =
    axiosError.response?.data?.message || axiosError.message || '请求失败'
  if (status === 401) return new PortalApiError('unauthorized', message, status)
  if (status === 403) return new PortalApiError('forbidden', message, status)
  if (axiosError.code === 'ERR_NETWORK' || !axiosError.response) {
    return new PortalApiError('network', '网络异常，请稍后重试')
  }
  const businessCode = axiosError.response?.data?.code
  if (businessCode === 401) return new PortalApiError('unauthorized', message, businessCode)
  if (businessCode === 403) return new PortalApiError('forbidden', message, businessCode)
  if (businessCode && businessCode !== 200) {
    return new PortalApiError('business', message, businessCode)
  }
  return new PortalApiError('unknown', message, status)
}
