<template>
  <div class="handle-page">
    <van-nav-bar title="案件处置" />

    <van-tabs v-model:active="activeTab" @change="onTabChange">
      <van-tab title="待处置" name="pending" />
      <van-tab title="经办案件" name="handled" />
    </van-tabs>

    <van-list
      v-model:loading="loading"
      :finished="finished"
      finished-text="没有更多了"
      @load="loadCases"
    >
      <van-cell-group inset>
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
</template>

<script setup>
defineOptions({ name: 'HandleList' })

import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { getPendingCaseList } from '@/api/case'
import { formatRemainingText, isRowStageOverdue, rowStageDeadline } from '@/utils/caseTimer'

const router = useRouter()
const activeTab = ref('pending')
const caseList = ref([])
const loading = ref(false)
const finished = ref(false)
const pageNum = ref(1)

const statusMap = {
  handling: '处置中',
  handle_finish: '待部门确认',
  pending_check: '待核实',
  closed: '已结案',
  forced_close: '已结案',
  pending_handle: '待处置',
  returned: '已回退'
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
  emptyText.value = name === 'pending' ? '暂无待处置案件' : '暂无经办案件'
  loadCases()
}

async function loadCases() {
  try {
    const status = activeTab.value === 'pending' ? 'handling' : 'handler_handled'
    const res = await getPendingCaseList({
      status,
      pageNum: pageNum.value,
      pageSize: 10
    })
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
</script>

<style scoped>
.handle-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
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
