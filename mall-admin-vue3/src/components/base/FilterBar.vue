<template>
  <section class="filter-bar">
    <slot />
    <div class="filter-actions">
      <el-button type="primary" :loading="loading" @click="$emit('search')">查询</el-button>
      <el-button @click="$emit('reset')">重置</el-button>
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{ loading?: boolean }>()
defineEmits<{ search: []; reset: [] }>()
</script>

<style scoped>
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.filter-actions {
  display: flex;
  gap: 8px;
}
</style>
