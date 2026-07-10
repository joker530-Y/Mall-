<template>
  <el-container class="app-shell">
    <el-aside :width="collapsed ? '64px' : '232px'" class="sidebar">
      <div class="brand">
        <span class="brand-mark">M</span>
        <div v-if="!collapsed">
          <strong>mall 管理端</strong>
          <small>运营后台</small>
        </div>
      </div>
      <el-menu router :default-active="activePath" class="nav-menu" :collapse="collapsed">
        <template v-for="item in sidebarMenus" :key="item.path">
          <el-sub-menu v-if="item.children?.length" :index="item.path">
            <template #title>
              <el-icon v-if="item.icon"><component :is="resolveIcon(item.icon)" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
              <el-icon v-if="child.icon"><component :is="resolveIcon(child.icon)" /></el-icon>
              <span>{{ child.title }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.path">
            <el-icon v-if="item.icon"><component :is="resolveIcon(item.icon)" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <div class="topbar-left">
          <el-button text :icon="collapsed ? Expand : Fold" @click="collapsed = !collapsed" />
          <div>
            <h2>{{ pageTitle }}</h2>
            <p>秒杀运营与业务管理</p>
          </div>
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
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Collection,
  DataLine,
  Document,
  Expand,
  Fold,
  Goods,
  Key,
  Lightning,
  Link,
  List,
  Menu,
  Odometer,
  Picture,
  Reading,
  RefreshLeft,
  Setting,
  Shop,
  Star,
  SwitchButton,
  Ticket,
  Tickets,
  TrendCharts,
  User,
  UserFilled
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { usePermissionStore } from '@/stores/permission'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const permission = usePermissionStore()
const collapsed = ref(false)

const sidebarMenus = computed(() => permission.sidebarMenus)
const activePath = computed(() => route.path)
const pageTitle = computed(() => String(route.meta.title || '工作台'))

const iconMap: Record<string, unknown> = {
  Collection,
  DataLine,
  Document,
  Goods,
  Key,
  Lightning,
  Link,
  List,
  Menu,
  Odometer,
  Picture,
  Reading,
  RefreshLeft,
  Setting,
  Shop,
  Star,
  Ticket,
  Tickets,
  TrendCharts,
  User,
  UserFilled
}

function resolveIcon(name?: string) {
  return (name && iconMap[name]) || DataLine
}

async function signOut() {
  await auth.signOut()
  router.push({ name: 'login' })
}
</script>

<style scoped>
.topbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
