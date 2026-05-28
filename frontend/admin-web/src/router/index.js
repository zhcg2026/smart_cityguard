import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getToken } from '@/utils/auth'
import { RoleGroups, canAccessMeta } from '@/utils/roleAccess'

// 路由配置（meta.roles 与后端 role_code 对应；ADMIN 在 canAccessMeta 中放行全部）
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/no-permission',
    name: 'NoPermission',
    component: () => import('@/views/error/NoPermission.vue'),
    meta: { title: '无权限', hidden: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/dashboard',
    meta: { roles: RoleGroups.DASHBOARD },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'HomeFilled', roles: RoleGroups.DASHBOARD }
      },
      {
        path: 'notice/announcement',
        name: 'AnnouncementBrowse',
        component: () => import('@/views/notice/AnnouncementBrowse.vue'),
        meta: { title: '公文通告', hidden: true, roles: RoleGroups.DASHBOARD }
      },
      {
        path: 'notice/dailytip',
        name: 'DailyTipBrowse',
        component: () => import('@/views/notice/DailyTipBrowse.vue'),
        meta: { title: '今日提示', hidden: true, roles: RoleGroups.DASHBOARD }
      }
    ]
  },
  // 案件管理
  {
    path: '/case',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/case/list',
    meta: { title: '案件管理', icon: 'Document', roles: RoleGroups.CASE },
    children: [
      {
        path: 'list',
        name: 'CaseList',
        component: () => import('@/views/case/CaseList.vue'),
        meta: { title: '案件列表', roles: RoleGroups.CASE }
      },
      {
        path: 'register',
        name: 'CaseManualRegister',
        component: () => import('@/views/case/CaseManualRegister.vue'),
        meta: { title: '案件登记', roles: RoleGroups.ACCEPTOR_CASE }
      },
      {
        path: 'pending-register',
        name: 'CasePendingRegister',
        component: () => import('@/views/case/CasePending.vue'),
        meta: {
          title: '待立案案件',
          roles: RoleGroups.ACCEPTOR_CASE,
          presetStatus: 'acceptor_pending_register'
        }
      },
      {
        path: 'pending-verify',
        name: 'CasePendingVerify',
        component: () => import('@/views/case/CasePending.vue'),
        meta: {
          title: '待核实案件',
          roles: RoleGroups.ACCEPTOR_CASE,
          presetStatus: 'acceptor_pending_verify',
          acceptorActionMode: 'verify'
        }
      },
      {
        path: 'pending-check',
        name: 'CasePendingCheck',
        component: () => import('@/views/case/CasePending.vue'),
        meta: {
          title: '待核查案件',
          roles: RoleGroups.ACCEPTOR_CASE,
          presetStatus: 'acceptor_collect_check'
        }
      },
      {
        path: 'pending-close',
        name: 'CaseAcceptorPendingClose',
        component: () => import('@/views/case/CasePending.vue'),
        meta: {
          title: '待我结案案件',
          roles: RoleGroups.ACCEPTOR_CASE,
          presetStatus: 'acceptor_pending_close',
          acceptorActionMode: 'close'
        }
      },
      {
        path: 'my-registered',
        name: 'CaseAcceptorRegistered',
        component: () => import('@/views/case/CasePending.vue'),
        meta: {
          title: '我立案的案件',
          roles: RoleGroups.ACCEPTOR_CASE,
          presetStatus: 'acceptor_registered'
        }
      },
      {
        path: 'rejected',
        name: 'CaseRejected',
        component: () => import('@/views/case/CasePending.vue'),
        meta: {
          title: '作废案件',
          roles: RoleGroups.ACCEPTOR_CASE,
          presetStatus: 'not_accepted'
        }
      },
      {
        path: 'pending-dispatch',
        name: 'CaseDispatcherPendingDispatch',
        component: () => import('@/views/case/CasePending.vue'),
        meta: {
          title: '待派遣案件',
          roles: RoleGroups.DISPATCHER_CASE,
          presetStatus: 'dispatcher_pending_dispatch'
        }
      },
      {
        path: 'pending-review',
        name: 'CaseDispatcherPendingReview',
        component: () => import('@/views/case/CasePending.vue'),
        meta: {
          title: '待办审核',
          roles: RoleGroups.DISPATCHER_CASE,
          presetStatus: 'dispatcher_pending_review'
        }
      },
      {
        path: 'returned',
        name: 'CaseDispatcherReturned',
        component: () => import('@/views/case/CasePending.vue'),
        meta: {
          title: '回退案件',
          roles: RoleGroups.DISPATCHER_CASE,
          presetStatus: 'dispatcher_returned'
        }
      },
      {
        path: 'handled',
        name: 'CaseDispatcherHandled',
        component: () => import('@/views/case/CasePending.vue'),
        meta: {
          title: '经办案件',
          roles: RoleGroups.DISPATCHER_CASE,
          presetStatus: 'dispatcher_handled'
        }
      },
      {
        path: 'adjustment-review',
        name: 'CaseAdjustmentReview',
        component: () => import('@/views/case/AdjustmentReview.vue'),
        meta: {
          title: '延期挂账审批',
          roles: RoleGroups.DISPATCHER_CASE
        }
      },
      {
        path: 'pending',
        name: 'CasePending',
        component: () => import('@/views/case/CasePending.vue'),
        meta: { title: '待处理队列', roles: RoleGroups.CASE_PENDING_OPS }
      },
      {
        path: 'detail/:id',
        name: 'CaseDetail',
        component: () => import('@/views/case/CaseDetail.vue'),
        meta: { title: '案件详情', hidden: true, roles: RoleGroups.CASE }
      }
    ]
  },
  // 任务台账（核查/核实督办，仅管理员与值班长）
  {
    path: '/task',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/task/ledger',
    meta: { title: '任务台账', icon: 'Notebook', roles: RoleGroups.TASK_LEDGER },
    children: [
      {
        path: 'ledger',
        name: 'TaskLedger',
        component: () => import('@/views/task/TaskLedger.vue'),
        meta: { title: '任务台账', roles: RoleGroups.TASK_LEDGER }
      },
      {
        path: 'check',
        redirect: (to) => ({ path: '/task/ledger', query: { ...to.query, tab: 'check' } }),
        meta: { hidden: true }
      },
      {
        path: 'verify',
        redirect: (to) => ({ path: '/task/ledger', query: { ...to.query, tab: 'verify' } }),
        meta: { hidden: true }
      }
    ]
  },
  // 申诉管理
  {
    path: '/appeal',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/appeal/list',
    meta: { title: '申诉管理', icon: 'ChatDotSquare', roles: RoleGroups.APPEAL },
    children: [
      {
        path: 'list',
        name: 'AppealList',
        component: () => import('@/views/appeal/AppealList.vue'),
        meta: { title: '申诉列表' }
      },
      {
        path: 'detail/:id',
        name: 'AppealDetail',
        component: () => import('@/views/appeal/AppealDetail.vue'),
        meta: { title: '申诉详情', hidden: true, activeMenu: '/appeal/list' }
      }
    ]
  },
  // 考核评价（综合查询与考核统计并列；后续统计报表共用查询条件）
  {
    path: '/evaluation',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/evaluation/index',
    meta: { title: '考核评价', icon: 'DataAnalysis', roles: RoleGroups.EVALUATION_SECTION },
    children: [
      {
        path: 'index',
        name: 'EvaluationIndex',
        component: () => import('@/views/evaluation/index.vue'),
        meta: { title: '考核统计', roles: RoleGroups.EVALUATION }
      },
      {
        path: 'query',
        name: 'CaseQuery',
        component: () => import('@/views/case/CaseQuery.vue'),
        meta: { title: '综合查询', roles: RoleGroups.CASE_QUERY }
      }
    ]
  },
  {
    path: '/case/query',
    redirect: '/evaluation/query',
    meta: { hidden: true }
  },
  // 采集员管理（独立一级菜单）
  {
    path: '/collector',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/collector/index',
    meta: { roles: RoleGroups.GEO },
    children: [
      {
        path: 'index',
        name: 'CollectorManage',
        component: () => import('@/views/geo/CollectorManage.vue'),
        meta: { title: '采集员管理', icon: 'Avatar', roles: RoleGroups.GEO }
      }
    ]
  },
  // 旧路径兼容（原地理信息子菜单）
  {
    path: '/geo/collector',
    redirect: '/collector/index',
    meta: { hidden: true }
  },
  // 地理信息（街道社区暂隐藏，恢复开发时去掉 street 路由的 meta.hidden）
  {
    path: '/geo',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/geo/grid',
    meta: { title: '地理信息', icon: 'Location', roles: RoleGroups.GEO },
    children: [
      {
        path: 'grid',
        name: 'GridManage',
        component: () => import('@/views/geo/GridManage.vue'),
        meta: { title: '片区管理' }
      },
      {
        path: 'street',
        name: 'StreetManage',
        component: () => import('@/views/geo/StreetManage.vue'),
        meta: { title: '街道社区', hidden: true }
      }
    ]
  },
  // 系统配置
  {
    path: '/system',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/system/user',
    meta: { title: '系统管理', icon: 'Setting', roles: RoleGroups.SYSTEM },
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
    meta: { title: '业务配置', icon: 'Tools', roles: RoleGroups.CONFIG },
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
        meta: { title: '内容发布', roles: RoleGroups.CONFIG }
      }
    ]
  },
  // 消息通知
  {
    path: '/message',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/message/list',
    meta: { title: '消息通知', icon: 'Bell', roles: RoleGroups.MESSAGE },
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

function requiredRolesForRoute(matched) {
  const rec = [...matched].reverse().find((r) => r.meta?.roles?.length)
  return rec?.meta?.roles || null
}

function tryNextWithRoleGuard(to, userStore, next) {
  // 无权限页面直接放行，避免死循环
  if (to.path === '/no-permission') {
    next()
    return
  }
  const req = requiredRolesForRoute(to.matched)
  if (req?.length && !canAccessMeta({ roles: req }, userStore.roles)) {
    ElMessage.warning('当前账号无权限访问该功能')
    next({ path: '/no-permission', replace: true })
    return
  }
  next()
}

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
        userStore
          .getUserInfo()
          .then(() => {
            tryNextWithRoleGuard(to, userStore, next)
          })
          .catch(() => {
            userStore.logout().then(() => {
              next({ path: '/login' })
            })
          })
      } else {
        tryNextWithRoleGuard(to, userStore, next)
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