<template>
  <div class="basic-layout">
    <el-container>
      <!-- 侧边栏 -->
      <el-aside :width="sidebarWidth" class="sidebar">
        <div class="logo">
          <img src="@/assets/images/logo.png" alt="logo" v-if="!appStore.sidebar.collapsed" />
          <span v-if="!appStore.sidebar.collapsed">智慧城管</span>
          <span v-else>城管</span>
        </div>
        <el-scrollbar>
          <el-menu
            :default-active="activeMenu"
            :collapse="appStore.sidebar.collapsed"
            :collapse-transition="false"
            :unique-opened="true"
            background-color="#304156"
            text-color="#bfcbd9"
            active-text-color="#409EFF"
            router
          >
            <template v-for="route in menuRoutes" :key="route.path">
              <!-- 没有子菜单 -->
              <el-menu-item
                v-if="!route.children || route.children.length === 1"
                :index="route.children?.[0]?.path || route.path"
              >
                <el-icon><component :is="route.meta?.icon || 'Document'" /></el-icon>
                <template #title>{{ route.meta?.title }}</template>
              </el-menu-item>

              <!-- 有子菜单 -->
              <el-sub-menu v-else :index="route.path">
                <template #title>
                  <el-icon><component :is="route.meta?.icon || 'Document'" /></el-icon>
                  <span>{{ route.meta?.title }}</span>
                </template>
                <el-menu-item
                  v-for="child in route.children"
                  :key="child.path"
                  :index="child.path"
                >
                  {{ child.meta?.title }}
                </el-menu-item>
              </el-sub-menu>
            </template>
          </el-menu>
        </el-scrollbar>
      </el-aside>

      <!-- 主内容区 -->
      <el-container class="main-container">
        <!-- 顶部导航栏 -->
        <el-header class="header">
          <div class="left">
            <el-icon
              class="collapse-btn"
              @click="appStore.toggleSidebar"
            >
              <Fold v-if="!appStore.sidebar.collapsed" />
              <Expand v-else />
            </el-icon>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
                {{ item.meta?.title }}
              </el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="right">
            <el-dropdown trigger="click">
              <div class="user-info">
                <el-avatar :size="32">{{ userStore.userInfo.realName?.charAt(0) || 'U' }}</el-avatar>
                <span class="username">{{ userStore.userInfo.realName || '用户' }}</span>
                <el-icon><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="goProfile">个人中心</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <!-- 标签页 -->
        <div class="tabs-wrapper" v-if="visitedViews.length > 0">
          <el-tabs
            v-model="activeTab"
            type="card"
            closable
            @tab-remove="removeTab"
            @tab-click="clickTab"
          >
            <el-tab-pane
              v-for="view in visitedViews"
              :key="view.path"
              :label="view.meta?.title"
              :name="view.path"
            />
          </el-tabs>
        </div>

        <!-- 内容区域 -->
        <el-main class="main">
          <router-view v-slot="{ Component }">
            <transition name="fade-transform" mode="out-in">
              <keep-alive>
                <component :is="Component" />
              </keep-alive>
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

// 侧边栏宽度
const sidebarWidth = computed(() => appStore.sidebar.collapsed ? '64px' : '220px')

// 当前激活菜单
const activeMenu = computed(() => route.path)

// 面包屑
const breadcrumbs = computed(() => {
  return route.matched.filter(item => item.meta?.title)
})

// 菜单路由
const menuRoutes = computed(() => {
  const routes = router.options.routes.filter(r => !r.meta?.hidden && r.children)
  return routes
})

// 标签页相关
const visitedViews = ref([])
const activeTab = ref('')

watch(
  () => route.path,
  (path) => {
    activeTab.value = path
    const view = visitedViews.value.find(v => v.path === path)
    if (!view && route.meta?.title) {
      visitedViews.value.push({
        path: route.path,
        meta: route.meta
      })
    }
  },
  { immediate: true }
)

function removeTab(path) {
  const index = visitedViews.value.findIndex(v => v.path === path)
  if (index > -1) {
    visitedViews.value.splice(index, 1)
    if (activeTab.value === path) {
      const lastView = visitedViews.value[visitedViews.value.length - 1]
      if (lastView) {
        router.push(lastView.path)
      } else {
        router.push('/dashboard')
      }
    }
  }
}

function clickTab(tab) {
  router.push(tab.paneName)
}

// 个人中心
function goProfile() {
  router.push('/system/user/profile')
}

// 退出登录
async function handleLogout() {
  await userStore.logout()
  router.push('/login')
}
</script>

<style lang="scss" scoped>
.basic-layout {
  width: 100%;
  height: 100vh;
  overflow: hidden;

  .el-container {
    height: 100%;
  }

  .sidebar {
    background-color: #304156;
    transition: width 0.3s;
    overflow: hidden;

    .logo {
      height: 60px;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: #263445;
      color: #fff;
      font-size: 18px;
      font-weight: bold;

      img {
        height: 32px;
        margin-right: 8px;
      }
    }

    .el-menu {
      border-right: none;
    }
  }

  .main-container {
    display: flex;
    flex-direction: column;
    background-color: #f0f2f5;
  }

  .header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    background-color: #fff;
    box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
    padding: 0 20px;

    .left {
      display: flex;
      align-items: center;

      .collapse-btn {
        font-size: 20px;
        cursor: pointer;
        margin-right: 16px;
      }
    }

    .right {
      .user-info {
        display: flex;
        align-items: center;
        cursor: pointer;

        .username {
          margin: 0 8px;
        }
      }
    }
  }

  .tabs-wrapper {
    background-color: #fff;
    padding: 0 10px;
    border-bottom: 1px solid #e4e7ed;

    :deep(.el-tabs__header) {
      margin: 0;
    }

    :deep(.el-tabs__item) {
      height: 32px;
      line-height: 32px;
    }
  }

  .main {
    flex: 1;
    overflow: auto;
    padding: 20px;
  }
}

// 过渡动画
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
</style>