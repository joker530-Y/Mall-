<template>
  <section class="page-container page-stack">
    <h1>我的订单</h1>
    <el-tabs v-model="status" @tab-change="reload">
      <el-tab-pane label="全部" :name="-1" />
      <el-tab-pane label="待付款" :name="0" />
      <el-tab-pane label="待发货" :name="1" />
      <el-tab-pane label="已发货" :name="2" />
      <el-tab-pane label="已完成" :name="3" />
      <el-tab-pane label="已关闭" :name="4" />
    </el-tabs>

    <el-alert v-if="error" type="error" :title="error" show-icon />

    <div v-loading="loading" class="order-list">
      <section v-for="order in tableData" :key="order.id" class="panel order-card">
        <div class="order-head">
          <span>{{ order.orderSn }}</span>
          <OrderStatusTag :status="order.status ?? 0" />
        </div>
        <div class="order-body">
          <div v-for="item in order.orderItemList || []" :key="item.id" class="order-item">
            <span>{{ item.productName }}</span>
            <span>x{{ item.productQuantity }}</span>
            <span>¥{{ formatMoney(item.productPrice) }}</span>
          </div>
        </div>
        <div class="order-foot">
          <strong>应付 ¥{{ formatMoney(order.payAmount) }}</strong>
          <div class="actions">
            <el-button link type="primary" @click="$router.push(`/orders/${order.id}`)">详情</el-button>
            <el-button v-if="order.status === 0" link @click="$router.push(`/payment/${order.id}`)">去支付</el-button>
          </div>
        </div>
      </section>
      <EmptyState v-if="!loading && !tableData.length" description="暂无订单" />
    </div>

    <el-pagination
      v-if="total > 0"
      layout="prev, pager, next"
      :total="total"
      v-model:current-page="pageNum"
      :page-size="pageSize"
      @current-change="reload"
    />
  </section>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import OrderStatusTag from '@/components/OrderStatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { useListQuery } from '@/composables/useListQuery'
import { listOrders, type OrderDetail } from '@/api/modules/order'
import { formatMoney } from '@/utils/format'

const status = ref(-1)
const { tableData, total, pageNum, pageSize, loading, error, reload } = useListQuery<OrderDetail, Record<string, never>>({
  fetcher: ({ pageNum, pageSize }) => listOrders(status.value, pageNum, pageSize),
  defaultPageSize: 5
})

watch(status, () => {
  pageNum.value = 1
  reload()
})
</script>

<style scoped>
.order-list {
  display: grid;
  gap: 12px;
}

.order-card {
  display: grid;
  gap: 12px;
}

.order-head,
.order-foot,
.order-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.actions {
  display: flex;
  gap: 8px;
}
</style>
