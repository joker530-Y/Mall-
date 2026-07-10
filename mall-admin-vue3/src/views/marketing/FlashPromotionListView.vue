<template>
  <section class="page-stack">
    <PageHeader title="秒杀活动" description="选择活动后可进入详情配置场次商品、预热与监控" />

    <FilterBar :loading="loading" @search="search" @reset="reset">
      <el-input
        v-model="filters.keyword"
        clearable
        placeholder="活动名称"
        style="width: 240px"
        @keyup.enter="search"
        @input="scheduleSearch"
      />
    </FilterBar>

    <BaseTable
      :data="tableData"
      :loading="loading"
      :error="error"
      :total="total"
      :page-num="pageNum"
      :page-size="pageSize"
      show-pagination
      empty-text="暂无秒杀活动"
      height="560"
      highlight-current-row
      @retry="reload"
      @page-change="(page) => { pageNum = page; search() }"
      @page-size-change="(size) => { pageSize = size; search() }"
    >
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="title" label="活动" min-width="180" />
      <el-table-column prop="status" label="状态" width="96">
        <template #default="{ row }">
          <StatusTag
            :status="row.status"
            :map="{
              1: { label: '上线', type: 'success' },
              0: { label: '下线', type: 'info' }
            }"
          />
        </template>
      </el-table-column>
      <el-table-column label="日期" min-width="200">
        <template #default="{ row }">{{ row.startDate }} 至 {{ row.endDate }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click.stop="openDetail(row)">管理</el-button>
        </template>
      </el-table-column>
    </BaseTable>
  </section>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import PageHeader from '@/components/base/PageHeader.vue'
import FilterBar from '@/components/base/FilterBar.vue'
import BaseTable from '@/components/base/BaseTable.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import { useListQuery } from '@/composables/useListQuery'
import { listFlashPromotions, type FlashPromotion } from '@/api/seckill'

const router = useRouter()

const {
  filters,
  tableData,
  total,
  pageNum,
  pageSize,
  loading,
  error,
  search,
  scheduleSearch,
  reset,
  reload
} = useListQuery<FlashPromotion, { keyword?: string }>({
  fetcher: listFlashPromotions,
  defaultPageSize: 10,
  syncKeys: ['keyword']
})

function openDetail(row: FlashPromotion) {
  router.push(`/marketing/flash-promotions/${row.id}/products`)
}
</script>
