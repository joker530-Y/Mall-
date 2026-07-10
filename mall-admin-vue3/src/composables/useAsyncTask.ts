import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ApiError } from '@/api/http/errors'

export function useAsyncTask() {
  const loading = ref(false)
  const error = ref('')

  async function run<T>(task: () => Promise<T>, successMessage?: string) {
    if (loading.value) return undefined
    loading.value = true
    error.value = ''
    try {
      const result = await task()
      if (successMessage) ElMessage.success(successMessage)
      return result
    } catch (err) {
      error.value = err instanceof ApiError ? err.message : '操作失败'
      if (!(err instanceof ApiError) || err.kind !== 'business') {
        ElMessage.error(error.value)
      }
      throw err
    } finally {
      loading.value = false
    }
  }

  return { loading, error, run }
}
