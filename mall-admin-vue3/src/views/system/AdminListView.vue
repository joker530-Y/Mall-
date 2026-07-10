<template>
  <section class="page-stack">
    <PageHeader title="管理员" description="后台账号与启用状态" />

    <FilterBar :loading="loading" @search="search" @reset="reset">
      <el-input v-model="filters.keyword" clearable placeholder="用户名/姓名" style="width: 200px" @keyup.enter="search" />
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
      <el-table-column prop="username" label="账号" width="140" />
      <el-table-column prop="nickName" label="昵称" width="120" />
      <el-table-column prop="email" label="邮箱" min-width="160" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <StatusTag :status="row.status ?? 0" :map="{ 1: { label: '启用', type: 'success' }, 0: { label: '禁用', type: 'danger' } }" />
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
import { listAdmins, updateAdminStatus, type AdminUser } from '@/api/system'

const { run } = useAsyncTask()
const { filters, tableData, total, pageNum, pageSize, loading, error, search, reset, reload } = useListQuery<
  AdminUser,
  { keyword?: string }
>({ fetcher: listAdmins, defaultPageSize: 10, syncKeys: ['keyword'] })

async function toggle(row: AdminUser) {
  const next = row.status === 1 ? 0 : 1
  await run(() => updateAdminStatus(row.id, next), '已更新')
  reload()
}
</script>
