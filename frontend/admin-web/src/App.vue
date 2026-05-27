<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getToken } from '@/utils/auth'

const userStore = useUserStore()

onMounted(async () => {
  userStore.initUser()
  if (getToken() && !userStore.roles?.length) {
    try {
      await userStore.getUserInfo()
    } catch (error) {
      console.warn('恢复登录态失败:', error)
    }
  }
})
</script>

<style>
#app {
  width: 100%;
  height: 100%;
}
</style>