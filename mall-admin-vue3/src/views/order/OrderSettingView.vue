<template>
  <section class="page-stack">
    <PageHeader title="订单设置" description="超时关闭、确认收货等全局参数" />

    <section class="panel" v-loading="loading">
      <el-form v-if="form" :model="form" label-width="160px" style="max-width: 560px">
        <el-form-item label="秒杀订单超时(分)"><el-input-number v-model="form.flashOrderOvertime" :min="1" /></el-form-item>
        <el-form-item label="正常订单超时(分)"><el-input-number v-model="form.normalOrderOvertime" :min="1" /></el-form-item>
        <el-form-item label="发货后自动确认(天)"><el-input-number v-model="form.confirmOvertime" :min="1" /></el-form-item>
        <el-form-item label="完成后自动五星(天)"><el-input-number v-model="form.finishOvertime" :min="1" /></el-form-item>
        <el-form-item label="评价超时(天)"><el-input-number v-model="form.commentOvertime" :min="1" /></el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        </el-form-item>
      </el-form>
    </section>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/base/PageHeader.vue'
import { getOrderSetting, updateOrderSetting, type OrderSetting } from '@/api/order'
import { useAsyncTask } from '@/composables/useAsyncTask'

const loading = ref(false)
const form = ref<OrderSetting>()
const { loading: saving, run } = useAsyncTask()

async function load() {
  loading.value = true
  form.value = await getOrderSetting(1)
  loading.value = false
}

async function save() {
  if (!form.value) return
  await run(() => updateOrderSetting(form.value!.id, form.value!), '保存成功')
}

onMounted(load)
</script>
