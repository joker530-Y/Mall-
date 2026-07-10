<template>
  <section class="page-container page-stack" v-loading="loading">
    <el-alert v-if="error" type="error" :title="error" show-icon />

    <div v-if="detail" class="detail-layout panel">
      <el-image :src="detail.product.pic" fit="cover" class="detail-image" loading="lazy" />
      <div class="detail-info">
        <h1>{{ detail.product.name }}</h1>
        <p class="muted">{{ detail.product.subTitle }}</p>
        <PriceTag :price="selectedSku?.price ?? detail.product.promotionPrice ?? detail.product.price" />

        <div v-for="attr in attributeGroups" :key="attr.id" class="sku-group">
          <strong>{{ attr.name }}</strong>
          <div class="sku-options">
            <el-button
              v-for="value in attr.values"
              :key="value"
              size="small"
              :type="selectedAttrs[attr.id] === value ? 'primary' : 'default'"
              @click="selectAttr(attr.id, value)"
            >
              {{ value }}
            </el-button>
          </div>
        </div>

        <div class="buy-row">
          <span>数量</span>
          <el-input-number v-model="quantity" :min="1" :max="selectedSku?.stock || 99" />
        </div>

        <div class="actions">
          <el-button type="primary" :disabled="!selectedSku" @click="addToCart">加入购物车</el-button>
          <el-button type="danger" :disabled="!selectedSku || !auth.isAuthenticated" @click="buyNow">立即购买</el-button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PriceTag from '@/components/PriceTag.vue'
import { getProductDetail, type ProductDetail, type SkuStock } from '@/api/modules/product'
import { useCart } from '@/composables/useCart'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { add } = useCart()
const loading = ref(false)
const error = ref('')
const detail = ref<ProductDetail>()
const quantity = ref(1)
const selectedAttrs = reactive<Record<number, string>>({})

const attributeGroups = computed(() => {
  if (!detail.value) return []
  return detail.value.productAttributeList.map((attr) => ({
    id: attr.id,
    name: attr.name,
    values: detail.value!.productAttributeValueList
      .filter((item) => item.productAttributeId === attr.id)
      .map((item) => item.value)
  }))
})

const selectedSku = computed(() => {
  if (!detail.value) return undefined
  const entries = Object.entries(selectedAttrs)
  if (!entries.length) return detail.value.skuStockList[0]
  return detail.value.skuStockList.find((sku) =>
    entries.every(([attrId, value]) => {
      const index = detail.value!.productAttributeList.findIndex((item) => item.id === Number(attrId))
      if (index === 0) return sku.sp1 === value
      if (index === 1) return sku.sp2 === value
      if (index === 2) return sku.sp3 === value
      return true
    })
  )
})

function selectAttr(attrId: number, value: string) {
  selectedAttrs[attrId] = value
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    detail.value = await getProductDetail(Number(route.params.id))
    detail.value.productAttributeList.forEach((attr) => {
      const first = detail.value!.productAttributeValueList.find((item) => item.productAttributeId === attr.id)
      if (first) selectedAttrs[attr.id] = first.value
    })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载商品失败'
  } finally {
    loading.value = false
  }
}

async function addToCart() {
  if (!detail.value || !selectedSku.value) return
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
    productPic: detail.value.product.pic,
    productAttr: [selectedSku.value.sp1, selectedSku.value.sp2, selectedSku.value.sp3].filter(Boolean).join(';')
  })
}

async function buyNow() {
  await addToCart()
  router.push('/cart')
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
