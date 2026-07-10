<template>
  <section class="page-stack">
    <PageHeader title="优惠券" description="优惠券列表与管理" />

    <FilterBar :loading="loading" @search="search" @reset="reset">
      <el-input v-model="filters.name" clearable placeholder="优惠券名称" style="width: 200px" @keyup.enter="search" />
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
      <el-table-column label="类型" width="120">
        <template #default="{ row }">{{ COUPON_TYPE_MAP[row.type ?? 0] || row.type }}</template>
      </el-table-column>
      <el-table-column label="面额" width="90">
        <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
      </el-table-column>
      <el-table-column label="门槛" width="90">
        <template #default="{ row }">{{ formatMoney(row.minPoint) }}</template>
      </el-table-column>
      <el-table-column prop="useCount" label="已使用" width="80" />
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
import { deleteCoupon, listCoupons, type Coupon } from '@/api/marketing'
import { COUPON_TYPE_MAP } from '@/constants/status'
import { formatMoney } from '@/utils/format'

const { run } = useAsyncTask()
const { filters, tableData, total, pageNum, pageSize, loading, error, search, reset, reload } = useListQuery<
  Coupon,
  { name?: string }
>({ fetcher: listCoupons, defaultPageSize: 10, syncKeys: ['name'] })

async function remove(id: number) {
  await run(() => deleteCoupon(id), '已删除')
  reload()
}
</script>
