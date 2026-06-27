<template>
  <div class="handle-page">
    <!-- 头部 -->
    <div class="header">
      <div class="user-info">
        <van-image round width="40" height="40" :src="userInfo.avatar || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'" />
        <div class="info">
          <div class="name">{{ displayName }}</div>
          <div class="dept">{{ departmentName }}</div>
        </div>
      </div>
      <van-badge :content="unreadCount || undefined" :max="99">
        <van-icon name="bell" size="24" @click="goMessage" />
      </van-badge>
    </div>

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

    <!-- 案件列表 -->
    <div class="case-section">
      <van-tabs v-model:active="activeTab" @change="onTabChange">
        <van-tab title="待处置" name="pending" />
        <van-tab title="经办案件" name="handled" />
      </van-tabs>

      <van-search v-model="searchKeyword" placeholder="搜索案件编号" @search="onSearch" @clear="onSearch" shape="round" />

      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadCases"
      >
      <van-cell-group>
        <van-cell
          v-for="item in caseList"
          :key="item.id"
          :title="item.caseCode"
          :label="caseCellLabel(item)"
          is-link
          @click="goDetail(item.id)"
        >
          <template #icon>
            <van-tag :type="activeTab === 'pending' ? 'warning' : 'default'">
              {{ statusTag(item) }}
            </van-tag>
          </template>
          <template #value>
            <div v-if="activeTab === 'pending' && hasTimerInfo(item)" class="timer-value" :class="{ overdue: isOverdue(item) }">
              <div class="timer-remain">{{ remainingText(item) }}</div>
              <div class="timer-deadline">截止 {{ formatDeadline(displayDeadline(item)) }}</div>
            </div>
            <div v-else class="status-value">{{ statusLabel(item.caseStatus) }}</div>
          </template>
        </van-cell>
      </van-cell-group>
      <van-empty v-if="caseList.length === 0 && finished" :description="emptyText" />
    </van-list>
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'HandleList' })

import { ref, computed, inject, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getPendingCaseList } from '@/api/case'
import { getDailyTipList, getAnnouncementList } from '@/api/message'
import { formatRemainingText, isRowStageOverdue, rowStageDeadline } from '@/utils/caseTimer'
import { isHandlerMobileUser, isDeptMobileUser } from '@/utils/roleAccess'

const router = useRouter()
const userStore = useUserStore()
const unreadCount = inject('unreadCount', ref(0))

const userInfo = computed(() => userStore.userInfo)
const roles = computed(() => userStore.roles?.length ? userStore.roles : userInfo.value?.roles || [])
const displayName = computed(() => userInfo.value.realName || '处置人员')
const departmentName = computed(() => userInfo.value?.departmentName?.trim() || '处置部门')

const activeTab = ref('pending')
const searchKeyword = ref('')
const caseList = ref([])
const loading = ref(false)
const finished = ref(false)
const pageNum = ref(1)

const dailyTips = ref([])
const announcements = ref([])
const contentLoading = ref(false)
const detailVisible = ref(false)
const detailItem = ref({})

function goMessage() {
  router.push('/message')
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

async function loadContent() {
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

const statusMap = {
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
}

function statusLabel(s) {
  return statusMap[s] || s
}

function statusTag(item) {
  if (activeTab.value === 'pending') return '处置中'
  return statusLabel(item.caseStatus)
}

const emptyText = ref('暂无待处置案件')

function caseCellLabel(item) {
  const parts = []
  if (item.smallName) parts.push(item.smallName)
  if (item.address) parts.push(item.address)
  return parts.join(' · ') || '—'
}

function displayDeadline(item) {
  return rowStageDeadline(item)
}

function hasTimerInfo(item) {
  return Boolean(displayDeadline(item) || item.timeRemaining)
}

function remainingText(item) {
  return formatRemainingText(item)
}

function isOverdue(item) {
  return isRowStageOverdue(item)
}

function formatDeadline(t) {
  if (!t) return ''
  const s = String(t)
  return s.length >= 16 ? s.slice(0, 16).replace('T', ' ') : s
}

function onTabChange(name) {
  caseList.value = []
  pageNum.value = 1
  finished.value = false
  loading.value = true
  searchKeyword.value = ''
  emptyText.value = name === 'pending' ? '暂无待处置案件' : '暂无经办案件'
  loadCases()
}

function onSearch() {
  caseList.value = []
  pageNum.value = 1
  finished.value = false
  loading.value = true
  loadCases()
}

async function loadCases() {
  try {
    const isDept = isDeptMobileUser(roles.value)
    let status
    if (activeTab.value === 'pending') {
      status = isDept ? 'handler_dept_todo' : 'handling'
    } else {
      status = 'handler_handled'
    }
    const params = {
      status,
      pageNum: pageNum.value,
      pageSize: 10
    }
    if (searchKeyword.value.trim()) {
      params.caseCode = searchKeyword.value.trim()
    }
    const res = await getPendingCaseList(params)
    const records = res.data?.records || []
    caseList.value.push(...records)
    loading.value = false
    const total = res.data?.total ?? 0
    if (caseList.value.length >= total) {
      finished.value = true
    } else {
      pageNum.value++
    }
  } catch {
    loading.value = false
    finished.value = true
  }
}

function goDetail(id) {
  router.push(`/handle/${id}`)
}

onMounted(async () => {
  await userStore.getUserInfo().catch(() => {})
  await loadContent()
})
</script>

<style scoped>
.handle-page {
  min-height: 100vh;
  background: #f7f8fa;
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
      margin-left: 12px;
      color: #fff;

      .name {
        font-size: 16px;
        font-weight: bold;
      }

      .dept {
        font-size: 12px;
        margin-top: 4px;
      }
    }
  }

  :deep(.van-icon) {
    color: #fff;
  }
}

.case-section {
  margin-top: 12px;
  background: #f7f8fa;
  padding: 0 16px;
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

.timer-value {
  text-align: right;
  font-size: 12px;
  line-height: 1.4;
  color: #323233;
}

.timer-value.overdue {
  color: #ee0a24;
}

.timer-remain {
  font-weight: 600;
}

.timer-deadline {
  color: #969799;
  font-size: 11px;
}

.timer-value.overdue .timer-deadline {
  color: #ee0a24;
}

.status-value {
  text-align: right;
  font-size: 12px;
  color: #969799;
}
</style>
