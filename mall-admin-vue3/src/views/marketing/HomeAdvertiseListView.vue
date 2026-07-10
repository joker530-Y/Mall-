<template>
  <section class="page-stack">
    <PageHeader title="广告列表" description="首页轮播与广告位管理" />

    <FilterBar :loading="loading" @search="search" @reset="reset">
      <el-input v-model="filters.name" clearable placeholder="广告名称" style="width: 200px" @keyup.enter="search" />
    </FilterBar>

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
      @page-change="(p) => { pageNum = p; search() }"
      @page-size-change="(s) => { pageSize = s; search() }"
    >
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="type" label="类型" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <StatusTag :status="row.status ?? 0" :map="{ 1: { label: '上线', type: 'success' }, 0: { label: '下线', type: 'info' } }" />
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
import PageHeader from '@/components/base/PageHeader.vue'
import FilterBar from '@/components/base/FilterBar.vue'
import BaseTable from '@/components/base/BaseTable.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import { useListQuery } from '@/composables/useListQuery'
import { useAsyncTask } from '@/composables/useAsyncTask'
import { listHomeAdvertises, updateHomeAdvertiseStatus, type HomeAdvertise } from '@/api/marketing'

const { run } = useAsyncTask()
const { filters, tableData, total, pageNum, pageSize, loading, error, search, reset, reload } = useListQuery<
  HomeAdvertise,
  { name?: string }
>({ fetcher: listHomeAdvertises, defaultPageSize: 10, syncKeys: ['name'] })

async function toggle(row: HomeAdvertise) {
  const next = row.status === 1 ? 0 : 1
  await run(() => updateHomeAdvertiseStatus(row.id, next), '已更新')
  reload()
}
</script>
