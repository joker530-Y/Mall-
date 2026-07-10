import { onScopeDispose, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { Page } from '@/types/pagination'
import { ApiError } from '@/api/http/errors'

export interface ListQueryState {
  pageNum: number
  pageSize: number
  keyword?: string
  [key: string]: string | number | undefined
}

interface UseListQueryOptions<T, Q extends Record<string, unknown>> {
  fetcher: (query: Q & { pageNum: number; pageSize: number }) => Promise<Page<T>>
  defaultQuery?: Partial<Q>
  defaultPageSize?: number
  debounceMs?: number
  syncKeys?: (keyof Q)[]
}

export function useListQuery<T, Q extends Record<string, unknown> = Record<string, never>>(
  options: UseListQueryOptions<T, Q>
) {
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
  let debounceTimer: ReturnType<typeof setTimeout> | null = null

  function buildQuery() {
    return {
      ...filters.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    } as Q & { pageNum: number; pageSize: number }
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
      error.value = err instanceof ApiError ? err.message : '加载失败'
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

  function scheduleSearch() {
    if (debounceTimer) clearTimeout(debounceTimer)
    debounceTimer = setTimeout(search, options.debounceMs ?? 300)
  }

  function restoreFromRoute() {
    pageNum.value = Number(route.query.pageNum) || 1
    pageSize.value = Number(route.query.pageSize) || options.defaultPageSize || 10
    const keys = options.syncKeys || (Object.keys(filters.value) as (keyof Q)[])
    keys.forEach((key) => {
      const value = route.query[String(key)]
      if (typeof value === 'string') {
        ;(filters.value as Record<string, unknown>)[String(key)] = value
      }
    })
  }

  restoreFromRoute()
  reload()

  watch(
    () => route.query,
    () => {
      restoreFromRoute()
      reload()
    }
  )

  onScopeDispose(() => {
    abortController?.abort()
    if (debounceTimer) clearTimeout(debounceTimer)
  })

  return {
    filters,
    tableData,
    total,
    pageNum,
    pageSize,
    loading,
    error,
    search,
    scheduleSearch,
    reset,
    reload
  }
}
