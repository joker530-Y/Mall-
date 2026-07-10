<template>
  <section class="page-stack">
    <PageHeader title="会员等级" description="会员成长与权益配置" />

    <BaseTable :data="levels" :loading="loading" :error="error" height="560" @retry="load">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="name" label="等级" min-width="140" />
      <el-table-column prop="growthPoint" label="成长值" width="100" />
      <el-table-column label="默认" width="80">
        <template #default="{ row }">{{ row.defaultStatus === 1 ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column prop="note" label="备注" min-width="160" show-overflow-tooltip />
    </BaseTable>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/base/PageHeader.vue'
import BaseTable from '@/components/base/BaseTable.vue'
import { listMemberLevels, type MemberLevel } from '@/api/member'

const levels = ref<MemberLevel[]>([])
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    levels.value = await listMemberLevels()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
