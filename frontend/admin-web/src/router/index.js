import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import { useUserStore } from '@/stores/user'
import { getToken } from '@/utils/auth'

// 路由配置
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'HomeFilled' }
      }
    ]
  },
  // 案件管理
  {
    path: '/case',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/case/list',
    meta: { title: '案件管理', icon: 'Document' },
    children: [
      {
        path: 'list',
        name: 'CaseList',
        component: () => import('@/views/case/CaseList.vue'),
        meta: { title: '案件列表' }
      },
      {
        path: 'pending',
        name: 'CasePending',
        component: () => import('@/views/case/CasePending.vue'),
        meta: { title: '待处理案件' }
      },
      {
        path: 'detail/:id',
        name: 'CaseDetail',
        component: () => import('@/views/case/CaseDetail.vue'),
        meta: { title: '案件详情', hidden: true }
      }
    ]
  },
  // 任务管理
  {
    path: '/task',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/task/verify',
    meta: { title: '任务管理', icon: 'List' },
    children: [
      {
        path: 'verify',
        name: 'VerifyTask',
        component: () => import('@/views/task/VerifyTask.vue'),
        meta: { title: '核查任务' }
      },
      {
        path: 'check',
        name: 'CheckTask',
        component: () => import('@/views/task/CheckTask.vue'),
        meta: { title: '核实任务' }
      }
    ]
  },
  // 申诉管理
  {
    path: '/appeal',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/appeal/list',
    meta: { title: '申诉管理', icon: 'ChatDotSquare' },
    children: [
      {
        path: 'list',
        name: 'AppealList',
        component: () => import('@/views/appeal/AppealList.vue'),
        meta: { title: '申诉列表' }
      }
    ]
  },
  // 考核评价
  {
    path: '/evaluation',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/evaluation/index',
    meta: { title: '考核评价', icon: 'DataAnalysis' },
    children: [
      {
        path: 'index',
        name: 'EvaluationIndex',
        component: () => import('@/views/evaluation/index.vue'),
        meta: { title: '考核统计' }
      }
    ]
  },
  // 地理信息
  {
    path: '/geo',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/geo/grid',
    meta: { title: '地理信息', icon: 'Location' },
    children: [
      {
        path: 'grid',
        name: 'GridManage',
        component: () => import('@/views/geo/GridManage.vue'),
        meta: { title: '网格管理' }
      },
      {
        path: 'street',
        name: 'StreetManage',
        component: () => import('@/views/geo/StreetManage.vue'),
        meta: { title: '街道社区' }
      }
    ]
  },
  // 系统配置
  {
    path: '/system',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/system/user',
    meta: { title: '系统管理', icon: 'Setting' },
    children: [
      {
        path: 'user',
        name: 'UserManage',
        component: () => import('@/views/system/UserManage.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'role',
        name: 'RoleManage',
        component: () => import('@/views/system/RoleManage.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: 'dept',
        name: 'DeptManage',
        component: () => import('@/views/system/DeptManage.vue'),
        meta: { title: '部门管理' }
      },
      {
        path: 'menu',
        name: 'MenuManage',
        component: () => import('@/views/system/MenuManage.vue'),
        meta: { title: '菜单管理' }
      }
    ]
  },
  // 业务配置
  {
    path: '/config',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/config/standard',
    meta: { title: '业务配置', icon: 'Tools' },
    children: [
      {
        path: 'standard',
        name: 'StandardManage',
        component: () => import('@/views/config/StandardManage.vue'),
        meta: { title: '立结案标准' }
      },
      {
        path: 'category',
        name: 'CategoryManage',
        component: () => import('@/views/config/CategoryManage.vue'),
        meta: { title: '案件分类' }
      },
      {
        path: 'timelimit',
        name: 'TimeLimitConfig',
        component: () => import('@/views/config/TimeLimitConfig.vue'),
        meta: { title: '时限配置' }
      },
      {
        path: 'announcement',
        name: 'AnnouncementManage',
        component: () => import('@/views/config/AnnouncementManage.vue'),
        meta: { title: '通告管理' }
      }
    ]
  },
  // 消息通知
  {
    path: '/message',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/message/list',
    meta: { title: '消息通知', icon: 'Bell' },
    children: [
      {
        path: 'list',
        name: 'MessageList',
        component: () => import('@/views/message/MessageList.vue'),
        meta: { title: '消息列表' }
      }
    ]
  },
  // 404
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404', hidden: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  NProgress.start()

  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 智慧城管` : '智慧城管管理平台'

  const token = getToken()
  if (token) {
    if (to.path === '/login') {
      next({ path: '/' })
    } else {
      const userStore = useUserStore()
      if (userStore.roles.length === 0) {
        userStore.getUserInfo().then(() => {
          next()
        }).catch(() => {
          userStore.logout().then(() => {
            next({ path: '/login' })
          })
        })
      } else {
        next()
      }
    }
  } else {
    if (to.path === '/login') {
      next()
    } else {
      next({ path: '/login' })
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router