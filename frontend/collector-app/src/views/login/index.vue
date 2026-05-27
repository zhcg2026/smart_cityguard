<template>
  <div class="login-page">
    <div class="login-header">
      <van-icon name="location" size="60" color="#1989fa" />
      <h2>{{ loginTitle }}</h2>
      <p>运城市城市综合管理服务系统</p>
    </div>

    <van-form @submit="handleLogin" class="login-form">
      <van-cell-group inset>
        <van-field
          v-model="form.username"
          name="username"
          label="用户名"
          placeholder="请输入用户名"
          :rules="[{ required: true, message: '请输入用户名' }]"
        />
        <van-field
          v-model="form.password"
          type="password"
          name="password"
          label="密码"
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请输入密码' }]"
        />
      </van-cell-group>

      <div class="login-btn">
        <van-button round block type="primary" native-type="submit" :loading="loading">
          登录
        </van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { defaultHomePath, primaryRoleLabel } from '@/utils/roleAccess'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const loginTitle = computed(() => {
  const label = primaryRoleLabel(userStore.roles)
  return label && label !== '用户' ? `智慧城管${label}` : '智慧城管移动端'
})

const form = reactive({
  username: '',
  password: ''
})

async function handleLogin() {
  loading.value = true
  try {
    await userStore.login(form)
    await userStore.getUserInfo()
    showToast('登录成功')
    router.push(defaultHomePath(userStore.roles))
  } catch (error) {
    showToast('登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 60px 20px;
}

.login-header {
  text-align: center;
  color: #fff;
  margin-bottom: 40px;

  h2 {
    margin: 16px 0 8px;
    font-size: 24px;
  }

  p {
    font-size: 14px;
  }
}

.login-form {
  .login-btn {
    margin: 20px 16px;
  }
}
</style>