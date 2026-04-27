import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/case/register',
    component: () => import('@/views/case/Register.vue'),
    meta: { title: '登记栏' }
  },
  {
    path: '/case/pending',
    component: () => import('@/views/case/Pending.vue'),
    meta: { title: '待查栏' }
  },
  {
    path: '/case/filing',
    component: () => import('@/views/case/Filing.vue'),
    meta: { title: '立案栏' }
  },
  {
    path: '/case/check',
    component: () => import('@/views/case/Check.vue'),
    meta: { title: '核查栏' }
  },
  {
    path: '/case/handled',
    component: () => import('@/views/case/Handled.vue'),
    meta: { title: '经办案件' }
  },
  {
    path: '/standard',
    component: () => import('@/views/Standard.vue'),
    meta: { title: '立结案标准' }
  },
  {
    path: '/user',
    component: () => import('@/views/User.vue'),
    meta: { title: '用户管理' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router