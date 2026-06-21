<template>
  <div class="not-found-page">
    <van-nav-bar title="页面不存在" left-arrow @click-left="goBack" />
    <van-empty description="页面不存在或已失效">
      <van-button round type="primary" @click="goHome">返回首页</van-button>
    </van-empty>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { defaultHomePath } from '@/utils/roleAccess'

const router = useRouter()
const userStore = useUserStore()

function goBack() {
  router.back()
}

function goHome() {
  const roles = userStore.roles?.length ? userStore.roles : userStore.userInfo?.roles || []
  router.push(defaultHomePath(roles))
}
</script>

<style scoped>
.not-found-page {
  min-height: 100vh;
  background: #f7f8fa;
}
</style>
