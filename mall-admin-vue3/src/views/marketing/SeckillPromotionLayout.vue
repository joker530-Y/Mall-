<template>
  <section class="page-stack">
    <PageHeader :title="`活动 #${promotionId}`" description="活动详情二级导航：场次商品 → 预热 → 运行监控 → 订单日志" />

    <el-tabs :model-value="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="场次与商品" name="products" />
      <el-tab-pane label="预热与库存" name="warmup" />
      <el-tab-pane label="实时运行" name="monitor" />
      <el-tab-pane label="订单日志" name="logs" />
    </el-tabs>

    <RouterView />
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '@/components/base/PageHeader.vue'

const route = useRoute()
const router = useRouter()

const promotionId = computed(() => route.params.id)
const activeTab = computed(() => {
  const segment = route.path.split('/').pop() || 'products'
  return ['products', 'warmup', 'monitor', 'logs'].includes(segment) ? segment : 'products'
})

function onTabChange(name: string | number) {
  router.push(`/marketing/flash-promotions/${promotionId.value}/${name}`)
}
</script>
