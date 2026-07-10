export function formatDateTime(value?: string | Date | null) {
  if (!value) return '-'
  const text = typeof value === 'string' ? value : value.toISOString()
  return text.replace('T', ' ').slice(0, 19)
}

export function formatMoney(value?: number | string | null) {
  if (value === null || value === undefined || value === '') return '-'
  const num = Number(value)
  if (Number.isNaN(num)) return String(value)
  return num.toFixed(2)
}

export function formatYesNo(value?: number | null, yes = '是', no = '否') {
  return value === 1 ? yes : no
}
