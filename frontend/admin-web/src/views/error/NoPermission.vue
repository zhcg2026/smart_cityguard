<template>
  <div class="error-page">
    <div class="error-content">
      <h1>无权限</h1>
      <p>当前账号无权限访问该功能</p>
      <div class="actions">
        <el-button type="primary" @click="goHome">进入工作台</el-button>
        <el-button @click="reLogin">重新登录</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

function goHome() {
  router.push('/')
}

function reLogin() {
  userStore.logout().then(() => {
    router.replace('/login')
  })
}
</script>

<style lang="scss" scoped>
.error-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  .error-content {
    text-align: center;
    h1 { font-size: 120px; color: #e6a23c; margin-bottom: 20px; }
    p { font-size: 24px; color: #666; margin-bottom: 30px; }
    .actions {
      display: flex;
      gap: 16px;
      justify-content: center;
    }
  }
}
</style>
