<template>
  <section class="data-panel">
    <el-alert
      v-if="error"
      type="error"
      :title="error"
      show-icon
      :closable="false"
      class="panel-alert"
    >
      <template #default>
        <el-button size="small" @click="$emit('retry')">重试</el-button>
      </template>
    </el-alert>
    <el-table v-bind="$attrs" :data="data" v-loading="loading">
      <slot />
      <template #empty>
        <EmptyState :description="emptyText" />
      </template>
    </el-table>
    <el-pagination
      v-if="showPagination"
      class="table-pagination"
      layout="total, sizes, prev, pager, next"
      :total="total"
      :page-sizes="[10, 20, 50]"
      :page-size="pageSize"
      :current-page="pageNum"
      @size-change="(size: number) => $emit('page-size-change', size)"
      @current-change="(page: number) => $emit('page-change', page)"
    />
  </section>
</template>

<script setup lang="ts">
import EmptyState from './EmptyState.vue'

defineProps<{
  data: unknown[]
  loading?: boolean
  error?: string
  emptyText?: string
  showPagination?: boolean
  total?: number
  pageNum?: number
  pageSize?: number
}>()

defineEmits<{
  retry: []
  'page-change': [page: number]
  'page-size-change': [size: number]
}>()
</script>

<style scoped>
.data-panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: #fff;
  padding: 16px;
}

.panel-alert {
  margin-bottom: 12px;
}

.table-pagination {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
