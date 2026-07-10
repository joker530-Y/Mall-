<template>
  <section class="page-stack">
    <div class="toolbar">
      <span>Relation ID</span>
      <el-input-number v-model="relationId" :min="1" controls-position="right" />
      <el-popconfirm title="确认预热该商品库存？此操作会影响 Redis 秒杀库存。" @confirm="handleWarmup">
        <template #reference>
          <el-button type="danger" :icon="Lightning" :loading="warming">预热库存</el-button>
        </template>
      </el-popconfirm>
      <el-button :icon="Refresh" :loading="loading" @click="loadSummary">刷新快照</el-button>
    </div>

    <el-alert v-if="warmupResult" type="success" :title="`预热成功：库存 ${warmupResult.stock}，Key ${warmupResult.stockKey}`" show-icon />

    <section class="panel" v-loading="loading">
      <div class="panel-header">
        <h3>库存快照</h3>
        <StatusTag
          v-if="summary"
          :status="summary.oversoldCount === 0 ? 'ok' : 'bad'"
          :map="{
            ok: { label: '零超卖', type: 'success' },
            bad: { label: '需排查', type: 'danger' }
          }"
        />
      </div>
      <el-descriptions v-if="summary" :column="2" border>
        <el-descriptions-item label="Relation ID">{{ summary.relationId }}</el-descriptions-item>
        <el-descriptions-item label="Product ID">{{ summary.productId }}</el-descriptions-item>
        <el-descriptions-item label="Redis 库存">{{ summary.redisStock ?? '未预热' }}</el-descriptions-item>
        <el-descriptions-item label="DB 剩余库存">{{ summary.dbRemainingStock }}</el-descriptions-item>
        <el-descriptions-item label="Redis Key">{{ summary.stockKey }}</el-descriptions-item>
        <el-descriptions-item label="最后刷新">{{ formatTime(summary.refreshedAt) }}</el-descriptions-item>
      </el-descriptions>
      <EmptyState v-else description="输入 relationId 后刷新或执行预热" />
    </section>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Lightning, Refresh } from '@element-plus/icons-vue'
import StatusTag from '@/components/base/StatusTag.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import { useAsyncTask } from '@/composables/useAsyncTask'
import { getSeckillSummary, warmupSeckill, type SeckillSummary, type WarmupResult } from '@/api/seckill'

const route = useRoute()
const relationId = ref(Number(route.query.relationId) || 1)
const summary = ref<SeckillSummary>()
const warmupResult = ref<WarmupResult>()
const { loading, run } = useAsyncTask()
const { loading: warming, run: runWarmup } = useAsyncTask()

async function loadSummary() {
  summary.value = await run(() => getSeckillSummary(relationId.value))
}

async function handleWarmup() {
  warmupResult.value = await runWarmup(
    () => warmupSeckill(relationId.value),
    `已预热 relation ${relationId.value}`
  )
  await loadSummary()
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

watch(
  () => route.query.relationId,
  (value) => {
    const next = Number(value)
    if (Number.isFinite(next) && next > 0) {
      relationId.value = next
      loadSummary()
    }
  }
)

onMounted(loadSummary)
</script>
