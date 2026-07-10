<template>
  <section class="page-stack">
    <PageHeader title="品牌管理" description="品牌列表与显示状态" />

    <FilterBar :loading="loading" @search="search" @reset="reset">
      <el-input v-model="filters.keyword" clearable placeholder="品牌名称" style="width: 200px" @keyup.enter="search" />
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
      <el-table-column prop="name" label="品牌" min-width="160" />
      <el-table-column prop="firstLetter" label="首字母" width="80" />
      <el-table-column prop="productCount" label="商品数" width="90" />
      <el-table-column label="显示" width="90">
        <template #default="{ row }">
          <StatusTag :status="row.showStatus ?? 0" :map="{ 1: { label: '显示', type: 'success' }, 0: { label: '隐藏', type: 'info' } }" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-popconfirm title="切换显示状态？" @confirm="toggleShow(row)">
            <template #reference>
              <el-button link>切换显示</el-button>
            </template>
          </el-popconfirm>
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
import { listBrands, updateBrandShowStatus, type Brand } from '@/api/product'

const { run } = useAsyncTask()
const { filters, tableData, total, pageNum, pageSize, loading, error, search, reset, reload } = useListQuery<
  Brand,
  { keyword?: string }
>({ fetcher: listBrands, defaultPageSize: 10, syncKeys: ['keyword'] })

async function toggleShow(row: Brand) {
  const next = row.showStatus === 1 ? 0 : 1
  await run(() => updateBrandShowStatus([row.id], next), '已更新')
  reload()
}
</script>
