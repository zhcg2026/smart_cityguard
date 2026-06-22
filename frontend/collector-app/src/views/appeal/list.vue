<template>
  <div class="appeal-list-page">
    <van-nav-bar title="超时申诉" left-arrow @click-left="goBack" />

    <template v-if="isReviewer">
      <van-tabs v-model:active="reviewerTab" @change="onReviewerTabChange">
        <van-tab title="待办" name="pending" />
        <van-tab title="已办" name="done" />
        <van-tab title="全部" name="all" />
      </van-tabs>
    </template>
    <template v-else>
      <van-tabs v-model:active="activeTab" @change="onTabChange">
        <van-tab title="可申诉案件" name="appealable" />
        <van-tab title="已申诉案件" name="appealed" />
      </van-tabs>
    </template>

    <van-search v-model="searchKeyword" placeholder="搜索案件编号" @search="onSearch" @clear="onSearch" shape="round" />

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list v-model:loading="loading" :finished="finished" finished-text="没有更多了" @load="loadList">
        <template v-if="isReviewer">
          <van-cell
            v-for="item in appealList"
            :key="item.id"
            :title="item.caseCode || '案件'"
            :label="statusLabel(item.appealStatus)"
            :value="formatTime(item.applyTime)"
            is-link
            @click="goDetail(item.id)"
          />
        </template>
        <template v-else-if="activeTab === 'appealable'">
          <van-cell
            v-for="item in caseList"
            :key="item.id"
            :title="item.caseCode || '案件'"
            :label="item.address"
            is-link
            @click="goSubmit(item.id)"
          >
            <template #right-icon>
              <van-button size="small" type="primary">申诉</van-button>
            </template>
          </van-cell>
        </template>
        <template v-else>
          <van-cell
            v-for="item in appealList"
            :key="item.id"
            :title="item.caseCode || '案件'"
            :label="statusLabel(item.appealStatus)"
            :value="formatTime(item.applyTime)"
            is-link
            @click="goDetail(item.id)"
          />
        </template>
      </van-list>
    </van-pull-refresh>

    <van-empty v-if="!loading && currentList.length === 0" :description="emptyDesc" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getAppealableCases, getTimeoutAppealList } from '@/api/appeal'

const router = useRouter()
const userStore = useUserStore()

const REVIEWER_ROLES = ['DISPATCHER', 'ACCEPTOR', 'ADMIN', 'SUPERVISOR']
const isReviewer = computed(() => {
  const roles = userStore.roles || []
  return roles.some(r => REVIEWER_ROLES.includes(r))
})

const activeTab = ref('appealable')
const reviewerTab = ref('pending')
const searchKeyword = ref('')
const caseList = ref([])
const appealList = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const page = ref(1)

const currentList = computed(() => {
  if (isReviewer.value) return appealList.value
  return activeTab.value === 'appealable' ? caseList.value : appealList.value
})

const emptyDesc = computed(() => {
  if (isReviewer.value) {
    return reviewerTab.value === 'pending' ? '暂无待审核申诉' : '暂无申诉记录'
  }
  return activeTab.value === 'appealable' ? '暂无可申诉案件' : '暂无申诉记录'
})

function goBack() {
  router.back()
}

function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}

function statusLabel(s) {
  return {
    pending_dept: '待部门审核',
    pending_dispatcher: '待派遣员审核',
    pending_acceptor: '待受理员审核',
    approved: '已通过',
    rejected: '已驳回'
  }[s] || s || '—'
}

function goSubmit(caseId) {
  router.push(`/appeal/submit/${caseId}`)
}

function goDetail(id) {
  router.push(`/appeal/${id}`)
}

function resetAndLoad() {
  page.value = 1
  caseList.value = []
  appealList.value = []
  finished.value = false
  loadList()
}

function onTabChange() {
  resetAndLoad()
}

function onReviewerTabChange() {
  resetAndLoad()
}

function onRefresh() {
  page.value = 1
  caseList.value = []
  appealList.value = []
  finished.value = false
  loadList()
}

function onSearch() {
  resetAndLoad()
}

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: page.value, pageSize: 10 }
    if (searchKeyword.value.trim()) {
      params.caseCode = searchKeyword.value.trim()
    }
    let records, total, pages

    if (isReviewer.value) {
      params.tab = reviewerTab.value
      const res = await getTimeoutAppealList(params)
      records = res.data?.records || []
      total = res.data?.total ?? 0
      pages = res.data?.pages || 1
      if (page.value === 1) {
        appealList.value = records
      } else {
        appealList.value.push(...records)
      }
    } else if (activeTab.value === 'appealable') {
      const res = await getAppealableCases(params)
      records = res.data?.records || []
      total = res.data?.total ?? 0
      pages = res.data?.pages || 1
      if (page.value === 1) {
        caseList.value = records
      } else {
        caseList.value.push(...records)
      }
    } else {
      params.tab = 'all'
      const res = await getTimeoutAppealList(params)
      records = res.data?.records || []
      total = res.data?.total ?? 0
      pages = res.data?.pages || 1
      if (page.value === 1) {
        appealList.value = records
      } else {
        appealList.value.push(...records)
      }
    }

    if (records.length < 10 || page.value >= pages) {
      finished.value = true
    } else {
      page.value++
    }
  } catch {
    finished.value = true
  } finally {
    loading.value = false
    refreshing.value = false
  }
}
</script>

<style scoped>
.appeal-list-page {
  min-height: 100vh;
  background: #f7f8fa;
}
</style>
