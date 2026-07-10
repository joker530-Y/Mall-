<template>
  <section class="page-container page-stack" v-loading="loading">
    <el-alert v-if="error" type="error" :title="error" show-icon />

    <div v-if="detail" class="detail-layout panel">
      <el-image
        :src="selectedSku?.pic || detail.product.pic"
        fit="cover"
        class="detail-image"
        loading="lazy"
      />
      <div class="detail-info">
        <h1>{{ detail.product.name }}</h1>
        <p class="muted">{{ detail.product.subTitle }}</p>
        <PriceTag :price="selectedSku?.price ?? detail.product.promotionPrice ?? detail.product.price" />

        <div v-for="attr in attributeGroups" :key="attr.name" class="sku-group">
          <strong>{{ attr.name }}</strong>
          <div class="sku-options">
            <el-button
              v-for="value in attr.values"
              :key="value"
              size="small"
              :type="selectedAttrs[attr.name] === value ? 'primary' : 'default'"
              @click="selectAttr(attr.name, value)"
            >
              {{ value }}
            </el-button>
          </div>
        </div>

        <div v-if="!attributeGroups.length && detail.skuStockList.length > 1" class="sku-group">
          <strong>规格</strong>
          <el-select v-model="manualSkuId" style="width: 280px">
            <el-option
              v-for="sku in detail.skuStockList"
              :key="sku.id"
              :label="`${formatSkuLabel(sku)} · ¥${sku.price}`"
              :value="sku.id"
              :disabled="(sku.stock ?? 0) <= 0"
            />
          </el-select>
        </div>

        <div class="buy-row">
          <span>数量</span>
          <el-input-number v-model="quantity" :min="1" :max="Math.max(selectedSku?.stock || 1, 1)" />
        </div>

        <div class="actions">
          <el-button type="primary" :disabled="!canBuy" @click="addToCart">加入购物车</el-button>
          <el-button type="danger" :disabled="!canBuy || !auth.isAuthenticated" @click="buyNow">
            立即购买
          </el-button>
        </div>
        <p v-if="detail.skuStockList.length && !canBuy" class="muted">当前规格暂无库存，请更换规格</p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PriceTag from '@/components/PriceTag.vue'
import { getProductDetail, type ProductDetail } from '@/api/modules/product'
import { useCart } from '@/composables/useCart'
import { useAuthStore } from '@/stores/auth'
import {
  buildSkuAttributeGroups,
  findSkuByAttrs,
  formatSkuAttrText,
  formatSkuLabel,
  parseSkuSpecs
} from '@/utils/sku'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { add } = useCart()
const loading = ref(false)
const error = ref('')
const detail = ref<ProductDetail>()
const quantity = ref(1)
const selectedAttrs = reactive<Record<string, string>>({})
const manualSkuId = ref<number>()

const attributeGroups = computed(() =>
  detail.value ? buildSkuAttributeGroups(detail.value.skuStockList) : []
)

const selectedSku = computed(() => {
  if (!detail.value?.skuStockList.length) return undefined
  if (attributeGroups.value.length) {
    return findSkuByAttrs(detail.value.skuStockList, selectedAttrs)
  }
  return (
    detail.value.skuStockList.find((sku) => sku.id === manualSkuId.value) ||
    detail.value.skuStockList.find((sku) => (sku.stock ?? 0) > 0) ||
    detail.value.skuStockList[0]
  )
})

const canBuy = computed(() => Boolean(selectedSku.value && (selectedSku.value.stock ?? 0) > 0))

function selectAttr(name: string, value: string) {
  selectedAttrs[name] = value
}

function applySkuSelection(skuId?: number) {
  if (!detail.value?.skuStockList.length) return
  const sku =
    detail.value.skuStockList.find((item) => item.id === skuId) ||
    detail.value.skuStockList.find((item) => (item.stock ?? 0) > 0) ||
    detail.value.skuStockList[0]
  manualSkuId.value = sku.id
  Object.keys(selectedAttrs).forEach((key) => delete selectedAttrs[key])
  parseSkuSpecs(sku).forEach((spec) => {
    selectedAttrs[spec.key] = spec.value
  })
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    detail.value = await getProductDetail(Number(route.params.id))
    applySkuSelection()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载商品失败'
  } finally {
    loading.value = false
  }
}

async function addToCart() {
  if (!detail.value || !selectedSku.value || !canBuy.value) return
  if (!auth.isAuthenticated) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  await add({
    productId: detail.value.product.id,
    productSkuId: selectedSku.value.id,
    quantity: quantity.value,
    price: selectedSku.value.price,
    productName: detail.value.product.name,
    productPic: selectedSku.value.pic || detail.value.product.pic,
    productAttr: formatSkuAttrText(selectedSku.value)
  })
}

async function buyNow() {
  await addToCart()
  if (auth.isAuthenticated) router.push('/cart')
}

onMounted(load)
</script>

<style scoped>
.detail-layout {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 24px;
}

.detail-image {
  width: 100%;
  aspect-ratio: 1;
  border-radius: var(--radius-card);
}

.detail-info {
  display: grid;
  gap: 16px;
}

.detail-info h1 {
  margin: 0;
  font-size: 24px;
}

.muted {
  margin: 0;
  color: var(--color-muted);
}

.sku-group {
  display: grid;
  gap: 8px;
}

.sku-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.buy-row,
.actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

@media (max-width: 768px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
}
</style>
