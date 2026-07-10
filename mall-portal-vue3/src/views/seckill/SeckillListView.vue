<template>
  <section class="page-container page-stack">
    <h1>秒杀专场</h1>
    <el-alert v-if="error" type="error" :title="error" show-icon />

    <section v-if="promotion" class="panel">
      <p>本场时间：{{ promotion.startTime }} - {{ promotion.endTime }}</p>
      <div class="product-grid">
        <div v-for="item in promotion.productList || []" :key="item.relationId || item.id" class="flash-item">
          <ProductCard :product="item" />
          <div class="flash-meta">
            <PriceTag :price="item.flashPromotionPrice" :original="item.price" />
            <el-button
              type="danger"
              size="small"
              :disabled="!item.relationId"
              @click="goDetail(item)"
            >
              立即抢购
            </el-button>
          </div>
        </div>
      </div>
      <EmptyState v-if="!promotion.productList?.length" description="当前暂无秒杀商品" />
    </section>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import ProductCard from '@/components/ProductCard.vue'
import PriceTag from '@/components/PriceTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getHomeContent, type FlashPromotionProduct, type HomeFlashPromotion } from '@/api/modules/home'

const router = useRouter()
const promotion = ref<HomeFlashPromotion>()
const error = ref('')

async function load() {
  try {
    const content = await getHomeContent()
    promotion.value = content.homeFlashPromotion
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载秒杀活动失败'
  }
}

function goDetail(item: FlashPromotionProduct) {
  if (!item.relationId) return
  router.push({ path: `/seckill/${item.relationId}`, query: { productId: String(item.id) } })
}

onMounted(load)
</script>

<style scoped>
.flash-item {
  display: grid;
  gap: 8px;
}

.flash-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 4px 8px;
}
</style>
