import { describe, expect, it } from 'vitest'
import type { SkuStock } from '@/api/modules/product'
import {
  buildSkuAttributeGroups,
  findSkuByAttrs,
  formatSkuLabel,
  parseSkuSpecs
} from '@/utils/sku'

const skus: SkuStock[] = [
  {
    id: 1,
    productId: 38,
    price: 3599,
    stock: 10,
    spData: JSON.stringify([
      { key: '颜色', value: '银色' },
      { key: '容量', value: '64G' }
    ])
  },
  {
    id: 2,
    productId: 38,
    price: 4799,
    stock: 0,
    spData: JSON.stringify([
      { key: '颜色', value: '银色' },
      { key: '容量', value: '256G' }
    ])
  },
  {
    id: 3,
    productId: 38,
    price: 3599,
    stock: 5,
    spData: JSON.stringify([
      { key: '颜色', value: '蓝色' },
      { key: '容量', value: '64G' }
    ])
  }
]

describe('sku utils', () => {
  it('parses spData specs', () => {
    expect(parseSkuSpecs(skus[0])).toEqual([
      { key: '颜色', value: '银色' },
      { key: '容量', value: '64G' }
    ])
  })

  it('builds attribute groups from skus', () => {
    expect(buildSkuAttributeGroups(skus)).toEqual([
      { name: '颜色', values: ['银色', '蓝色'] },
      { name: '容量', values: ['64G', '256G'] }
    ])
  })

  it('finds matching sku and formats label', () => {
    const matched = findSkuByAttrs(skus, { 颜色: '蓝色', 容量: '64G' })
    expect(matched?.id).toBe(3)
    expect(formatSkuLabel(matched!)).toContain('蓝色')
  })

  it('falls back to first in-stock sku when attrs empty', () => {
    expect(findSkuByAttrs(skus, {})?.id).toBe(1)
  })
})
