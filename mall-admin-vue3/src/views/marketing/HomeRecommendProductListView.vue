<template>
  <HomeRecommendPanel
    title="人气推荐"
    description="首页人气商品推荐"
    keyword-placeholder="商品名称"
    name-field="productName"
    name-label="商品"
    keyword-key="productName"
    :fetcher="(p) => listHomeRecommendProducts(p as never)"
    :toggle-status="updateHomeRecommendProductStatus"
  />
</template>

<script setup lang="ts">
import HomeRecommendPanel from '@/components/business/HomeRecommendPanel.vue'
import { listHomeRecommendProducts, updateHomeRecommendProductStatus } from '@/api/marketing'
</script>
