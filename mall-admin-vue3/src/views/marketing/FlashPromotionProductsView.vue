<template>
  <section class="page-stack">
    <div class="toolbar">
      <span>场次</span>
      <el-select v-model="sessionId" style="width: 220px" @change="reload">
        <el-option
          v-for="session in sessions"
          :key="session.id"
          :label="`${session.name} (#${session.id})`"
          :value="session.id"
        />
      </el-select>
    </div>

    <BaseTable
      :data="tableData"
      :loading="loading"
      :error="error"
      :total="total"
      :page-num="pageNum"
      :page-size="pageSize"
      show-pagination
      empty-text="当前场次暂无商品"
      height="520"
      @retry="reload"
      @page-change="(page) => { pageNum = page; reload() }"
      @page-size-change="(size) => { pageSize = size; reload() }"
    >
      <el-table-column prop="id" label="关系ID" width="90" />
      <el-table-column prop="productName" label="商品" min-width="180" show-overflow-tooltip />
      <el-table-column prop="flashPromotionPrice" label="秒杀价" width="100" />
      <el-table-column prop="flashPromotionCount" label="库存" width="90" />
      <el-table-column prop="flashPromotionLimit" label="限购" width="80" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="goWarmup(row.id)">预热</el-button>
          <el-button size="small" @click="goMonitor(row.id)">监控</el-button>
        </template>
      </el-table-column>
    </BaseTable>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseTable from '@/components/base/BaseTable.vue'
import {
  listFlashProducts,
  listFlashSessions,
  type FlashPromotionProduct,
  type FlashPromotionSession
} from '@/api/seckill'

const route = useRoute()
const router = useRouter()
const promotionId = computed(() => Number(route.params.id))
const sessions = ref<FlashPromotionSession[]>([])
const sessionId = ref<number>()
const tableData = ref<FlashPromotionProduct[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const error = ref('')

async function loadSessions() {
  if (!promotionId.value) return
  sessions.value = await listFlashSessions(promotionId.value)
  sessionId.value = sessions.value[0]?.id
}

async function reload() {
  if (!promotionId.value || !sessionId.value) return
  loading.value = true
  error.value = ''
  try {
    const page = await listFlashProducts({
      flashPromotionId: promotionId.value,
      flashPromotionSessionId: sessionId.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    tableData.value = page.list
    total.value = page.total
  } catch (err) {
    tableData.value = []
    total.value = 0
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function goWarmup(relationId: number) {
  router.push({
    path: `/marketing/flash-promotions/${promotionId.value}/warmup`,
    query: { relationId: String(relationId) }
  })
}

function goMonitor(relationId: number) {
  router.push({
    path: `/marketing/flash-promotions/${promotionId.value}/monitor`,
    query: { relationId: String(relationId) }
  })
}

watch(promotionId, async () => {
  pageNum.value = 1
  await loadSessions()
  await reload()
}, { immediate: true })
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
