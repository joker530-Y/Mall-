<template>
  <section class="page-container page-stack">
    <div class="head">
      <h1>收货地址</h1>
      <el-button type="primary" @click="openDialog()">新增地址</el-button>
    </div>

    <el-alert v-if="error" type="error" :title="error" show-icon />

    <div class="panel" v-loading="loading">
      <el-table :data="addresses">
        <el-table-column prop="name" label="收货人" width="100" />
        <el-table-column prop="phoneNumber" label="电话" width="140" />
        <el-table-column label="地址" min-width="220">
          <template #default="{ row }">
            {{ row.province }}{{ row.city }}{{ row.region }}{{ row.detailAddress }}
          </template>
        </el-table-column>
        <el-table-column label="默认" width="80">
          <template #default="{ row }">{{ row.defaultStatus === 1 ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button link @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="visible" :title="editingId ? '编辑地址' : '新增地址'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="收货人"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phoneNumber" /></el-form-item>
        <el-form-item label="省份"><el-input v-model="form.province" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="form.city" /></el-form-item>
        <el-form-item label="区县"><el-input v-model="form.region" /></el-form-item>
        <el-form-item label="详细地址"><el-input v-model="form.detailAddress" /></el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="form.defaultStatus" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  addAddress,
  deleteAddress,
  listAddresses,
  updateAddress,
  type MemberAddress
} from '@/api/modules/member'

const addresses = ref<MemberAddress[]>([])
const loading = ref(false)
const error = ref('')
const visible = ref(false)
const editingId = ref<number>()
const form = reactive<Partial<MemberAddress>>({
  name: '',
  phoneNumber: '',
  province: '',
  city: '',
  region: '',
  detailAddress: '',
  defaultStatus: 0
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    addresses.value = await listAddresses()
  } catch (err) {
    addresses.value = []
    error.value = err instanceof Error ? err.message : '加载地址失败'
  } finally {
    loading.value = false
  }
}

function openDialog(row?: MemberAddress) {
  editingId.value = row?.id
  Object.assign(form, row || {
    name: '',
    phoneNumber: '',
    province: '',
    city: '',
    region: '',
    detailAddress: '',
    defaultStatus: 0
  })
  visible.value = true
}

async function save() {
  try {
    if (editingId.value) {
      await updateAddress(editingId.value, form)
    } else {
      await addAddress(form)
    }
    ElMessage.success('保存成功')
    visible.value = false
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存失败'
  }
}

async function remove(id: number) {
  try {
    await deleteAddress(id)
    ElMessage.success('已删除')
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '删除失败'
  }
}

onMounted(load)
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
