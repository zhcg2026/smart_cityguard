<template>
  <div class="handle-page">
    <van-nav-bar title="待处置案件" />

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
            <van-tag type="warning">处置中</van-tag>
          </template>
          <template #value>
            <span v-if="displayDeadline(item)" :class="{ overdue: isOverdue(item) }">
              {{ deadlinePrefix(item) }}{{ formatDeadline(displayDeadline(item)) }}
            </span>
          </template>
        </van-cell>
      </van-cell-group>
      <van-empty v-if="caseList.length === 0 && finished" description="暂无待处置案件" />
    </van-list>
  </div>
</template>

<script setup>
defineOptions({ name: 'HandleList' })

import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { getPendingCaseList } from '@/api/case'

const router = useRouter()
const caseList = ref([])
const loading = ref(false)
const finished = ref(false)
const pageNum = ref(1)

function caseCellLabel(item) {
  const parts = []
  if (item.smallName) parts.push(item.smallName)
  if (item.address) parts.push(item.address)
  return parts.join(' · ') || '—'
}

function displayDeadline(item) {
  return item.stageDeadlineTime || item.deadlineTime || ''
}

function deadlinePrefix(item) {
  if (item.timerStageName) return `${item.timerStageName} `
  return ''
}

function isOverdue(item) {
  if (item.stageTimeout != null) return item.stageTimeout
  if (item.handleTimeout) return true
  const t = displayDeadline(item)
  if (!t) return false
  return new Date(t).getTime() < Date.now()
}

function formatDeadline(t) {
  if (!t) return ''
  const s = String(t)
  return s.length >= 16 ? s.slice(0, 16).replace('T', ' ') : s
}

async function loadCases() {
  try {
    const res = await getPendingCaseList({
      status: 'handling',
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

.overdue {
  color: #ee0a24;
  font-size: 12px;
}
</style>
