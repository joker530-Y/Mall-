<template>
  <section class="page-grid">
    <section class="panel">
      <div class="panel-header">
        <h3>秒杀活动</h3>
        <el-input v-model="keyword" clearable placeholder="活动名称" @keyup.enter="loadPromotions">
          <template #append>
            <el-button :icon="Search" @click="loadPromotions" />
          </template>
        </el-input>
      </div>
      <el-table :data="promotions" v-loading="promotionLoading" height="560" highlight-current-row @current-change="selectPromotion">
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column prop="title" label="活动" min-width="150" />
        <el-table-column prop="status" label="状态" width="86">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">
              {{ row.status === 1 ? '上线' : '下线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="日期" width="190">
          <template #default="{ row }">{{ row.startDate }} 至 {{ row.endDate }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        layout="prev, pager, next"
        :total="promotionTotal"
        :page-size="promotionQuery.pageSize"
        v-model:current-page="promotionQuery.pageNum"
        @current-change="loadPromotions"
      />
    </section>

    <section class="panel">
      <div class="panel-header">
        <div>
          <h3>场次商品</h3>
          <p>当前活动 ID：{{ selectedPromotionId || '-' }}</p>
        </div>
        <el-input-number v-model="sessionId" :min="1" controls-position="right" @change="loadProducts" />
      </div>
      <el-table :data="products" v-loading="productLoading" height="520">
        <el-table-column prop="id" label="关系ID" width="90" />
        <el-table-column prop="productName" label="商品" min-width="180" show-overflow-tooltip />
        <el-table-column prop="flashPromotionPrice" label="秒杀价" width="100" />
        <el-table-column prop="flashPromotionCount" label="库存" width="90" />
        <el-table-column prop="flashPromotionLimit" label="限购" width="80" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" :icon="Lightning" @click="warmup(row.id)">预热</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        layout="prev, pager, next"
        :total="productTotal"
        :page-size="productQuery.pageSize"
        v-model:current-page="productQuery.pageNum"
        @current-change="loadProducts"
      />
    </section>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lightning, Search } from '@element-plus/icons-vue'
import {
  listFlashProducts,
  listFlashPromotions,
  warmupSeckill,
  type FlashPromotion,
  type FlashPromotionProduct
} from '@/api/seckill'

const router = useRouter()
const keyword = ref('')
const sessionId = ref(1)
const selectedPromotionId = ref<number>()
const promotions = ref<FlashPromotion[]>([])
const products = ref<FlashPromotionProduct[]>([])
const promotionTotal = ref(0)
const productTotal = ref(0)
const promotionLoading = ref(false)
const productLoading = ref(false)
const promotionQuery = reactive({ pageNum: 1, pageSize: 8 })
const productQuery = reactive({ pageNum: 1, pageSize: 8 })

async function loadPromotions() {
  promotionLoading.value = true
  try {
    const page = await listFlashPromotions({ keyword: keyword.value, ...promotionQuery })
    promotions.value = page.list
    promotionTotal.value = page.total
    if (!selectedPromotionId.value && promotions.value.length > 0) {
      selectedPromotionId.value = promotions.value[0].id
      await loadProducts()
    }
  } finally {
    promotionLoading.value = false
  }
}

async function loadProducts() {
  if (!selectedPromotionId.value) return
  productLoading.value = true
  try {
    const page = await listFlashProducts({
      flashPromotionId: selectedPromotionId.value,
      flashPromotionSessionId: sessionId.value,
      ...productQuery
    })
    products.value = page.list
    productTotal.value = page.total
  } finally {
    productLoading.value = false
  }
}

async function selectPromotion(row?: FlashPromotion) {
  if (!row) return
  selectedPromotionId.value = row.id
  productQuery.pageNum = 1
  await loadProducts()
}

async function warmup(relationId: number) {
  const result = await warmupSeckill(relationId)
  ElMessage.success(`已预热库存 ${result.stock}`)
  router.push({ name: 'dashboard', query: { relationId } })
}

onMounted(loadPromotions)
</script>
