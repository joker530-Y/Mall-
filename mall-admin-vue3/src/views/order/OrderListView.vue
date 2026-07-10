<template>
  <section class="page-stack">
    <PageHeader title="订单列表" description="查询订单、发货与关闭" />

    <FilterBar :loading="loading" @search="search" @reset="reset">
      <el-input v-model="filters.orderSn" clearable placeholder="订单号" style="width: 180px" @keyup.enter="search" />
      <el-input v-model="filters.receiverKeyword" clearable placeholder="收货人/手机号" style="width: 180px" @keyup.enter="search" />
      <el-select v-model="filters.status" clearable placeholder="订单状态" style="width: 140px">
        <el-option v-for="(item, key) in ORDER_STATUS_MAP" :key="key" :label="item.label" :value="Number(key)" />
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
      <el-table-column prop="orderSn" label="订单号" min-width="180" />
      <el-table-column prop="memberUsername" label="会员" width="120" />
      <el-table-column label="金额" width="100">
        <template #default="{ row }">{{ formatMoney(row.payAmount) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <StatusTag :status="row.status ?? 0" :map="ORDER_STATUS_MAP" />
        </template>
      </el-table-column>
      <el-table-column label="下单时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/order/${row.id}`)">详情</el-button>
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
import { listOrders, type Order } from '@/api/order'
import { ORDER_STATUS_MAP } from '@/constants/status'
import { formatDateTime, formatMoney } from '@/utils/format'

const { filters, tableData, total, pageNum, pageSize, loading, error, search, reset, reload } = useListQuery<
  Order,
  { orderSn?: string; receiverKeyword?: string; status?: number }
>({
  fetcher: listOrders,
  defaultPageSize: 10,
  syncKeys: ['orderSn', 'receiverKeyword', 'status']
})
</script>
