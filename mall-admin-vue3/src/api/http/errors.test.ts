import { describe, expect, it } from 'vitest'
import { ApiError, toApiError } from './errors'

describe('ApiError', () => {
  it('marks network errors as retryable', () => {
    const error = new ApiError('network', '网络异常')
    expect(error.retryable).toBe(true)
  })

  it('converts 401 responses to unauthorized errors', () => {
    const error = toApiError({ response: { status: 401, data: { message: '未登录' } } })
    expect(error.kind).toBe('unauthorized')
    expect(error.message).toBe('未登录')
  })

  it('converts 403 responses to forbidden errors', () => {
    const error = toApiError({ response: { status: 403, data: { message: '无权限' } } })
    expect(error.kind).toBe('forbidden')
  })

  it('converts business code 401 to unauthorized errors', () => {
    const error = toApiError({ response: { status: 200, data: { code: 401, message: '未登录' } } })
    expect(error.kind).toBe('unauthorized')
  })
})
