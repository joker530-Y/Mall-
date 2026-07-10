<template>
  <section class="page-container page-stack">
    <div class="panel toolbar">
      <el-input v-model="filters.keyword" clearable placeholder="搜索商品" @keyup.enter="search" />
      <el-select v-model="filters.sort" style="width: 160px">
        <el-option label="相关度" :value="0" />
        <el-option label="新品" :value="1" />
        <el-option label="销量" :value="2" />
        <el-option label="价格升序" :value="3" />
        <el-option label="价格降序" :value="4" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="search">搜索</el-button>
    </div>

    <el-alert v-if="error" type="error" :title="error" show-icon />

    <div v-loading="loading" class="product-grid">
      <ProductCard v-for="item in tableData" :key="item.id" :product="item" />
    </div>
    <EmptyState v-if="!loading && !tableData.length" description="没有找到相关商品" />

    <el-pagination
      v-if="total > 0"
      layout="total, prev, pager, next"
      :total="total"
      v-model:current-page="pageNum"
      :page-size="pageSize"
      @current-change="search"
    />
  </section>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useRoute } from 'vue-router'
import ProductCard from '@/components/ProductCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { useListQuery } from '@/composables/useListQuery'
import { searchProducts, type Product } from '@/api/modules/product'

const route = useRoute()
const { filters, tableData, total, pageNum, pageSize, loading, error, search } = useListQuery<
  Product,
  { keyword?: string; sort?: number; productCategoryId?: number; brandId?: number }
>({
  fetcher: searchProducts,
  defaultQuery: {
    keyword: String(route.query.keyword || ''),
    sort: 0,
    productCategoryId: route.query.productCategoryId ? Number(route.query.productCategoryId) : undefined,
    brandId: route.query.brandId ? Number(route.query.brandId) : undefined
  },
  defaultPageSize: 12,
  syncKeys: ['keyword', 'sort', 'productCategoryId', 'brandId']
})

watch(
  () => [route.query.keyword, route.query.productCategoryId, route.query.brandId],
  ([keyword, categoryId, brandId]) => {
    filters.value.keyword = String(keyword || '')
    filters.value.productCategoryId = categoryId ? Number(categoryId) : undefined
    filters.value.brandId = brandId ? Number(brandId) : undefined
    search()
  },
  { immediate: true }
)
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
