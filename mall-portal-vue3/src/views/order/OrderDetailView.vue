<template>
  <section class="page-container page-stack" v-loading="loading">
    <el-alert v-if="error" type="error" :title="error" show-icon />

    <section v-if="order" class="panel">
      <div class="head">
        <h1>订单 {{ order.orderSn }}</h1>
        <OrderStatusTag :status="order.status ?? 0" />
      </div>
      <p>应付金额：<strong>¥{{ formatMoney(order.payAmount) }}</strong></p>
      <p>收货人：{{ order.receiverName }} {{ order.receiverPhone }}</p>
      <p>地址：{{ order.receiverProvince }}{{ order.receiverCity }}{{ order.receiverRegion }}{{ order.receiverDetailAddress }}</p>

      <h3>商品明细</h3>
      <el-table :data="order.orderItemList || []">
        <el-table-column prop="productName" label="商品" min-width="180" />
        <el-table-column prop="productQuantity" label="数量" width="80" />
        <el-table-column label="单价" width="100">
          <template #default="{ row }">¥{{ formatMoney(row.productPrice) }}</template>
        </el-table-column>
      </el-table>

      <div class="actions">
        <el-button v-if="order.status === 0" type="primary" @click="$router.push(`/payment/${order.id}`)">模拟支付</el-button>
        <el-popconfirm v-if="order.status === 0" title="确认取消订单？" @confirm="cancel">
          <template #reference><el-button>取消订单</el-button></template>
        </el-popconfirm>
        <el-popconfirm v-if="order.status === 2" title="确认收货？" @confirm="confirm">
          <template #reference><el-button type="success">确认收货</el-button></template>
        </el-popconfirm>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import OrderStatusTag from '@/components/OrderStatusTag.vue'
import { cancelUserOrder, confirmReceiveOrder, getOrderDetail, type OrderDetail } from '@/api/modules/order'
import { formatMoney } from '@/utils/format'

const route = useRoute()
const order = ref<OrderDetail>()
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    order.value = await getOrderDetail(Number(route.params.id))
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function cancel() {
  await cancelUserOrder(Number(route.params.id))
  ElMessage.success('订单已取消')
  load()
}

async function confirm() {
  await confirmReceiveOrder(Number(route.params.id))
  ElMessage.success('已确认收货')
  load()
}

onMounted(load)
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}
</style>
