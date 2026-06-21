<template>
  <div class="mine-report-page">
    <van-nav-bar title="我的上报" left-arrow @click-left="goBack" />

    <van-search v-model="searchKeyword" placeholder="搜索案件编号" @search="onSearch" @clear="onSearch" shape="round" />

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list v-model:loading="loading" :finished="finished" finished-text="没有更多了" @load="loadList">
        <van-cell v-for="item in list" :key="item.id" :title="item.caseCode || '案件'" :label="item.address" :value="statusLabel(item.caseStatus)" is-link @click="goDetail(item.id)" />
      </van-list>
    </van-pull-refresh>

    <van-empty v-if="!loading && list.length === 0" description="暂无上报记录" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyCaseList } from '@/api/case'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const page = ref(1)
const searchKeyword = ref('')

const statusLabel = (s) =>
  ({
    pending_verify: '待核查',
    pending_register: '待立案',
    pending_dispatch: '待派遣',
    pending_handle: '待处置',
    handle_finish: '待部门确认',
    pending_check: '待核实',
    closed: '已结案',
    not_accepted: '不受理'
  }[s] || s)

function goBack() {
  router.back()
}

function goDetail(id) {
  router.push(`/handle/${id}`)
}

function onRefresh() {
  page.value = 1
  list.value = []
  finished.value = false
  loadList()
}

function onSearch() {
  page.value = 1
  list.value = []
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
    const res = await getMyCaseList(params)
    const records = res.data?.records || []
    if (page.value === 1) {
      list.value = records
    } else {
      list.value.push(...records)
    }
    if (records.length < 10 || page.value >= (res.data?.pages || 1)) {
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
.mine-report-page {
  min-height: 100vh;
  background: #f7f8fa;
}
</style>
