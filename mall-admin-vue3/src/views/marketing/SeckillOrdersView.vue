<template>
  <section class="page-stack">
    <FilterBar :loading="loading" @search="search" @reset="reset">
      <span>Relation ID</span>
      <el-input-number v-model="relationId" :min="1" controls-position="right" />
    </FilterBar>

    <BaseTable
      :data="tableData"
      :loading="loading"
      :error="error"
      :total="total"
      :page-num="pageNum"
      :page-size="pageSize"
      show-pagination
      empty-text="暂无订单日志，可先执行预热并提交秒杀请求"
      height="560"
      @retry="reload"
      @page-change="(page) => { pageNum = page; search() }"
      @page-size-change="(size) => { pageSize = size; search() }"
    >
      <el-table-column prop="id" label="日志ID" width="92" />
      <el-table-column prop="requestId" label="请求ID" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <el-button link type="primary" @click="copyRequestId(row.requestId)">{{ row.requestId }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="memberId" label="会员" width="100" />
      <el-table-column prop="orderSn" label="订单号" min-width="180" show-overflow-tooltip />
      <el-table-column prop="statusText" label="状态" width="120">
        <template #default="{ row }">
          <StatusTag
            :status="row.statusText"
            :map="{
              SUCCESS: { label: 'SUCCESS', type: 'success' },
              FAILED: { label: 'FAILED', type: 'danger' }
            }"
          />
        </template>
      </el-table-column>
      <el-table-column prop="failReason" label="失败原因" min-width="160" show-overflow-tooltip />
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
    </BaseTable>
  </section>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import FilterBar from '@/components/base/FilterBar.vue'
import BaseTable from '@/components/base/BaseTable.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import { useListQuery } from '@/composables/useListQuery'
import { listSeckillOrderLogs, type SeckillOrderLog } from '@/api/seckill'

const route = useRoute()
const relationId = ref(Number(route.query.relationId) || 1)

const {
  filters,
  tableData,
  total,
  pageNum,
  pageSize,
  loading,
  error,
  search,
  reset,
  reload
} = useListQuery<SeckillOrderLog, { relationId: number }>({
  fetcher: (query) => listSeckillOrderLogs(query),
  defaultQuery: { relationId: relationId.value },
  defaultPageSize: 10
})

watch(relationId, (value) => {
  filters.value.relationId = value
  search()
})

watch(
  () => route.query.relationId,
  (value) => {
    const next = Number(value)
    if (Number.isFinite(next) && next > 0) relationId.value = next
  },
  { immediate: true }
)

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

async function copyRequestId(value: string) {
  await navigator.clipboard.writeText(value)
  ElMessage.success('已复制 requestId')
}
</script>
