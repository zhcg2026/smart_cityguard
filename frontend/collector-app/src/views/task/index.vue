<template>
  <div class="task-page">
    <van-nav-bar title="我的任务" />

    <van-tabs v-model:active="activeTab" sticky>
      <van-tab title="核查任务">
        <van-list
          v-model:loading="checkLoading"
          :finished="checkFinished"
          finished-text="没有更多了"
          @load="loadCheckTasks"
        >
          <van-cell-group inset>
            <van-cell
              v-for="task in checkTasks"
              :key="task.id"
              :title="task.caseNo || task.caseCode"
              :label="taskListLabel(task)"
              is-link
              @click="goCheckDetail(task.id)"
            >
              <template #icon>
                <van-tag type="warning">核查</van-tag>
              </template>
              <template #value>
                <van-tag v-if="task.taskStatus === 'pending'" type="primary">待办</van-tag>
                <van-tag v-else type="success">已完成</van-tag>
              </template>
            </van-cell>
          </van-cell-group>
          <van-empty v-if="checkTasks.length === 0 && checkFinished" description="暂无核查任务" />
        </van-list>
      </van-tab>

      <van-tab title="核实任务">
        <van-list
          v-model:loading="verifyLoading"
          :finished="verifyFinished"
          finished-text="没有更多了"
          @load="loadVerifyTasks"
        >
          <van-cell-group inset>
            <van-cell
              v-for="task in verifyTasks"
              :key="task.id"
              :title="task.caseNo || task.caseCode"
              :label="taskListLabel(task)"
              is-link
              @click="goVerifyDetail(task.id)"
            >
              <template #icon>
                <van-tag type="success">核实</van-tag>
              </template>
              <template #value>
                <van-tag v-if="task.taskStatus === 'pending'" type="primary">待办</van-tag>
                <van-tag v-else type="success">已完成</van-tag>
              </template>
            </van-cell>
          </van-cell-group>
          <van-empty v-if="verifyTasks.length === 0 && verifyFinished" description="暂无核实任务" />
        </van-list>
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup>
defineOptions({ name: 'Task' })
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getCheckTaskList, getVerifyTaskList } from '@/api/task'
import { buildTaskListLabel } from '@/utils/taskTimer'

const router = useRouter()
const route = useRoute()
const activeTab = ref(route.query.tab === 'verify' ? 1 : 0)

watch(() => route.query.tab, (val) => {
  activeTab.value = val === 'verify' ? 1 : 0
})

const checkTasks = ref([])
const checkLoading = ref(false)
const checkFinished = ref(false)
const checkPage = ref(1)

const verifyTasks = ref([])
const verifyLoading = ref(false)
const verifyFinished = ref(false)
const verifyPage = ref(1)

async function loadCheckTasks() {
  try {
    const res = await getCheckTaskList({ pageNum: checkPage.value, pageSize: 10, status: 0 })
    const page = res.data || {}
    const records = page.records || []
    checkTasks.value.push(...records)
    checkLoading.value = false
    const total = page.total ?? 0
    if (checkTasks.value.length >= total) {
      checkFinished.value = true
    } else {
      checkPage.value++
    }
  } catch {
    checkLoading.value = false
    checkFinished.value = true
  }
}

async function loadVerifyTasks() {
  try {
    const res = await getVerifyTaskList({ pageNum: verifyPage.value, pageSize: 10, status: 0 })
    const page = res.data || {}
    const records = page.records || []
    verifyTasks.value.push(...records)
    verifyLoading.value = false
    const total = page.total ?? 0
    if (verifyTasks.value.length >= total) {
      verifyFinished.value = true
    } else {
      verifyPage.value++
    }
  } catch {
    verifyLoading.value = false
    verifyFinished.value = true
  }
}

function taskListLabel(task) {
  return buildTaskListLabel(task)
}

function goCheckDetail(id) {
  router.push(`/task/check/${id}`)
}

function goVerifyDetail(id) {
  router.push(`/task/verify/${id}`)
}

function reloadTaskLists() {
  checkTasks.value = []
  verifyTasks.value = []
  checkPage.value = 1
  verifyPage.value = 1
  checkFinished.value = false
  verifyFinished.value = false
  checkLoading.value = true
  verifyLoading.value = true
  loadCheckTasks()
  loadVerifyTasks()
}

onMounted(() => {
  window.addEventListener('cityguard:refresh-lists', reloadTaskLists)
})

onBeforeUnmount(() => {
  window.removeEventListener('cityguard:refresh-lists', reloadTaskLists)
})
</script>

<style scoped>
.task-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}
</style>
