export function resolveInternalRedirect(target?: string | null, fallback = '/') {
  if (!target || typeof target !== 'string') return fallback
  if (!target.startsWith('/') || target.startsWith('//')) return fallback
  return target
}
