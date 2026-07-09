<template>
  <el-container class="app-shell">
    <el-aside width="232px" class="sidebar">
      <div class="brand">
        <span class="brand-mark">M</span>
        <div>
          <strong>mall 管理端</strong>
          <small>Phase 3</small>
        </div>
      </div>
      <el-menu router :default-active="route.path" class="nav-menu">
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon>
          <span>秒杀看板</span>
        </el-menu-item>
        <el-menu-item index="/flash-promotions">
          <el-icon><Lightning /></el-icon>
          <span>活动与预热</span>
        </el-menu-item>
        <el-menu-item index="/seckill-orders">
          <el-icon><Tickets /></el-icon>
          <span>订单结果</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <div>
          <h2>{{ title }}</h2>
          <p>Redis + Lua + MQ 秒杀链路管理视图</p>
        </div>
        <div class="topbar-actions">
          <el-tag type="info" effect="plain">{{ auth.username || 'admin' }}</el-tag>
          <el-button :icon="SwitchButton" @click="signOut">退出</el-button>
        </div>
      </el-header>
      <el-main class="content">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DataLine, Lightning, SwitchButton, Tickets } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const title = computed(() => {
  if (route.name === 'flash-promotions') return '活动与预热'
  if (route.name === 'seckill-orders') return '订单结果'
  return '秒杀看板'
})

async function signOut() {
  await auth.signOut()
  router.push({ name: 'login' })
}
</script>
