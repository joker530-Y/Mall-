<template>
  <section class="page-stack">
    <PageHeader title="商品分类" description="管理分类树与显示状态" />

    <BaseTable :data="flatList" :loading="loading" :error="error" empty-text="暂无分类" height="560" @retry="load">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="name" label="分类名称" min-width="200" />
      <el-table-column prop="level" label="级别" width="80" />
      <el-table-column prop="productCount" label="商品数" width="90" />
      <el-table-column label="显示" width="90">
        <template #default="{ row }">
          <StatusTag :status="row.showStatus ?? 0" :map="{ 1: { label: '显示', type: 'success' }, 0: { label: '隐藏', type: 'info' } }" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-popconfirm title="确认删除该分类？" @confirm="remove(row.id)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </BaseTable>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/base/PageHeader.vue'
import BaseTable from '@/components/base/BaseTable.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import { deleteCategory, listCategoriesWithChildren, type ProductCategory } from '@/api/product'
import { useAsyncTask } from '@/composables/useAsyncTask'

const loading = ref(false)
const error = ref('')
const flatList = ref<ProductCategory[]>([])
const { run } = useAsyncTask()

function flatten(categories: ProductCategory[], level = 0): ProductCategory[] {
  return categories.flatMap((item) => [
    { ...item, name: `${'—'.repeat(level)}${item.name}` },
    ...flatten(item.children || [], level + 1)
  ])
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const tree = await listCategoriesWithChildren()
    flatList.value = flatten(tree)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function remove(id: number) {
  await run(() => deleteCategory(id), '已删除')
  load()
}

onMounted(load)
</script>
