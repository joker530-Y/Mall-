<template>
  <RouterLink :to="`/product/${product.id}`" class="product-card">
    <el-image :src="product.pic" fit="cover" class="product-card__image" loading="lazy">
      <template #error><div class="image-fallback">暂无图片</div></template>
    </el-image>
    <div class="product-card__body">
      <h3>{{ product.name }}</h3>
      <p v-if="product.subTitle" class="muted">{{ product.subTitle }}</p>
      <PriceTag :price="product.promotionPrice ?? product.price" :original="product.price" />
      <p v-if="product.sale" class="muted">已售 {{ product.sale }}</p>
    </div>
  </RouterLink>
</template>

<script setup lang="ts">
import PriceTag from './PriceTag.vue'
import type { Product } from '@/api/modules/product'

defineProps<{ product: Product }>()
</script>

<style scoped>
.product-card {
  display: grid;
  gap: 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: #fff;
  overflow: hidden;
  transition: box-shadow 0.2s ease;
}

.product-card:hover {
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

.product-card__image {
  width: 100%;
  aspect-ratio: 1;
}

.product-card__body {
  padding: 12px;
  display: grid;
  gap: 6px;
}

.product-card__body h3 {
  margin: 0;
  font-size: 14px;
  line-height: 1.4;
  min-height: 2.8em;
}

.muted {
  margin: 0;
  color: var(--color-muted);
  font-size: 12px;
}

.image-fallback {
  display: grid;
  place-items: center;
  height: 100%;
  background: #f3f4f6;
  color: var(--color-muted);
}
</style>
