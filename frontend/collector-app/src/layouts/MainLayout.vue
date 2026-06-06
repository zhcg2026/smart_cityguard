<template>
  <div class="main-layout">
    <router-view v-slot="{ Component }">
      <keep-alive :include="keepAliveNames">
        <component :is="Component" />
      </keep-alive>
    </router-view>

    <van-tabbar v-if="showTabbar" v-model="active" fixed @change="handleChange">
      <van-tabbar-item
        v-for="(tab, index) in tabs"
        :key="tab.path"
        :icon="tab.icon"
      >
        {{ tab.label }}
      </van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { isHandlerMobileUser, isCollectorMobileUser } from '@/utils/roleAccess'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const active = ref(0)

const tabs = computed(() => {
  const roles = userStore.roles?.length ? userStore.roles : userStore.userInfo?.roles || []
  const handler = isHandlerMobileUser(roles)
  const collector = isCollectorMobileUser(roles)

  if (handler && !collector) {
    return [
      { path: '/handle', label: '待处置', icon: 'todo-list-o' },
      { path: '/mine', label: '我的', icon: 'user-o' }
    ]
  }

  if (handler && collector) {
    return [
      { path: '/handle', label: '待处置', icon: 'todo-list-o' },
      { path: '/report', label: '上报', icon: 'edit' },
      { path: '/task', label: '我的任务', icon: 'orders-o' },
      { path: '/mine', label: '我的', icon: 'user-o' }
    ]
  }

  return [
    { path: '/home', label: '首页', icon: 'home-o' },
    { path: '/report', label: '上报', icon: 'edit' },
    { path: '/task', label: '我的任务', icon: 'todo-list-o' },
    { path: '/mine', label: '我的', icon: 'user-o' }
  ]
})

const keepAliveNames = computed(() => {
  const names = ['Home', 'Report', 'Task', 'Mine', 'HandleList']
  return names
})

const showTabbar = computed(() => {
  const p = route.path
  return !p.startsWith('/handle/') && !p.startsWith('/task/verify/') && !p.startsWith('/task/check/')
})

watch(
  () => [route.path, tabs.value],
  () => {
    const idx = tabs.value.findIndex((t) => t.path === route.path)
    active.value = idx >= 0 ? idx : 0
  },
  { immediate: true }
)

function handleChange(index) {
  const tab = tabs.value[index]
  if (tab) router.push(tab.path)
}
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  padding-bottom: 50px;
}
</style>
