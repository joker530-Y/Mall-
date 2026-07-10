<template>
  <el-tag :type="tagType" effect="plain">
    <span class="status-text">{{ label }}</span>
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  status: string | number
  map?: Record<string | number, { label: string; type?: 'success' | 'warning' | 'danger' | 'info' }>
}>()

const resolved = computed(() => {
  const fallback = { label: String(props.status), type: 'info' as const }
  return props.map?.[props.status] || fallback
})

const label = computed(() => resolved.value.label)
const tagType = computed(() => resolved.value.type || 'info')
</script>

<style scoped>
.status-text {
  font-weight: 500;
}
</style>
