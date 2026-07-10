<template>
  <section class="page-container page-stack">
    <h1>购物车</h1>
    <el-alert v-if="error" type="error" :title="error" show-icon />

    <div class="panel" v-loading="loading">
      <el-table :data="items" @selection-change="onSelect">
        <el-table-column type="selection" width="48" />
        <el-table-column label="商品" min-width="220">
          <template #default="{ row }">
            <div class="cart-product">
              <el-image :src="row.productPic" style="width:56px;height:56px" fit="cover" loading="lazy" />
              <div>
                <strong>{{ row.productName }}</strong>
                <p class="muted">{{ row.productAttr }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="100">
          <template #default="{ row }">¥{{ formatMoney(row.price) }}</template>
        </el-table-column>
        <el-table-column label="数量" width="140">
          <template #default="{ row }">
            <el-input-number
              :model-value="row.quantity"
              :min="1"
              size="small"
              @change="(value: number) => updateQuantity(row.id, value)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="danger" @click="remove([row.id])">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState v-if="!loading && !items.length" description="购物车还是空的，去逛逛吧">
        <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
      </EmptyState>
    </div>

    <div class="panel checkout-bar">
      <span>已选 {{ selectedIds.length }} 件</span>
      <el-button type="primary" :disabled="!selectedIds.length" @click="goCheckout">去结算</el-button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import EmptyState from '@/components/EmptyState.vue'
import { useCart } from '@/composables/useCart'
import { formatMoney } from '@/utils/format'

const router = useRouter()
const { items, loading, error, load, updateQuantity, remove } = useCart()
const selectedIds = ref<number[]>([])

function onSelect(rows: { id: number }[]) {
  selectedIds.value = rows.map((row) => row.id)
}

function goCheckout() {
  router.push({ path: '/checkout', query: { cartIds: selectedIds.value.join(',') } })
}

onMounted(load)
</script>

<style scoped>
.cart-product {
  display: flex;
  gap: 12px;
  align-items: center;
}

.muted {
  margin: 4px 0 0;
  color: var(--color-muted);
  font-size: 12px;
}

.checkout-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
