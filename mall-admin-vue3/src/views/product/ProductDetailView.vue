<template>
  <section class="page-stack">
    <PageHeader :title="`编辑商品 #${productId}`" description="编辑基础信息并保存" />

    <el-alert v-if="error" type="error" :title="error" show-icon />

    <section class="panel" v-loading="loading">
      <el-form v-if="form" :model="form" label-width="100px" style="max-width: 720px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="副标题"><el-input v-model="form.subTitle" /></el-form-item>
        <el-form-item label="货号"><el-input v-model="form.productSn" /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="form.price" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="form.stock" :min="0" /></el-form-item>
        <el-form-item label="上架">
          <el-switch v-model="form.publishStatus" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '@/components/base/PageHeader.vue'
import { getProductUpdateInfo, updateProduct } from '@/api/product'
import { useAsyncTask } from '@/composables/useAsyncTask'

const route = useRoute()
const router = useRouter()
const productId = computed(() => Number(route.params.id))
const loading = ref(false)
const error = ref('')
const rawPayload = ref<Record<string, unknown>>({})
const form = reactive({
  name: '',
  subTitle: '',
  productSn: '',
  price: 0,
  stock: 0,
  publishStatus: 0
})
const { loading: saving, run } = useAsyncTask()

async function load() {
  if (!Number.isFinite(productId.value) || productId.value <= 0) {
    error.value = '无效的商品 ID'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const data = await getProductUpdateInfo(productId.value)
    rawPayload.value = { ...data }
    Object.assign(form, {
      name: data.name || '',
      subTitle: data.subTitle || '',
      productSn: data.productSn || '',
      price: Number(data.price) || 0,
      stock: Number(data.stock) || 0,
      publishStatus: data.publishStatus ?? 0
    })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function save() {
  await run(
    () =>
      updateProduct(productId.value, {
        ...rawPayload.value,
        ...form
      }),
    '保存成功'
  )
  router.push('/product/list')
}

onMounted(load)
</script>
