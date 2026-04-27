import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/report',
    component: () => import('@/views/Report.vue'),
    meta: { title: '问题上报' }
  },
  {
    path: '/task',
    component: () => import('@/views/Task.vue'),
    meta: { title: '我的任务' }
  },
  {
    path: '/notice',
    component: () => import('@/views/Notice.vue'),
    meta: { title: '公文通告' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router