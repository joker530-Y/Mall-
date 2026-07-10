<template>
  <section class="page-stack">
    <PageHeader title="资源管理" description="接口资源与权限分类" />

    <FilterBar :loading="loading" @search="search" @reset="reset">
      <el-input v-model="filters.nameKeyword" clearable placeholder="资源名称" style="width: 180px" @keyup.enter="search" />
      <el-input v-model="filters.urlKeyword" clearable placeholder="资源 URL" style="width: 180px" @keyup.enter="search" />
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
      <el-table-column prop="url" label="URL" min-width="200" show-overflow-tooltip />
      <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-popconfirm title="确认删除？" @confirm="remove(row.id)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
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
import { useListQuery } from '@/composables/useListQuery'
import { useAsyncTask } from '@/composables/useAsyncTask'
import { deleteResource, listResources, type Resource } from '@/api/system'

const { run } = useAsyncTask()
const { filters, tableData, total, pageNum, pageSize, loading, error, search, reset, reload } = useListQuery<
  Resource,
  { nameKeyword?: string; urlKeyword?: string }
>({ fetcher: listResources, defaultPageSize: 10, syncKeys: ['nameKeyword', 'urlKeyword'] })

async function remove(id: number) {
  await run(() => deleteResource(id), '已删除')
  reload()
}
</script>
