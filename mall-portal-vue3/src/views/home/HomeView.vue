<template>
  <section class="page-container page-stack">
    <el-skeleton v-if="loading" :rows="8" animated />
    <el-alert v-else-if="error" type="error" :title="error" show-icon>
      <template #default><el-button size="small" @click="load">重试</el-button></template>
    </el-alert>
    <template v-else-if="content">
      <section v-if="content.advertiseList?.length" class="panel hero">
        <el-carousel height="220px" indicator-position="outside">
          <el-carousel-item v-for="ad in content.advertiseList" :key="ad.id">
            <el-image :src="ad.pic" fit="cover" style="width:100%;height:220px" loading="lazy" />
          </el-carousel-item>
        </el-carousel>
      </section>

      <section v-if="categories.length" class="panel">
        <div class="section-header"><h2>商品分类</h2></div>
        <div class="category-list">
          <el-button
            v-for="cat in categories"
            :key="cat.id"
            size="small"
            @click="goCategory(cat.id)"
          >
            {{ cat.name }}
          </el-button>
        </div>
      </section>

      <section v-if="content.brandList?.length" class="panel">
        <div class="section-header"><h2>推荐品牌</h2></div>
        <div class="brand-list">
          <RouterLink
            v-for="brand in content.brandList"
            :key="brand.id"
            :to="{ path: '/search', query: { brandId: String(brand.id) } }"
            class="brand-item"
          >
            <el-image v-if="brand.logo" :src="brand.logo" fit="contain" class="brand-logo" loading="lazy" />
            <span>{{ brand.name }}</span>
          </RouterLink>
        </div>
      </section>

      <section v-if="flashProducts.length" class="panel">
        <div class="section-header">
          <h2>限时秒杀</h2>
          <RouterLink to="/seckill">查看更多</RouterLink>
        </div>
        <div class="product-grid">
          <div v-for="item in flashProducts" :key="item.relationId || item.id" class="flash-item">
            <ProductCard :product="item" />
            <div class="flash-meta">
              <PriceTag :price="item.flashPromotionPrice" :original="item.price" />
              <el-button type="danger" size="small" @click="goSeckill(item)">去抢购</el-button>
            </div>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="section-header"><h2>人气推荐</h2></div>
        <div class="product-grid">
          <ProductCard v-for="item in content.hotProductList || []" :key="item.id" :product="item" />
        </div>
      </section>

      <section class="panel">
        <div class="section-header"><h2>新品推荐</h2></div>
        <div class="product-grid">
          <ProductCard v-for="item in content.newProductList || []" :key="item.id" :product="item" />
        </div>
      </section>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import ProductCard from '@/components/ProductCard.vue'
import PriceTag from '@/components/PriceTag.vue'
import { getHomeContent, type FlashPromotionProduct, type HomeContent } from '@/api/modules/home'
import { getCategoryTree, type ProductCategoryNode } from '@/api/modules/product'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const content = ref<HomeContent>()
const categories = ref<ProductCategoryNode[]>([])

const flashProducts = computed(() => content.value?.homeFlashPromotion?.productList || [])

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [home, tree] = await Promise.all([getHomeContent(), getCategoryTree()])
    content.value = home
    categories.value = tree.flatMap((node) => node.children || [node]).slice(0, 12)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载首页失败'
  } finally {
    loading.value = false
  }
}

function goCategory(categoryId: number) {
  router.push({ path: '/search', query: { productCategoryId: String(categoryId) } })
}

function goSeckill(item: FlashPromotionProduct) {
  if (!item.relationId) return
  router.push({ path: `/seckill/${item.relationId}`, query: { productId: String(item.id) } })
}

onMounted(load)
</script>

<style scoped>
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h2 {
  margin: 0;
  font-size: 18px;
}

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

.category-list,
.brand-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.brand-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  font-size: 13px;
}

.brand-logo {
  width: 32px;
  height: 32px;
}
</style>
