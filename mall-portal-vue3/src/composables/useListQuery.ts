import { onScopeDispose, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { Page } from '@/types/pagination'
import { PortalApiError } from '@/api/http/errors'

export function useListQuery<T, Q extends Record<string, unknown> = Record<string, never>>(options: {
  fetcher: (query: Q & { pageNum: number; pageSize: number }) => Promise<Page<T>>
  defaultQuery?: Partial<Q>
  defaultPageSize?: number
  syncKeys?: (keyof Q)[]
}) {
  const route = useRoute()
  const router = useRouter()
  const tableData = ref<T[]>([])
  const total = ref(0)
  const loading = ref(false)
  const error = ref('')
  const pageNum = ref(Number(route.query.pageNum) || 1)
  const pageSize = ref(Number(route.query.pageSize) || options.defaultPageSize || 10)
  const filters = ref({ ...(options.defaultQuery || {}) } as Q)

  let abortController: AbortController | null = null

  function buildQuery() {
    return { ...filters.value, pageNum: pageNum.value, pageSize: pageSize.value } as Q & {
      pageNum: number
      pageSize: number
    }
  }

  function syncRouteQuery() {
    const query: Record<string, string> = {
      pageNum: String(pageNum.value),
      pageSize: String(pageSize.value)
    }
    const keys = options.syncKeys || (Object.keys(filters.value) as (keyof Q)[])
    keys.forEach((key) => {
      const value = filters.value[key]
      if (value !== undefined && value !== null && value !== '') {
        query[String(key)] = String(value)
      }
    })
    router.replace({ query: { ...route.query, ...query } })
  }

  async function reload() {
    abortController?.abort()
    abortController = new AbortController()
    loading.value = true
    error.value = ''
    try {
      const page = await options.fetcher(buildQuery())
      if (abortController.signal.aborted) return
      tableData.value = page.list
      total.value = page.total
    } catch (err) {
      if (abortController.signal.aborted) return
      tableData.value = []
      total.value = 0
      error.value = err instanceof PortalApiError ? err.message : '加载失败'
    } finally {
      if (!abortController.signal.aborted) loading.value = false
    }
  }

  function search() {
    pageNum.value = 1
    syncRouteQuery()
    reload()
  }

  function reset() {
    filters.value = { ...(options.defaultQuery || {}) } as Q
    pageNum.value = 1
    pageSize.value = options.defaultPageSize || 10
    syncRouteQuery()
    reload()
  }

  reload()
  watch(() => route.query, reload)

  onScopeDispose(() => abortController?.abort())

  return { filters, tableData, total, pageNum, pageSize, loading, error, search, reset, reload }
}
