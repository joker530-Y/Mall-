<template>
  <section class="page-container page-stack" v-loading="loading">
    <el-alert v-if="error" type="error" :title="error" show-icon />

    <section v-if="detail" class="panel detail-layout">
      <el-image :src="detail.product.pic" fit="cover" class="detail-image" loading="lazy" />
      <div>
        <h1>{{ detail.product.name }}</h1>
        <PriceTag :price="flashItem?.flashPromotionPrice" :original="detail.product.price" />
        <p>限购：{{ flashItem?.flashPromotionLimit || 1 }} 件</p>

        <div class="sku-group" v-if="detail.skuStockList.length">
          <strong>规格</strong>
          <el-select v-model="skuId" style="width: 240px">
            <el-option
              v-for="sku in detail.skuStockList"
              :key="sku.id"
              :label="formatSkuLabel(sku)"
              :value="sku.id"
            />
          </el-select>
        </div>

        <div class="buy-row">
          <span>数量</span>
          <el-input-number v-model="quantity" :min="1" :max="flashItem?.flashPromotionLimit || 1" />
        </div>

        <section class="panel">
          <h3>收货地址</h3>
          <el-select v-model="addressId" style="width: 100%">
            <el-option
              v-for="addr in addresses"
              :key="addr.id"
              :label="`${addr.name} ${addr.phoneNumber} · ${addr.province}${addr.city}${addr.detailAddress}`"
              :value="addr.id"
            />
          </el-select>
        </section>

        <el-button type="danger" :loading="submitting" :disabled="!canSubmit" @click="submit">提交秒杀</el-button>

        <el-alert
          v-if="result"
          :title="resultMessage"
          :type="result.status === 'SUCCESS' ? 'success' : result.status === 'PROCESSING' ? 'info' : 'warning'"
          show-icon
        />
        <div v-if="result?.orderId" class="actions">
          <el-button type="primary" @click="$router.push(`/payment/${result.orderId}`)">去支付</el-button>
          <el-button @click="$router.push(`/orders/${result.orderId}`)">查看订单</el-button>
        </div>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import PriceTag from '@/components/PriceTag.vue'
import { getHomeContent, type FlashPromotionProduct } from '@/api/modules/home'
import { getProductDetail, type ProductDetail } from '@/api/modules/product'
import { formatSkuLabel } from '@/utils/sku'
import { listAddresses, type MemberAddress } from '@/api/modules/member'
import { getSeckillResult, submitSeckillOrder, type SeckillOrderResult } from '@/api/modules/seckill'

const FINAL_STATUSES = new Set(['SUCCESS', 'FAILED', 'SOLD_OUT', 'REPEAT', 'NOT_WARMED'])
const STATUS_HINTS: Record<string, string> = {
  PROCESSING: '订单处理中，请稍候',
  SUCCESS: '抢购成功',
  FAILED: '抢购失败',
  SOLD_OUT: '库存不足',
  REPEAT: '请勿重复下单',
  NOT_WARMED: '活动尚未预热，请联系管理员在后台执行预热'
}

const route = useRoute()
const relationId = computed(() => Number(route.params.relationId))
const productId = computed(() => Number(route.query.productId))
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const detail = ref<ProductDetail>()
const flashItem = ref<FlashPromotionProduct>()
const addresses = ref<MemberAddress[]>([])
const addressId = ref<number>()
const skuId = ref<number>()
const quantity = ref(1)
const result = ref<SeckillOrderResult>()
let timer: ReturnType<typeof setInterval> | null = null
let pollAttempts = 0

const canSubmit = computed(
  () =>
    Boolean(flashItem.value) &&
    Boolean(addressId.value) &&
    Boolean(skuId.value) &&
    Number.isFinite(productId.value) &&
    productId.value > 0
)

const resultMessage = computed(() => {
  if (!result.value) return ''
  const hint = STATUS_HINTS[result.value.status] || result.value.status
  return result.value.reason ? `${hint}：${result.value.reason}` : hint
})

function stopPolling() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

async function pollResult() {
  stopPolling()
  pollAttempts = 0
  const pollOnce = async () => {
    try {
      result.value = await getSeckillResult(relationId.value)
      pollAttempts += 1
      if (result.value && FINAL_STATUSES.has(result.value.status)) {
        stopPolling()
      } else if (pollAttempts >= 30) {
        stopPolling()
        error.value = '抢购结果查询超时，请稍后在订单列表查看'
      }
    } catch (err) {
      stopPolling()
      error.value = err instanceof Error ? err.message : '抢购结果查询失败'
    }
  }
  await pollOnce()
  if (!error.value && (!result.value || !FINAL_STATUSES.has(result.value.status))) {
    timer = setInterval(() => {
      void pollOnce()
    }, 2000)
  }
}

async function load() {
  if (!Number.isFinite(relationId.value) || relationId.value <= 0) {
    error.value = '无效的秒杀活动'
    return
  }
  if (!Number.isFinite(productId.value) || productId.value <= 0) {
    error.value = '缺少商品信息，请从秒杀列表重新进入'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const [content, product, addressList] = await Promise.all([
      getHomeContent(),
      getProductDetail(productId.value),
      listAddresses()
    ])
    detail.value = product
    flashItem.value = content.homeFlashPromotion?.productList?.find((item) => item.relationId === relationId.value)
    if (!flashItem.value) {
      error.value = '当前秒杀商品不存在或活动已结束'
    }
    addresses.value = addressList
    addressId.value = addressList.find((item) => item.defaultStatus === 1)?.id || addressList[0]?.id
    skuId.value = product.skuStockList[0]?.id
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!canSubmit.value) return
  submitting.value = true
  error.value = ''
  try {
    await submitSeckillOrder({
      relationId: relationId.value,
      productSkuId: skuId.value!,
      memberReceiveAddressId: addressId.value!,
      quantity: quantity.value,
      payType: 1
    })
    await pollResult()
  } catch (err) {
    stopPolling()
    error.value = err instanceof Error ? err.message : '提交秒杀失败'
  } finally {
    submitting.value = false
  }
}

onMounted(load)
onUnmounted(stopPolling)
</script>

<style scoped>
.detail-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 24px;
}

.detail-image {
  width: 100%;
  aspect-ratio: 1;
}

.buy-row,
.actions {
  display: flex;
  gap: 12px;
  align-items: center;
  margin: 12px 0;
}

@media (max-width: 768px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
}
</style>
