<template>
  <el-tag :type="tagType" effect="plain">{{ label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ORDER_STATUS_MAP } from '@/constants/status'

const props = defineProps<{ status: number }>()
const resolved = computed(() => ORDER_STATUS_MAP[props.status] || { label: String(props.status), type: 'info' as const })
const label = computed(() => resolved.value.label)
const tagType = computed(() => resolved.value.type)
</script>
