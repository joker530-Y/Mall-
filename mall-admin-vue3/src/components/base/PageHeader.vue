<template>
  <header class="page-header">
    <div>
      <h3>{{ title }}</h3>
      <p v-if="description">{{ description }}</p>
    </div>
    <div class="page-header-actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-header h3,
.page-header p {
  margin: 0;
}

.page-header p {
  margin-top: 6px;
  color: var(--color-muted);
  font-size: 13px;
}

.page-header-actions {
  display: flex;
  gap: 8px;
}
</style>
