<template>
  <section class="page-stack">
    <PageHeader title="角色管理" description="角色列表与菜单授权" />

    <FilterBar :loading="loading" @search="search" @reset="reset">
      <el-input v-model="filters.keyword" clearable placeholder="角色名称" style="width: 200px" @keyup.enter="search" />
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
      <el-table-column prop="name" label="角色" min-width="140" />
      <el-table-column prop="description" label="描述" min-width="180" />
      <el-table-column prop="adminCount" label="用户数" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <StatusTag :status="row.status ?? 0" :map="{ 1: { label: '启用', type: 'success' }, 0: { label: '禁用', type: 'info' } }" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openMenuDialog(row)">分配菜单</el-button>
        </template>
      </el-table-column>
    </BaseTable>

    <el-dialog v-model="menuDialogVisible" title="分配菜单" width="480px">
      <el-tree
        ref="treeRef"
        :data="menuTree"
        node-key="id"
        show-checkbox
        default-expand-all
        :props="{ label: 'title', children: 'children' }"
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveMenus">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { ElTree } from 'element-plus'
import PageHeader from '@/components/base/PageHeader.vue'
import FilterBar from '@/components/base/FilterBar.vue'
import BaseTable from '@/components/base/BaseTable.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import { useListQuery } from '@/composables/useListQuery'
import { useAsyncTask } from '@/composables/useAsyncTask'
import { allocRoleMenus, listMenuTree, listRoleMenus, listRoles, type Role } from '@/api/system'
import type { MenuItem } from '@/types/menu'

const menuDialogVisible = ref(false)
const menuTree = ref<MenuItem[]>([])
const currentRoleId = ref(0)
const treeRef = ref<InstanceType<typeof ElTree>>()
const { loading: saving, run } = useAsyncTask()

const { filters, tableData, total, pageNum, pageSize, loading, error, search, reset, reload } = useListQuery<
  Role,
  { keyword?: string }
>({ fetcher: listRoles, defaultPageSize: 10, syncKeys: ['keyword'] })

async function openMenuDialog(row: Role) {
  currentRoleId.value = row.id
  menuTree.value = await listMenuTree()
  const checked = await listRoleMenus(row.id)
  menuDialogVisible.value = true
  setTimeout(() => {
    treeRef.value?.setCheckedKeys(checked.map((item) => item.id))
  }, 0)
}

async function saveMenus() {
  const menuIds = (treeRef.value?.getCheckedKeys(false) as number[]) || []
  await run(() => allocRoleMenus(currentRoleId.value, menuIds), '菜单已分配')
  menuDialogVisible.value = false
}
</script>
