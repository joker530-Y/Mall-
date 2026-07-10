<template>
  <HomeRecommendPanel
    title="品牌推荐"
    description="首页品牌推荐管理"
    keyword-placeholder="品牌名称"
    name-field="brandName"
    name-label="品牌"
    keyword-key="brandName"
    :fetcher="(p) => listHomeBrands(p as never)"
    :toggle-status="updateHomeBrandRecommendStatus"
  />
</template>

<script setup lang="ts">
import HomeRecommendPanel from '@/components/business/HomeRecommendPanel.vue'
import { listHomeBrands, updateHomeBrandRecommendStatus } from '@/api/marketing'
</script>
