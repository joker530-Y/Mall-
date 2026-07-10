<template>
  <section class="page-container page-stack" v-loading="loading">
    <h1>确认订单</h1>
    <el-alert v-if="error" type="error" :title="error" show-icon />

    <template v-if="confirm">
      <section class="panel">
        <h3>收货地址</h3>
        <el-radio-group v-model="addressId" class="address-list">
          <el-radio v-for="addr in confirm.memberReceiveAddressList" :key="addr.id" :value="addr.id" border>
            {{ addr.name }} {{ addr.phoneNumber }} · {{ addr.province }}{{ addr.city }}{{ addr.region }}{{ addr.detailAddress }}
          </el-radio>
        </el-radio-group>
        <el-button link type="primary" @click="$router.push('/account/addresses')">管理地址</el-button>
      </section>

      <section class="panel">
        <h3>商品清单</h3>
        <el-table :data="confirm.cartPromotionItemList">
          <el-table-column prop="productName" label="商品" min-width="180" />
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="小计" width="100">
            <template #default="{ row }">¥{{ formatMoney((row.price || 0) * (row.quantity || 0)) }}</template>
          </el-table-column>
        </el-table>
      </section>

      <section class="panel">
        <h3>优惠券</h3>
        <el-select v-model="couponId" clearable placeholder="可选优惠券" style="width: 280px">
          <el-option
            v-for="item in couponOptions"
            :key="item.id"
            :label="item.label"
            :value="item.couponId"
          />
        </el-select>
      </section>

      <section class="panel summary">
        <div>商品合计：¥{{ formatMoney(confirm.calcAmount?.totalAmount) }}</div>
        <div>运费：¥{{ formatMoney(confirm.calcAmount?.freightAmount) }}</div>
        <div>活动优惠：-¥{{ formatMoney(confirm.calcAmount?.promotionAmount) }}</div>
        <div v-if="confirm.calcAmount?.couponAmount">优惠券：-¥{{ formatMoney(confirm.calcAmount?.couponAmount) }}</div>
        <div v-if="confirm.calcAmount?.integrationAmount">积分抵扣：-¥{{ formatMoney(confirm.calcAmount?.integrationAmount) }}</div>
        <strong>应付金额：¥{{ formatMoney(confirm.calcAmount?.payAmount) }}</strong>
        <el-button type="primary" :loading="submitting" :disabled="!addressId" @click="submit">提交订单</el-button>
      </section>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCheckout } from '@/composables/useCheckout'
import { formatMoney } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const { confirm, loading, submitting, error, loadConfirm, createOrder } = useCheckout()
const addressId = ref<number>()
const couponId = ref<number>()

const cartIds = computed(() =>
  String(route.query.cartIds || '')
    .split(',')
    .map((id) => Number(id))
    .filter((id) => Number.isFinite(id) && id > 0)
)

const couponOptions = computed(() =>
  (confirm.value?.couponHistoryDetailList || [])
    .map((item) => ({
      id: item.id,
      couponId: item.coupon?.id,
      label: item.coupon?.name || `优惠券 #${item.id}`
    }))
    .filter((item): item is { id: number; couponId: number; label: string } => typeof item.couponId === 'number')
)

async function load() {
  if (!cartIds.value.length) {
    router.replace('/cart')
    return
  }
  await loadConfirm(cartIds.value, { couponId: couponId.value })
  addressId.value = confirm.value?.memberReceiveAddressList.find((item) => item.defaultStatus === 1)?.id
    || confirm.value?.memberReceiveAddressList[0]?.id
}

watch(couponId, async (id) => {
  if (!cartIds.value.length || loading.value) return
  await loadConfirm(cartIds.value, { couponId: id })
})

async function submit() {
  if (!addressId.value || !cartIds.value.length) return
  const result = await createOrder({
    memberReceiveAddressId: addressId.value,
    couponId: couponId.value,
    payType: 1,
    cartIds: cartIds.value
  })
  if (result?.order?.id) {
    router.push(`/payment/${result.order.id}`)
  }
}

onMounted(load)
</script>

<style scoped>
.address-list {
  display: grid;
  gap: 8px;
}

.summary {
  display: grid;
  gap: 8px;
  justify-items: start;
}
</style>
