import { describe, expect, it } from 'vitest'
import { formatDateTime, formatMoney } from './format'

describe('format utils', () => {
  it('formats datetime strings', () => {
    expect(formatDateTime('2026-07-10T12:00:00')).toBe('2026-07-10 12:00:00')
  })

  it('formats money with two decimals', () => {
    expect(formatMoney(12.5)).toBe('12.50')
  })

  it('returns dash for empty money values', () => {
    expect(formatMoney(null)).toBe('-')
    expect(formatMoney(undefined)).toBe('-')
  })
})
