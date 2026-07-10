<template>
  <section class="page-stack">
    <PageHeader title="经营概览" description="聚合商品、订单、售后与秒杀活动关键指标" />

    <el-alert v-if="error" type="error" :title="error" show-icon>
      <template #default>
        <el-button size="small" @click="load">重试</el-button>
      </template>
    </el-alert>

    <el-row :gutter="16" v-loading="loading">
      <el-col :xs="24" :sm="12" :lg="6" v-for="metric in metrics" :key="metric.label">
        <div class="metric">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </div>
      </el-col>
    </el-row>

    <section class="panel">
      <div class="panel-header">
        <h3>待发货订单</h3>
        <el-button link type="primary" @click="$router.push('/order/list')">查看全部</el-button>
      </div>
      <el-table :data="overview?.recentOrders || []" empty-text="暂无待发货订单">
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
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/order/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/base/PageHeader.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import { fetchDashboardOverview, type DashboardOverview } from '@/api/dashboard'
import { ORDER_STATUS_MAP } from '@/constants/status'
import { formatMoney } from '@/utils/format'

const loading = ref(false)
const error = ref('')
const overview = ref<DashboardOverview>()

const metrics = computed(() => [
  { label: '商品总数', value: overview.value?.productTotal ?? '-' },
  { label: '在售商品', value: overview.value?.onSaleProducts ?? '-' },
  { label: '待发货订单', value: overview.value?.pendingOrders ?? '-' },
  { label: '待处理售后', value: overview.value?.pendingReturns ?? '-' },
  { label: '进行中秒杀', value: overview.value?.activeFlashPromotions ?? '-' }
])

async function load() {
  loading.value = true
  error.value = ''
  try {
    overview.value = await fetchDashboardOverview()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
