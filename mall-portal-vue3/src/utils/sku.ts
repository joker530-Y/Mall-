import type { SkuStock } from '@/api/modules/product'

export type SkuSpec = { key: string; value: string }

export function parseSkuSpecs(sku: SkuStock): SkuSpec[] {
  if (sku.spData) {
    try {
      const parsed = JSON.parse(sku.spData) as unknown
      if (Array.isArray(parsed)) {
        return parsed.filter(
          (item): item is SkuSpec =>
            Boolean(item) &&
            typeof (item as SkuSpec).key === 'string' &&
            typeof (item as SkuSpec).value === 'string'
        )
      }
    } catch {
      // ignore malformed spData
    }
  }
  const legacy: SkuSpec[] = []
  if (sku.sp1) legacy.push({ key: '规格1', value: sku.sp1 })
  if (sku.sp2) legacy.push({ key: '规格2', value: sku.sp2 })
  if (sku.sp3) legacy.push({ key: '规格3', value: sku.sp3 })
  return legacy
}

export function formatSkuLabel(sku: SkuStock): string {
  const specs = parseSkuSpecs(sku)
  if (specs.length) {
    return specs.map((item) => `${item.key}:${item.value}`).join(' / ')
  }
  return `SKU ${sku.id}`
}

export function formatSkuAttrText(sku: SkuStock): string {
  const specs = parseSkuSpecs(sku)
  if (specs.length) {
    return specs.map((item) => `${item.key}:${item.value}`).join(';')
  }
  return [sku.sp1, sku.sp2, sku.sp3].filter(Boolean).join(';')
}

export function buildSkuAttributeGroups(skus: SkuStock[]) {
  const groups = new Map<string, Set<string>>()
  for (const sku of skus) {
    for (const spec of parseSkuSpecs(sku)) {
      if (!groups.has(spec.key)) groups.set(spec.key, new Set())
      groups.get(spec.key)!.add(spec.value)
    }
  }
  return [...groups.entries()].map(([name, values]) => ({
    name,
    values: [...values]
  }))
}

export function findSkuByAttrs(skus: SkuStock[], selectedAttrs: Record<string, string>) {
  const entries = Object.entries(selectedAttrs).filter(([, value]) => Boolean(value))
  if (!entries.length) {
    return skus.find((sku) => (sku.stock ?? 0) > 0) || skus[0]
  }
  return skus.find((sku) => {
    const specs = parseSkuSpecs(sku)
    return entries.every(([key, value]) => specs.some((spec) => spec.key === key && spec.value === value))
  })
}
