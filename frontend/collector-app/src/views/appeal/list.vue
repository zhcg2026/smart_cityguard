<template>
  <div class="appeal-list-page">
    <van-nav-bar title="超时申诉" left-arrow @click-left="goBack" />

    <van-tabs v-model:active="activeTab" @change="onTabChange">
      <van-tab title="可申诉案件" name="appealable" />
      <van-tab title="已申诉案件" name="appealed" />
    </van-tabs>

    <van-search v-model="searchKeyword" placeholder="搜索案件编号" @search="onSearch" @clear="onSearch" shape="round" />

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list v-model:loading="loading" :finished="finished" finished-text="没有更多了" @load="loadList">
        <template v-if="activeTab === 'appealable'">
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

    <van-empty v-if="!loading && currentList.length === 0" :description="activeTab === 'appealable' ? '暂无可申诉案件' : '暂无申诉记录'" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getAppealableCases, getTimeoutAppealList } from '@/api/appeal'

const router = useRouter()
const activeTab = ref('appealable')
const searchKeyword = ref('')
const caseList = ref([])
const appealList = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const page = ref(1)

const currentList = computed(() => activeTab.value === 'appealable' ? caseList.value : appealList.value)

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

function onTabChange() {
  page.value = 1
  caseList.value = []
  appealList.value = []
  finished.value = false
  loading.value = true
  loadList()
}

function onRefresh() {
  page.value = 1
  caseList.value = []
  appealList.value = []
  finished.value = false
  loadList()
}

function onSearch() {
  page.value = 1
  caseList.value = []
  appealList.value = []
  finished.value = false
  loading.value = true
  loadList()
}

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: page.value, pageSize: 10 }
    if (searchKeyword.value.trim()) {
      params.caseCode = searchKeyword.value.trim()
    }
    let records, total, pages
    if (activeTab.value === 'appealable') {
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
