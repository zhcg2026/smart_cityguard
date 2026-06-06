import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'
import { defaultHomePath, isHandlerMobileUser, isCollectorMobileUser } from '@/utils/roleAccess'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/index.vue'),
        meta: { title: '首页', collectorOnly: true }
      },
      {
        path: 'handle',
        name: 'HandleList',
        component: () => import('@/views/handle/index.vue'),
        meta: { title: '待处置', handler: true }
      },
      {
        path: 'handle/:id',
        name: 'HandleDetail',
        component: () => import('@/views/handle/HandleDetail.vue'),
        meta: { title: '案件处置', handler: true, hideTabbar: true }
      },
      {
        path: 'report',
        name: 'Report',
        component: () => import('@/views/report/index.vue'),
        meta: { title: '问题上报', collector: true }
      },
      {
        path: 'task',
        name: 'Task',
        component: () => import('@/views/task/index.vue'),
        meta: { title: '我的任务', collector: true }
      },
      {
        path: 'task/verify/:id',
        name: 'VerifyTask',
        component: () => import('@/views/task/VerifyTask.vue'),
        meta: { title: '核实任务详情', hideTabbar: true }
      },
      {
        path: 'task/check/:id',
        name: 'CheckTask',
        component: () => import('@/views/task/CheckTask.vue'),
        meta: { title: '核查任务详情', hideTabbar: true }
      },
      {
        path: 'mine',
        name: 'Mine',
        component: () => import('@/views/mine/index.vue'),
        meta: { title: '我的' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

function resolveRoles() {
  const userStore = useUserStore()
  if (userStore.roles?.length) return userStore.roles
  try {
    const raw = localStorage.getItem('userInfo')
    if (raw && raw !== 'undefined') {
      return JSON.parse(raw).roles || []
    }
  } catch {
    /* ignore */
  }
  return []
}

router.beforeEach((to, from, next) => {
  const roles = resolveRoles()
  const handlerOnly = isHandlerMobileUser(roles) && !isCollectorMobileUser(roles)
  const appTitle = handlerOnly ? '智慧城管处置端' : '智慧城管采集端'
  document.title = to.meta.title ? `${to.meta.title} - ${appTitle}` : appTitle

  const token = getToken()
  if (!token) {
    if (to.path === '/login') next()
    else next('/login')
    return
  }

  if (to.path === '/login') {
    next(defaultHomePath(roles))
    return
  }

  if (handlerOnly) {
    if (to.meta.collectorOnly || to.path === '/home' || to.path === '/report' || to.path === '/task') {
      if (to.path.startsWith('/handle')) {
        next()
      } else {
        next('/handle')
      }
      return
    }
  }

  if (to.path === '/' || to.path === '/home') {
    const home = defaultHomePath(roles)
    if (to.path !== home) {
      next(home)
      return
    }
  }

  next()
})

export default router
