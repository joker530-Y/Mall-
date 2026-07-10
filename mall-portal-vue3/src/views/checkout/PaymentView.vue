<template>
  <section class="page-container page-stack" v-loading="loading">
    <el-alert v-if="error" type="error" :title="error" show-icon />

    <section class="panel">
      <h1>模拟支付</h1>
      <el-alert
        title="当前为演示环境模拟支付，金额以订单数据库记录为准，不会发起真实第三方支付。"
        type="warning"
        show-icon
        :closable="false"
      />
      <template v-if="order">
        <p>订单号：{{ order.orderSn }}</p>
        <p>应付金额：<strong>¥{{ formatMoney(order.payAmount) }}</strong></p>
        <p>订单状态：<OrderStatusTag :status="order.status ?? 0" /></p>
        <div class="actions">
          <el-button
            v-if="order.status === 0"
            type="primary"
            :loading="paying"
            @click="handlePay"
          >
            确认模拟支付
          </el-button>
          <el-button @click="$router.push(`/orders/${order.id}`)">查看订单</el-button>
        </div>
      </template>
    </section>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import OrderStatusTag from '@/components/OrderStatusTag.vue'
import { getOrderDetail, type OrderDetail } from '@/api/modules/order'
import { useCheckout } from '@/composables/useCheckout'
import { formatMoney } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const order = ref<OrderDetail>()
const loading = ref(false)
const error = ref('')
const { paying, payMock } = useCheckout()

async function load() {
  loading.value = true
  error.value = ''
  try {
    order.value = await getOrderDetail(Number(route.params.orderId))
  } catch (err) {
    order.value = undefined
    error.value = err instanceof Error ? err.message : '加载订单失败'
  } finally {
    loading.value = false
  }
}

async function handlePay() {
  if (!order.value) return
  await payMock(order.value.id, order.value.payType || 1)
  await load()
  router.push(`/orders/${order.value.id}`)
}

onMounted(load)
</script>

<style scoped>
.actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}
</style>
