<template>
  <section class="page-stack">
    <div class="toolbar">
      <el-input-number v-model="relationId" :min="1" controls-position="right" />
      <el-button type="primary" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </div>
    <section class="panel">
      <div class="panel-header">
        <h3>订单处理日志</h3>
        <el-tag effect="plain">Relation {{ relationId }}</el-tag>
      </div>
      <el-table :data="logs" v-loading="loading" height="560">
        <el-table-column prop="id" label="日志ID" width="92" />
        <el-table-column prop="requestId" label="请求ID" min-width="220" show-overflow-tooltip />
        <el-table-column prop="memberId" label="会员" width="100" />
        <el-table-column prop="orderSn" label="订单号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="statusText" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="tagType(row.statusText)" effect="plain">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="failReason" label="失败原因" min-width="160" show-overflow-tooltip />
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        layout="total, sizes, prev, pager, next"
        :total="total"
        v-model:page-size="query.pageSize"
        v-model:current-page="query.pageNum"
        @size-change="load"
        @current-change="load"
      />
    </section>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { listSeckillOrderLogs, type SeckillOrderLog } from '@/api/seckill'
import { useRelationId } from '@/composables/useRelationId'

const relationId = useRelationId()
const loading = ref(false)
const logs = ref<SeckillOrderLog[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10 })

async function load() {
  loading.value = true
  try {
    const page = await listSeckillOrderLogs({ relationId: relationId.value, ...query })
    logs.value = page.list
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function tagType(status: string) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

watch(relationId, () => {
  query.pageNum = 1
  load()
})

onMounted(load)
</script>
