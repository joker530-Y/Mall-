<template>
  <el-empty :description="description">
    <slot />
  </el-empty>
</template>

<script setup lang="ts">
defineProps<{ description?: string }>()
</script>
