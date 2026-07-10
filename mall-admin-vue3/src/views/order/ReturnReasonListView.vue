<template>
  <section class="page-stack">
    <PageHeader title="退货原因" description="维护退货原因字典" />

    <BaseTable
      :data="tableData"
      :loading="loading"
      :error="error"
      :total="total"
      :page-num="pageNum"
      :page-size="pageSize"
      show-pagination
      height="560"
      @retry="reload"
      @page-change="(p) => { pageNum = p; reload() }"
      @page-size-change="(s) => { pageSize = s; reload() }"
    >
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="name" label="原因" min-width="200" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <StatusTag :status="row.status ?? 0" :map="{ 1: { label: '启用', type: 'success' }, 0: { label: '禁用', type: 'info' } }" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link @click="toggle(row)">切换状态</el-button>
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
import { listReturnReasons, updateReturnReasonStatus, type ReturnReason } from '@/api/order'
import { useAsyncTask } from '@/composables/useAsyncTask'

const tableData = ref<ReturnReason[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const error = ref('')
const { run } = useAsyncTask()

async function reload() {
  loading.value = true
  error.value = ''
  try {
    const page = await listReturnReasons(pageNum.value, pageSize.value)
    tableData.value = page.list
    total.value = page.total
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function toggle(row: ReturnReason) {
  const next = row.status === 1 ? 0 : 1
  await run(() => updateReturnReasonStatus([row.id], next), '已更新')
  reload()
}

onMounted(reload)
</script>
