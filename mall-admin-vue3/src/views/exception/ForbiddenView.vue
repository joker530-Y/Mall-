<template>
  <section class="forbidden">
    <el-result icon="warning" title="403" sub-title="你没有访问该页面的权限">
      <template #extra>
        <el-button type="primary" @click="goBack">返回上一页</el-button>
        <el-button @click="goHome">返回看板</el-button>
      </template>
    </el-result>
  </section>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goBack() {
  router.back()
}

function goHome() {
  router.push('/dashboard')
}
</script>

<style scoped>
.forbidden {
  display: grid;
  place-items: center;
  min-height: 60vh;
}
</style>
