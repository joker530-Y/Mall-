<template>
  <main class="login-page">
    <section class="login-panel panel">
      <h1>会员登录</h1>
      <el-form :model="form" label-position="top" @submit.prevent="submit">
        <el-form-item label="账号">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" autocomplete="current-password" show-password />
        </el-form-item>
        <el-button class="login-button" type="primary" :loading="loading" @click="submit">登录</el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { resolveInternalRedirect } from '@/utils/navigation'
import { PortalApiError } from '@/api/http/errors'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function submit() {
  if (loading.value) return
  if (!form.username.trim() || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    await auth.signIn(form.username.trim(), form.password)
    const redirect = resolveInternalRedirect(
      typeof route.query.redirect === 'string' ? route.query.redirect : undefined
    )
    await router.replace(redirect)
  } catch (err) {
    // 业务/鉴权错误已由 request 拦截器提示，避免重复弹窗
    if (!(err instanceof PortalApiError)) {
      ElMessage.error('登录失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.login-panel {
  width: min(420px, 100%);
}

.login-button {
  width: 100%;
}
</style>
