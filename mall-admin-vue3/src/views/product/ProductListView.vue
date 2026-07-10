<template>
  <section class="page-stack">
    <PageHeader title="商品列表" description="查询、上下架与编辑商品" />

    <FilterBar :loading="loading" @search="search" @reset="reset">
      <el-input v-model="filters.keyword" clearable placeholder="商品名称" style="width: 200px" @keyup.enter="search" />
      <el-select v-model="filters.publishStatus" clearable placeholder="上架状态" style="width: 140px">
        <el-option label="上架" :value="1" />
        <el-option label="下架" :value="0" />
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
      <el-table-column label="图片" width="72">
        <template #default="{ row }">
          <el-image v-if="row.pic" :src="row.pic" style="width: 40px; height: 40px" fit="cover" />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="商品" min-width="180" show-overflow-tooltip />
      <el-table-column prop="productSn" label="货号" width="120" />
      <el-table-column label="价格" width="100">
        <template #default="{ row }">{{ formatMoney(row.price) }}</template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <StatusTag :status="row.publishStatus ?? 0" :map="PUBLISH_STATUS_MAP" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/product/${row.id}`)">编辑</el-button>
          <el-popconfirm
            :title="row.publishStatus === 1 ? '确认下架？' : '确认上架？'"
            @confirm="togglePublish(row)"
          >
            <template #reference>
              <el-button link>{{ row.publishStatus === 1 ? '下架' : '上架' }}</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </BaseTable>
  </section>
</template>

<script setup lang="ts">
import PageHeader from '@/components/base/PageHeader.vue'
import FilterBar from '@/components/base/FilterBar.vue'
import BaseTable from '@/components/base/BaseTable.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import { useListQuery } from '@/composables/useListQuery'
import { useAsyncTask } from '@/composables/useAsyncTask'
import { listProducts, updateProductPublishStatus, type Product } from '@/api/product'
import { PUBLISH_STATUS_MAP } from '@/constants/status'
import { formatMoney } from '@/utils/format'

const { run } = useAsyncTask()
const { filters, tableData, total, pageNum, pageSize, loading, error, search, reset, reload } = useListQuery<
  Product,
  { keyword?: string; publishStatus?: number }
>({
  fetcher: listProducts,
  defaultPageSize: 10,
  syncKeys: ['keyword', 'publishStatus']
})

async function togglePublish(row: Product) {
  const next = row.publishStatus === 1 ? 0 : 1
  await run(() => updateProductPublishStatus([row.id], next), next === 1 ? '已上架' : '已下架')
  reload()
}
</script>
