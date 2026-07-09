import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const STORAGE_KEY = 'mall-admin-vue3-relation-id'

export function useRelationId(defaultValue = 1) {
  const route = useRoute()
  const router = useRouter()

  const relationId = computed({
    get() {
      const queryValue = Number(route.query.relationId)
      if (Number.isFinite(queryValue) && queryValue > 0) {
        return queryValue
      }
      const storedValue = Number(localStorage.getItem(STORAGE_KEY))
      return Number.isFinite(storedValue) && storedValue > 0 ? storedValue : defaultValue
    },
    set(value: number) {
      localStorage.setItem(STORAGE_KEY, String(value))
      router.replace({ query: { ...route.query, relationId: String(value) } })
    }
  })

  return relationId
}
