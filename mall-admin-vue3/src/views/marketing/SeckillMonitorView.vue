<template>
  <section class="page-stack">
    <div class="toolbar">
      <span>Relation ID</span>
      <el-input-number v-model="relationId" :min="1" controls-position="right" />
      <el-button type="primary" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      <el-popconfirm title="确认预热该商品库存？" @confirm="warmup">
        <template #reference>
          <el-button :icon="Lightning" :loading="warming">预热库存</el-button>
        </template>
      </el-popconfirm>
      <el-switch v-model="autoRefresh" active-text="自动刷新" />
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
        <StatusTag
          v-if="summary"
          :status="summary.oversoldCount === 0 ? 'ok' : 'bad'"
          :map="{
            ok: { label: '零超卖', type: 'success' },
            bad: { label: '需排查', type: 'danger' }
          }"
        />
        <StatusTag v-else status="wait" :map="{ wait: { label: '等待数据', type: 'info' } }" />
      </div>
      <el-descriptions v-if="summary" :column="2" border>
        <el-descriptions-item label="Relation ID">{{ summary.relationId }}</el-descriptions-item>
        <el-descriptions-item label="Product ID">{{ summary.productId }}</el-descriptions-item>
        <el-descriptions-item label="Redis Key">{{ summary.stockKey }}</el-descriptions-item>
        <el-descriptions-item label="刷新时间">{{ formatTime(summary.refreshedAt) }}</el-descriptions-item>
        <el-descriptions-item label="重复成功用户">{{ summary.duplicateMemberCount }}</el-descriptions-item>
        <el-descriptions-item label="超卖数量">{{ summary.oversoldCount }}</el-descriptions-item>
      </el-descriptions>
      <EmptyState v-else description="输入 relationId 后刷新" />
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Lightning, Refresh } from '@element-plus/icons-vue'
import StatusTag from '@/components/base/StatusTag.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import { useAsyncTask } from '@/composables/useAsyncTask'
import { useRelationId } from '@/composables/useRelationId'
import { getSeckillSummary, warmupSeckill, type SeckillSummary } from '@/api/seckill'

const route = useRoute()
const relationId = useRelationId()
const autoRefresh = ref(false)
const summary = ref<SeckillSummary>()
const { loading, run } = useAsyncTask()
const { loading: warming, run: runWarmup } = useAsyncTask()
let timer: ReturnType<typeof setInterval> | null = null

watch(
  () => route.query.relationId,
  (value) => {
    const next = Number(value)
    if (Number.isFinite(next) && next > 0) relationId.value = next
  },
  { immediate: true }
)

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

async function load() {
  summary.value = await run(() => getSeckillSummary(relationId.value))
}

async function warmup() {
  await runWarmup(() => warmupSeckill(relationId.value), `已预热 relation ${relationId.value}`)
  await load()
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

function clearTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

watch(autoRefresh, (enabled) => {
  clearTimer()
  if (enabled) timer = setInterval(load, 8000)
})

onMounted(load)
onUnmounted(clearTimer)
</script>
