<template>
  <section class="page-stack">
    <PageHeader title="菜单管理" description="后台菜单树与隐藏状态" />

    <BaseTable :data="flatMenus" :loading="loading" :error="error" height="560" @retry="load">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="name" label="前端名" width="140" />
      <el-table-column prop="level" label="级别" width="80" />
      <el-table-column label="隐藏" width="90">
        <template #default="{ row }">
          <StatusTag :status="row.hidden ?? 0" :map="{ 1: { label: '隐藏', type: 'info' }, 0: { label: '显示', type: 'success' } }" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link @click="toggleHidden(row)">切换隐藏</el-button>
        </template>
      </el-table-column>
    </BaseTable>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/base/PageHeader.vue'
import BaseTable from '@/components/base/BaseTable.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import { listMenuTree, updateMenuHidden } from '@/api/system'
import { useAsyncTask } from '@/composables/useAsyncTask'
import type { MenuItem } from '@/types/menu'

const flatMenus = ref<MenuItem[]>([])
const loading = ref(false)
const error = ref('')
const { run } = useAsyncTask()

function flatten(items: MenuItem[], level = 0): MenuItem[] {
  return items.flatMap((item) => [
    { ...item, title: `${'—'.repeat(level)}${item.title}` },
    ...flatten(item.children || [], level + 1)
  ])
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    flatMenus.value = flatten(await listMenuTree())
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function toggleHidden(row: MenuItem) {
  const next = row.hidden === 1 ? 0 : 1
  await run(() => updateMenuHidden(row.id, next), '已更新')
  load()
}

onMounted(load)
</script>
