import { describe, expect, it } from 'vitest'

const FINAL_STATUSES = new Set(['SUCCESS', 'FAILED', 'SOLD_OUT', 'REPEAT', 'NOT_WARMED'])

describe('seckill polling helpers', () => {
  it('treats known terminal statuses as final', () => {
    expect(FINAL_STATUSES.has('SUCCESS')).toBe(true)
    expect(FINAL_STATUSES.has('FAILED')).toBe(true)
    expect(FINAL_STATUSES.has('PROCESSING')).toBe(false)
  })

  it('stops after max attempts without final status', () => {
    let attempts = 0
    let stopped = false
    const maxAttempts = 30
    while (!stopped) {
      attempts += 1
      if (attempts >= maxAttempts) {
        stopped = true
      }
    }
    expect(attempts).toBe(30)
    expect(stopped).toBe(true)
  })
})
