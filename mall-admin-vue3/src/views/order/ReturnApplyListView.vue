<template>
  <section class="page-stack">
    <PageHeader title="退货申请" description="处理退货申请与审核" />

    <FilterBar :loading="loading" @search="search" @reset="reset">
      <el-select v-model="filters.status" clearable placeholder="状态" style="width: 140px">
        <el-option v-for="(item, key) in RETURN_APPLY_STATUS_MAP" :key="key" :label="item.label" :value="Number(key)" />
      </el-select>
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
      <el-table-column prop="orderSn" label="订单号" min-width="160" />
      <el-table-column prop="memberUsername" label="会员" width="120" />
      <el-table-column prop="productName" label="商品" min-width="160" show-overflow-tooltip />
      <el-table-column label="退款金额" width="100">
        <template #default="{ row }">{{ formatMoney(row.returnAmount) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <StatusTag :status="row.status ?? 0" :map="RETURN_APPLY_STATUS_MAP" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" link type="primary" @click="approve(row.id, 1)">同意</el-button>
          <el-button v-if="row.status === 0" link type="danger" @click="approve(row.id, 3)">拒绝</el-button>
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
import { listReturnApplies, updateReturnApplyStatus, type ReturnApply } from '@/api/order'
import { RETURN_APPLY_STATUS_MAP } from '@/constants/status'
import { formatMoney } from '@/utils/format'

const { run } = useAsyncTask()
const { filters, tableData, total, pageNum, pageSize, loading, error, search, reset, reload } = useListQuery<
  ReturnApply,
  { status?: number }
>({ fetcher: listReturnApplies, defaultPageSize: 10, syncKeys: ['status'] })

async function approve(id: number, status: number) {
  await run(() => updateReturnApplyStatus(id, { status, handleNote: status === 1 ? '同意退货' : '拒绝退货' }), '已处理')
  reload()
}
</script>
