<template>
  <div class="portal-shell">
    <header class="portal-header">
      <div class="page-container header-inner">
        <RouterLink to="/" class="brand">mall 商城</RouterLink>
        <el-input
          v-model="keyword"
          class="search-input"
          placeholder="搜索商品"
          clearable
          @keyup.enter="goSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="goSearch" />
          </template>
        </el-input>
        <nav class="header-nav">
          <RouterLink to="/seckill">秒杀</RouterLink>
          <RouterLink to="/orders">订单</RouterLink>
          <RouterLink to="/cart" class="cart-link">
            购物车
            <el-badge v-if="auth.cartCount" :value="auth.cartCount" />
          </RouterLink>
          <RouterLink v-if="auth.isAuthenticated" to="/account">{{ auth.nickname || auth.username }}</RouterLink>
          <RouterLink v-else to="/login">登录</RouterLink>
        </nav>
      </div>
    </header>
    <main class="portal-main">
      <RouterView />
    </main>
    <footer class="portal-footer">
      <div class="page-container">mall 消费者商城 · 模拟支付演示环境</div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const keyword = ref('')

onMounted(() => {
  if (auth.isAuthenticated) {
    void auth.bootstrap().catch(() => {
      // 会话恢复失败时由请求拦截器处理 401；非致命错误降级展示
    })
  }
})

function goSearch() {
  router.push({ path: '/search', query: { keyword: keyword.value || undefined } })
}
</script>

<style scoped>
.portal-shell {
  min-height: 100vh;
  display: grid;
  grid-template-rows: auto 1fr auto;
}

.portal-header {
  background: #fff;
  border-bottom: 1px solid var(--color-border);
}

.header-inner {
  display: grid;
  grid-template-columns: 120px 1fr auto;
  gap: 16px;
  align-items: center;
  min-height: 64px;
}

.brand {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-primary);
}

.search-input {
  max-width: 480px;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 16px;
}

.cart-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.portal-footer {
  padding: 16px 0;
  color: var(--color-muted);
  font-size: 13px;
  border-top: 1px solid var(--color-border);
  background: #fff;
}

@media (max-width: 768px) {
  .header-inner {
    grid-template-columns: 1fr;
    gap: 12px;
    padding-top: 12px;
    padding-bottom: 12px;
  }

  .header-nav {
    flex-wrap: wrap;
    gap: 12px;
  }
}
</style>
