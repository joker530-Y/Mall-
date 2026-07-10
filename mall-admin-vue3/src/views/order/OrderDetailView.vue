<template>
  <section class="page-stack">
    <PageHeader :title="`订单 ${detail?.orderSn || ''}`" description="订单详情与运营操作" />

    <el-alert v-if="error" type="error" :title="error" show-icon />

    <section class="panel" v-loading="loading">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ detail.orderSn }}</el-descriptions-item>
          <el-descriptions-item label="会员">{{ detail.memberUsername }}</el-descriptions-item>
          <el-descriptions-item label="应付金额">{{ formatMoney(detail.payAmount) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :status="detail.status ?? 0" :map="ORDER_STATUS_MAP" />
          </el-descriptions-item>
          <el-descriptions-item label="收货人">{{ detail.receiverName }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ detail.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">
            {{ detail.receiverProvince }}{{ detail.receiverCity }}{{ detail.receiverRegion }}{{ detail.receiverDetailAddress }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="toolbar" style="margin-top: 16px">
          <el-button v-if="detail.status === 1" type="primary" @click="showDeliver = true">发货</el-button>
          <el-popconfirm v-if="detail.status === 0 || detail.status === 1" title="确认关闭订单？" @confirm="closeOrder">
            <template #reference>
              <el-button type="danger">关闭订单</el-button>
            </template>
          </el-popconfirm>
          <el-button @click="$router.back()">返回</el-button>
        </div>

        <h3 style="margin-top: 20px">商品明细</h3>
        <el-table :data="detail.orderItemList || []">
          <el-table-column prop="productName" label="商品" min-width="180" />
          <el-table-column label="单价" width="100">
            <template #default="{ row }">{{ formatMoney(row.productPrice) }}</template>
          </el-table-column>
          <el-table-column prop="productQuantity" label="数量" width="80" />
        </el-table>
      </template>
    </section>

    <el-dialog v-model="showDeliver" title="订单发货" width="480px">
      <el-form :model="deliverForm" label-width="90px">
        <el-form-item label="物流公司"><el-input v-model="deliverForm.deliveryCompany" /></el-form-item>
        <el-form-item label="物流单号"><el-input v-model="deliverForm.deliverySn" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDeliver = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="deliver">确认发货</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import PageHeader from '@/components/base/PageHeader.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import { closeOrders, deliverOrders, getOrderDetail, type OrderDetail } from '@/api/order'
import { useAsyncTask } from '@/composables/useAsyncTask'
import { ORDER_STATUS_MAP } from '@/constants/status'
import { formatDateTime, formatMoney } from '@/utils/format'

const route = useRoute()
const orderId = computed(() => Number(route.params.id))
const loading = ref(false)
const error = ref('')
const detail = ref<OrderDetail>()
const showDeliver = ref(false)
const deliverForm = reactive({ deliveryCompany: '', deliverySn: '' })
const { loading: saving, run } = useAsyncTask()

async function load() {
  loading.value = true
  error.value = ''
  try {
    detail.value = await getOrderDetail(orderId.value)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function deliver() {
  await run(
    () =>
      deliverOrders([
        { orderId: orderId.value, deliveryCompany: deliverForm.deliveryCompany, deliverySn: deliverForm.deliverySn }
      ]),
    '发货成功'
  )
  showDeliver.value = false
  load()
}

async function closeOrder() {
  await run(() => closeOrders([orderId.value], '运营关闭'), '订单已关闭')
  load()
}

onMounted(load)
</script>
