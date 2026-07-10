<template>
  <HomeRecommendPanel
    title="新品推荐"
    description="首页新品推荐管理"
    keyword-placeholder="商品名称"
    name-field="productName"
    name-label="商品"
    keyword-key="productName"
    :fetcher="(p) => listHomeNewProducts(p as never)"
    :toggle-status="updateHomeNewProductRecommendStatus"
  />
</template>

<script setup lang="ts">
import HomeRecommendPanel from '@/components/business/HomeRecommendPanel.vue'
import { listHomeNewProducts, updateHomeNewProductRecommendStatus } from '@/api/marketing'
</script>
