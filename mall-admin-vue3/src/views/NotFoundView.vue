<template>
  <section class="not-found">
    <el-empty description="页面不存在或没有访问权限">
      <el-button type="primary" @click="goHome">返回看板</el-button>
    </el-empty>
  </section>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/dashboard')
}
</script>

<style scoped>
.not-found {
  display: grid;
  place-items: center;
  min-height: 60vh;
}
</style>
