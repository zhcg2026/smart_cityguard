<template>
  <router-view />
</template>

<script setup>
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
// 初始化用户信息
userStore.initUser()
</script>

<style>
#app {
  width: 100%;
  height: 100%;
}
</style>