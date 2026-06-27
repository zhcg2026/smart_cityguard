<template>
  <div class="home-page">
    <!-- 头部 -->
    <div class="header">
      <div class="user-info">
        <van-image round width="60" height="60" :src="userInfo.avatar || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'" />
        <div class="info">
          <div class="name">{{ userInfo.realName || '采集员' }}</div>
          <div class="grid">{{ gridDisplayName }}</div>
        </div>
      </div>
      <van-badge :content="unreadCount || undefined" :max="99">
        <van-icon name="bell" size="24" @click="goMessage" />
      </van-badge>
    </div>

    <!-- 快捷功能 -->
    <van-grid :column-num="2" class="quick-grid">
      <van-grid-item icon="edit" text="问题上报" to="/report" />
      <van-grid-item icon="orders-o" text="我的任务" to="/task" />
    </van-grid>

    <!-- 今日提示 -->
    <van-cell-group title="今日提示" inset>
      <van-cell
        v-for="tip in dailyTips"
        :key="tip.id"
        :title="tip.title || tip.content"
        :label="tip.title && tip.content ? tip.content : ''"
        icon="info-o"
        is-link
        @click="openContentDetail(tip)"
      />
      <van-empty v-if="!contentLoading && dailyTips.length === 0" description="暂无提示" image-size="60" />
    </van-cell-group>

    <!-- 公文通告 -->
    <van-cell-group title="公文通告" inset>
      <van-cell
        v-for="item in announcements"
        :key="item.id"
        :title="item.title"
        :value="formatPublishTime(item.publishTime)"
        is-link
        @click="openContentDetail(item)"
      />
      <van-empty v-if="!contentLoading && announcements.length === 0" description="暂无通告" image-size="60" />
    </van-cell-group>

    <van-popup v-model:show="detailVisible" round position="bottom" :style="{ maxHeight: '70%' }">
      <div class="content-detail">
        <div class="content-detail__title">{{ detailItem.title || '详情' }}</div>
        <div v-if="detailItem.publishTime" class="content-detail__meta">
          {{ formatPublishTime(detailItem.publishTime) }}
          <span v-if="detailItem.publisherName"> · {{ detailItem.publisherName }}</span>
        </div>
        <div class="content-detail__body">{{ detailItem.content || '—' }}</div>
        <van-button block type="primary" @click="detailVisible = false">关闭</van-button>
      </div>
    </van-popup>

    <!-- 待办任务 -->
    <van-cell-group inset>
      <template #title>
        <div class="section-title-row">
          <span>待办任务</span>
          <span v-if="pendingTasks.length" class="section-link" @click="goTaskList">查看全部</span>
        </div>
      </template>
      <van-cell
        v-for="task in pendingTasks"
        :key="`${task.type}-${task.id}`"
        :title="task.title"
        :label="task.deadline"
        is-link
        @click="openTask(task)"
      >
        <template #icon>
          <van-tag :type="task.type === 'check' ? 'warning' : 'success'">
            {{ task.type === 'check' ? '核查' : '核实' }}
          </van-tag>
        </template>
      </van-cell>
      <van-empty v-if="!pendingLoading && pendingTasks.length === 0" description="暂无待办" image-size="60" />
    </van-cell-group>

    <!-- 我的最近上报 -->
    <van-cell-group title="我的最近上报" inset>
      <van-cell
        v-for="c in myRecentCases"
        :key="c.id"
        :title="c.caseCode || '案件'"
        :label="c.address"
        :value="statusLabel(c.caseStatus)"
      />
      <van-empty v-if="myRecentCases.length === 0" description="暂无上报记录" image-size="60" />
    </van-cell-group>
  </div>
</template>

<script setup>
defineOptions({ name: 'Home' })
import { ref, computed, onMounted, onBeforeUnmount, inject } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getToken } from '@/utils/auth'
import { getMyCaseList } from '@/api/case'
import { getCheckTaskList, getVerifyTaskList } from '@/api/task'
import { isCollectorMobileUser, isHandlerMobileUser } from '@/utils/roleAccess'
import {
  getDailyTipList,
  getAnnouncementList
} from '@/api/message'
import { formatTaskRemaining, formatDateTimeShort } from '@/utils/taskTimer'

const router = useRouter()
const userStore = useUserStore()
const unreadCount = inject('unreadCount', ref(0))

const userInfo = computed(() => userStore.userInfo)
const roles = computed(() => userStore.roles?.length ? userStore.roles : userInfo.value?.roles || [])
const gridDisplayName = computed(() => {
  if (isHandlerMobileUser(roles.value) && !isCollectorMobileUser(roles.value)) {
    return userInfo.value?.departmentName?.trim() || '处置部门'
  }
  const name = userInfo.value?.gridName?.trim()
  return name || '未绑定片区'
})
const dailyTips = ref([])
const announcements = ref([])
const pendingTasks = ref([])
const pendingLoading = ref(false)
const myRecentCases = ref([])
const contentLoading = ref(false)
const detailVisible = ref(false)
const detailItem = ref({})

const statusLabel = (s) =>
  ({
    pending_verify: '待核查',
    pending_register: '待立案',
    pending_dispatch: '待派遣',
    pending_handle: '待处置',
    handling: '处置中',
    suspended: '挂起中',
    handle_finish: '待部门确认',
    pending_check: '待核实',
    checking: '核查中',
    closed: '已结案',
    forced_close: '已结案',
    not_accepted: '不受理',
    returned: '已退回'
  }[s] || s)

function formatTaskDeadlineLine(task) {
  const remain = formatTaskRemaining(task)
  const deadline = formatDateTimeShort(task.deadlineTime)
  const parts = []
  if (remain) parts.push(remain)
  if (deadline) parts.push(`截止 ${deadline}`)
  return parts.join(' · ') || '截止时间：—'
}

function mapPendingTask(task, type) {
  return {
    id: task.id,
    type,
    title: task.caseCode || task.caseNo || task.taskCode || '任务',
    deadline: formatTaskDeadlineLine(task)
  }
}

async function loadPendingTasks() {
  pendingLoading.value = true
  try {
    const [checkRes, verifyRes] = await Promise.all([
      getCheckTaskList({ pageNum: 1, pageSize: 5, status: 0 }),
      getVerifyTaskList({ pageNum: 1, pageSize: 5, status: 0 })
    ])
    const checkRows = (checkRes.data?.records || [])
      .filter((t) => t.taskStatus === 'pending')
      .map((t) => mapPendingTask(t, 'check'))
    const verifyRows = (verifyRes.data?.records || [])
      .filter((t) => t.taskStatus === 'pending')
      .map((t) => mapPendingTask(t, 'verify'))
    pendingTasks.value = [...checkRows, ...verifyRows].slice(0, 8)
  } catch {
    pendingTasks.value = []
  } finally {
    pendingLoading.value = false
  }
}

function openTask(task) {
  if (!task?.id) return
  if (task.type === 'check') {
    router.push(`/task/check/${task.id}`)
  } else {
    router.push(`/task/verify/${task.id}`)
  }
}

function goTaskList() {
  router.push('/task')
}

async function reloadHome() {
  await Promise.all([loadHomeContent(), loadPendingTasks()])
  try {
    const res = await getMyCaseList({ pageNum: 1, pageSize: 5 })
    myRecentCases.value = res.data?.records || []
  } catch {
    myRecentCases.value = []
  }
}

async function loadHomeContent() {
  contentLoading.value = true
  try {
    const [tipRes, annRes] = await Promise.all([
      getDailyTipList({ limit: 5 }),
      getAnnouncementList({ limit: 5 })
    ])
    dailyTips.value = tipRes.data || []
    announcements.value = annRes.data || []
  } catch {
    dailyTips.value = []
    announcements.value = []
  } finally {
    contentLoading.value = false
  }
}

function formatPublishTime(value) {
  if (!value) return ''
  const text = String(value)
  return text.length >= 16 ? text.slice(0, 16).replace('T', ' ') : text
}

function openContentDetail(row) {
  if (!row) return
  detailItem.value = { ...row }
  detailVisible.value = true
}

onMounted(async () => {
  userStore.initUser()
  if (!getToken()) {
    return
  }
  try {
    await userStore.getUserInfo()
  } catch {
    /* 使用本地缓存 */
  }
  await reloadHome()
  window.addEventListener('cityguard:refresh-lists', reloadHome)
})

onBeforeUnmount(() => {
  window.removeEventListener('cityguard:refresh-lists', reloadHome)
})

function goMessage() {
  router.push('/message')
}
</script>

<style scoped>
.home-page {
  padding-bottom: 60px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #1989fa, #36cfc9);

  .user-info {
    display: flex;
    align-items: center;

    .info {
      margin-left: 16px;
      color: #fff;

      .name {
        font-size: 18px;
        font-weight: bold;
      }

      .grid {
        font-size: 12px;
        margin-top: 4px;
      }
    }
  }

  .van-icon {
    color: #fff;
  }
}

.quick-grid {
  margin: 20px 0;
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 4px;
}

.section-link {
  font-size: 12px;
  color: #1989fa;
  font-weight: normal;
}

.content-detail {
  padding: 20px 16px 24px;

  &__title {
    font-size: 17px;
    font-weight: 600;
    margin-bottom: 8px;
  }

  &__meta {
    font-size: 12px;
    color: #969799;
    margin-bottom: 12px;
  }

  &__body {
    font-size: 14px;
    line-height: 1.6;
    color: #323233;
    white-space: pre-wrap;
    max-height: 45vh;
    overflow-y: auto;
    margin-bottom: 16px;
  }
}
</style>