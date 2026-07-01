<template>
  <div class="main-layout">
    <router-view v-slot="{ Component }">
      <keep-alive :include="keepAliveNames">
        <component :is="Component" />
      </keep-alive>
    </router-view>

    <van-tabbar v-if="showTabbar" route fixed>
      <van-tabbar-item
        v-for="tab in tabs"
        :key="tab.path"
        :to="tab.path"
        :icon="tab.icon"
      >
        {{ tab.label }}
      </van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { isHandlerMobileUser, isCollectorMobileUser, isDeptMobileUser } from '@/utils/roleAccess'

const route = useRoute()
const userStore = useUserStore()

const tabs = computed(() => {
  const roles = userStore.roles?.length ? userStore.roles : userStore.userInfo?.roles || []
  const handler = isHandlerMobileUser(roles)
  const collector = isCollectorMobileUser(roles)
  const dept = isDeptMobileUser(roles)
  const canAppeal = handler || dept

  if (handler && !collector) {
    return [
      { path: '/handle', label: '首页', icon: 'home-o' },
      ...(canAppeal ? [{ path: '/appeal', label: '申诉', icon: 'comment-o' }] : []),
      { path: '/mine', label: '我的', icon: 'user-o' }
    ]
  }

  if (handler && collector) {
    return [
      { path: '/handle', label: '首页', icon: 'home-o' },
      ...(canAppeal ? [{ path: '/appeal', label: '申诉', icon: 'comment-o' }] : []),
      { path: '/report', label: '上报', icon: 'edit' },
      { path: '/task', label: '我的任务', icon: 'orders-o' },
      { path: '/mine', label: '我的', icon: 'user-o' }
    ]
  }

  if (dept) {
    return [
      { path: '/handle', label: '首页', icon: 'home-o' },
      { path: '/appeal', label: '申诉', icon: 'comment-o' },
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
  return !p.startsWith('/handle/') && !p.startsWith('/task/verify/') && !p.startsWith('/task/check/') && !p.startsWith('/appeal/submit/') && !p.match(/^\/appeal\/\d+$/)
})
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  padding-bottom: 50px;
}
</style>
