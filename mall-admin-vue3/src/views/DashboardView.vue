<template>
  <section class="page-stack">
    <div class="toolbar">
      <el-input-number v-model="relationId" :min="1" controls-position="right" />
      <el-button type="primary" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      <el-button :icon="Lightning" :loading="warming" @click="warmup">预热库存</el-button>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="12" :lg="6" v-for="metric in metrics" :key="metric.label">
        <div class="metric">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </div>
      </el-col>
    </el-row>

    <section class="panel">
      <div class="panel-header">
        <h3>一致性快照</h3>
        <el-tag :type="summaryStatus.type" effect="plain">
          {{ summaryStatus.label }}
        </el-tag>
      </div>
      <el-descriptions v-if="summary" :column="2" border>
        <el-descriptions-item label="Relation ID">{{ summary.relationId }}</el-descriptions-item>
        <el-descriptions-item label="Product ID">{{ summary.productId }}</el-descriptions-item>
        <el-descriptions-item label="Redis Key">{{ summary.stockKey }}</el-descriptions-item>
        <el-descriptions-item label="刷新时间">{{ formatTime(summary.refreshedAt) }}</el-descriptions-item>
        <el-descriptions-item label="重复成功用户">{{ summary.duplicateMemberCount }}</el-descriptions-item>
        <el-descriptions-item label="超卖数量">{{ summary.oversoldCount }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="输入 relationId 后刷新" />
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Lightning, Refresh } from '@element-plus/icons-vue'
import { getSeckillSummary, warmupSeckill, type SeckillSummary } from '@/api/seckill'
import { useRelationId } from '@/composables/useRelationId'

const relationId = useRelationId()
const loading = ref(false)
const warming = ref(false)
const summary = ref<SeckillSummary>()

const metrics = computed(() => [
  { label: 'Redis 库存', value: summary.value?.redisStock ?? '未预热' },
  { label: 'DB 剩余库存', value: summary.value?.dbRemainingStock ?? '-' },
  { label: '成功订单', value: summary.value?.successCount ?? '-' },
  { label: '失败请求', value: summary.value?.failedCount ?? '-' },
  { label: '处理中', value: summary.value?.processingCount ?? '-' },
  { label: '总请求', value: summary.value?.totalRequestCount ?? '-' },
  { label: '限购', value: summary.value?.limit ?? '-' },
  { label: '超卖', value: summary.value?.oversoldCount ?? '-' }
])

const summaryStatus = computed(() => {
  if (!summary.value) {
    return { type: 'info' as const, label: '等待数据' }
  }
  return summary.value.oversoldCount === 0
    ? { type: 'success' as const, label: '零超卖' }
    : { type: 'danger' as const, label: '需排查' }
})

async function load() {
  loading.value = true
  try {
    summary.value = await getSeckillSummary(relationId.value)
  } finally {
    loading.value = false
  }
}

async function warmup() {
  warming.value = true
  try {
    const result = await warmupSeckill(relationId.value)
    ElMessage.success(`已预热 relation ${result.relationId}，库存 ${result.stock}`)
    await load()
  } finally {
    warming.value = false
  }
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

onMounted(load)
</script>
