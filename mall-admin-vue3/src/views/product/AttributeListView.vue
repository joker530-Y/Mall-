<template>
  <section class="page-stack">
    <PageHeader title="商品属性" description="按属性分类查看规格与参数" />

    <FilterBar :loading="loading" @search="search" @reset="reset">
      <el-input-number v-model="filters.cid" :min="1" controls-position="right" />
      <el-select v-model="filters.type" style="width: 140px">
        <el-option label="规格" :value="0" />
        <el-option label="参数" :value="1" />
      </el-select>
    </FilterBar>

    <BaseTable
      :data="tableData"
      :loading="loading"
      :error="error"
      :total="total"
      :page-num="pageNum"
      :page-size="pageSize"
      show-pagination
      height="560"
      @retry="reload"
      @page-change="(p) => { pageNum = p; search() }"
      @page-size-change="(s) => { pageSize = s; search() }"
    >
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="name" label="属性名" min-width="160" />
      <el-table-column prop="inputType" label="录入方式" width="100" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-popconfirm title="确认删除？" @confirm="remove(row.id)">
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
import { ref, watch } from 'vue'
import PageHeader from '@/components/base/PageHeader.vue'
import FilterBar from '@/components/base/FilterBar.vue'
import BaseTable from '@/components/base/BaseTable.vue'
import { useAsyncTask } from '@/composables/useAsyncTask'
import { deleteProductAttributes, listProductAttributes, type ProductAttribute } from '@/api/product'

const filters = ref({ cid: 1, type: 0 })
const tableData = ref<ProductAttribute[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const error = ref('')
const { run } = useAsyncTask()

async function reload() {
  loading.value = true
  error.value = ''
  try {
    const page = await listProductAttributes(filters.value.cid, filters.value.type, pageNum.value, pageSize.value)
    tableData.value = page.list
    total.value = page.total
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function search() {
  pageNum.value = 1
  reload()
}

function reset() {
  filters.value = { cid: 1, type: 0 }
  pageNum.value = 1
  reload()
}

async function remove(id: number) {
  await run(() => deleteProductAttributes([id]), '已删除')
  reload()
}

watch([pageNum, pageSize], reload, { immediate: true })
</script>
