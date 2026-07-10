<template>
  <section class="page-container page-stack">
    <section class="panel account-card">
      <h1>个人中心</h1>
      <p>欢迎，{{ auth.nickname || auth.username }}</p>
      <div class="links">
        <el-button @click="$router.push('/orders')">我的订单</el-button>
        <el-button @click="$router.push('/account/addresses')">收货地址</el-button>
        <el-button @click="$router.push('/cart')">购物车</el-button>
        <el-button type="danger" plain @click="signOut">退出登录</el-button>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

onMounted(() => {
  if (!auth.nickname) auth.bootstrap()
})

function signOut() {
  auth.clearSession()
  router.push('/login')
}
</script>

<style scoped>
.links {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
}
</style>
