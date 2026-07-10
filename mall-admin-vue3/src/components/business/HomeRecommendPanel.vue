<template>
  <section class="page-stack">
    <PageHeader :title="title" :description="description" />

    <FilterBar :loading="loading" @search="search" @reset="reset">
      <el-input v-model="filters.keyword" clearable :placeholder="keywordPlaceholder" style="width: 200px" @keyup.enter="search" />
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
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column :prop="nameField" :label="nameLabel" min-width="180" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="推荐" width="90">
        <template #default="{ row }">
          <StatusTag :status="row.recommendStatus ?? 0" :map="{ 1: { label: '推荐', type: 'success' }, 0: { label: '未推荐', type: 'info' } }" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link @click="toggle(row)">切换推荐</el-button>
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
import type { HomeRecommendItem } from '@/api/marketing'
import type { Page } from '@/types/pagination'

const props = defineProps<{
  title: string
  description: string
  keywordPlaceholder: string
  nameField: 'productName' | 'brandName' | 'subjectName'
  nameLabel: string
  fetcher: (params: Record<string, unknown>) => Promise<Page<HomeRecommendItem>>
  toggleStatus: (ids: number[], status: number) => Promise<unknown>
  keywordKey: string
}>()

const { run } = useAsyncTask()
const { filters, tableData, total, pageNum, pageSize, loading, error, search, reset, reload } = useListQuery<
  HomeRecommendItem,
  { keyword?: string }
>({
  fetcher: (query) =>
    props.fetcher({
      [props.keywordKey]: query.keyword,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    }),
  defaultPageSize: 10,
  syncKeys: ['keyword']
})

async function toggle(row: HomeRecommendItem) {
  const next = row.recommendStatus === 1 ? 0 : 1
  await run(() => props.toggleStatus([row.id], next), '已更新')
  reload()
}
</script>
