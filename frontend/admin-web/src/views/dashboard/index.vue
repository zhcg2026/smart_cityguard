<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <div class="stat-section">
      <div class="stat-section-header">
        <span class="stat-section-title">案件统计</span>
        <el-radio-group v-model="statsPeriod" size="small" @change="loadStatistics">
          <el-radio-button label="day">日</el-radio-button>
          <el-radio-button label="week">周</el-radio-button>
          <el-radio-button label="month">月</el-radio-button>
          <el-radio-button label="year">年</el-radio-button>
        </el-radio-group>
      </div>
      <div class="stat-cards">
      <el-card
        v-for="item in statCards"
        :key="item.key"
        class="stat-card"
        shadow="hover"
        @click="goCaseList(item.key)"
      >
        <div class="stat-content">
          <div class="stat-icon" :class="item.iconClass">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics[item.field] }}</div>
            <div class="stat-label">{{ item.label }}</div>
          </div>
        </div>
      </el-card>
    </div>
    </div>

    <!-- 今日提示 & 公文通告 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="daily-tip-card">
          <template #header>
            <div class="header-with-action">
              <span>今日提示</span>
              <el-button type="primary" size="small" link @click="goTo('/notice/dailytip')">
                更多
              </el-button>
            </div>
          </template>
          <div
            v-for="tip in dailyTips"
            :key="tip.id"
            class="tip-item clickable"
            @click="openContentDetail(tip, 'dailytip')"
          >
            <el-icon><Bell /></el-icon>
            <span class="item-text">{{ tip.title || tip.content }}</span>
          </div>
          <el-empty v-if="dailyTips.length === 0" description="暂无提示" :image-size="48" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="announcement-card">
          <template #header>
            <div class="header-with-action">
              <span>公文通告</span>
              <el-button type="primary" size="small" link @click="goTo('/notice/announcement')">
                更多
              </el-button>
            </div>
          </template>
          <div
            v-for="item in announcements"
            :key="item.id"
            class="announcement-item clickable"
            @click="openContentDetail(item, 'announcement')"
          >
            <el-tag :type="item.announcementType === 'urgent' ? 'danger' : 'info'" size="small">
              {{ item.announcementType === 'urgent' ? '紧急' : item.announcementType === 'system' ? '系统' : '普通' }}
            </el-tag>
            <span class="title">{{ item.title }}</span>
            <span class="date">{{ formatPublishTime(item.publishTime) }}</span>
          </div>
          <el-empty v-if="announcements.length === 0" description="暂无通告" :image-size="48" />
        </el-card>
      </el-col>
    </el-row>

    <ContentDetailDialog
      v-model:visible="contentDetailVisible"
      :type="contentDetailType"
      :item="contentDetailItem"
    />

    <!-- 待办事项 -->
    <el-card class="todo-card">
      <template #header>
        <div class="header-with-action">
          <span>待办事项</span>
          <el-button type="primary" size="small" link @click="goPendingPage">
            全部待办
          </el-button>
        </div>
      </template>
      <el-table v-loading="todoLoading" :data="todoList" style="width: 100%">
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 'case' ? 'primary' : 'success'" size="small">
              {{ row.type === 'case' ? '案件' : '任务' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="getTodoStatusType(row.status)" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="阶段截止" width="170">
          <template #default="{ row }">
            <span v-if="row.timerStageName" class="todo-stage-name">{{ row.timerStageName }} </span>
            {{ formatDateTime(row.deadline) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleTodo(row)">
              处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!todoLoading && todoList.length === 0" description="暂无待办" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import {
  Clock,
  Loading,
  CircleCheck,
  Warning,
  CircleClose
} from '@element-plus/icons-vue'
import {
  getAnnouncementList,
  getDailyTip,
  getAnnouncementDetail,
  getDailyTipDetail
} from '@/api/config'
import { getCaseDashboardStats, getCaseDashboardTodos } from '@/api/case'
import { formatDateTime } from '@/utils/dateFormat'
import ContentDetailDialog from '@/components/ContentDetailDialog.vue'

const router = useRouter()

const statistics = reactive({
  pendingCases: 0,
  processingCases: 0,
  completedCases: 0,
  overdueCases: 0,
  cancelledCases: 0
})

const statsPeriod = ref('month')

const statCards = [
  { key: 'pending', field: 'pendingCases', label: '待处理案件', icon: Clock, iconClass: 'pending' },
  { key: 'processing', field: 'processingCases', label: '处理中案件', icon: Loading, iconClass: 'processing' },
  { key: 'completed', field: 'completedCases', label: '已完成案件', icon: CircleCheck, iconClass: 'completed' },
  { key: 'overdue', field: 'overdueCases', label: '超时案件', icon: Warning, iconClass: 'overdue' },
  { key: 'cancelled', field: 'cancelledCases', label: '作废案件', icon: CircleClose, iconClass: 'cancelled' }
]

const dailyTips = ref([])
const announcements = ref([])
const todoList = ref([])
const todoLoading = ref(false)
const contentDetailVisible = ref(false)
const contentDetailType = ref('announcement')
const contentDetailItem = ref(null)

function onRefreshLists() {
  loadStatistics()
  loadDailyTips()
  loadAnnouncements()
  loadTodoList()
}

onMounted(async () => {
  await loadStatistics()
  await loadDailyTips()
  await loadAnnouncements()
  await loadTodoList()
  window.addEventListener('cityguard:refresh-lists', onRefreshLists)
})

onBeforeUnmount(() => {
  window.removeEventListener('cityguard:refresh-lists', onRefreshLists)
})

async function loadStatistics() {
  try {
    const res = await getCaseDashboardStats({ period: statsPeriod.value })
    const data = res.data || {}
    statistics.pendingCases = data.pendingCases ?? 0
    statistics.processingCases = data.processingCases ?? 0
    statistics.completedCases = data.completedCases ?? 0
    statistics.overdueCases = data.overdueCases ?? 0
    statistics.cancelledCases = data.cancelledCases ?? 0
  } catch (error) {
    console.error('获取案件统计失败:', error)
  }
}

async function loadDailyTips() {
  try {
    const res = await getDailyTip({ limit: 5 })
    dailyTips.value = res.data || []
  } catch (error) {
    console.error('获取每日提示失败:', error)
  }
}

async function loadAnnouncements() {
  try {
    const res = await getAnnouncementList({ limit: 5 })
    announcements.value = res.data || []
  } catch (error) {
    console.error('获取通告失败:', error)
  }
}

async function loadTodoList() {
  todoLoading.value = true
  try {
    const res = await getCaseDashboardTodos({ limit: 10 })
    todoList.value = res.data?.items || []
  } catch (error) {
    console.error('获取待办事项失败:', error)
    todoList.value = []
  } finally {
    todoLoading.value = false
  }
}

function goTo(path) {
  router.push(path)
}

function goPendingPage() {
  router.push({ name: 'CaseDashboardTodos' })
}

function goCaseList(statGroup) {
  router.push({ path: '/case/list', query: { statGroup, period: statsPeriod.value } })
}

function formatPublishTime(value) {
  if (!value) return ''
  const text = String(value)
  return text.length >= 16 ? text.slice(0, 16).replace('T', ' ') : text
}

async function openContentDetail(row, type) {
  if (!row?.id) return
  contentDetailType.value = type
  try {
    const res = type === 'dailytip'
      ? await getDailyTipDetail(row.id)
      : await getAnnouncementDetail(row.id)
    contentDetailItem.value = res.data || row
  } catch {
    contentDetailItem.value = row
  }
  contentDetailVisible.value = true
}

function handleTodo(row) {
  if (row.type === 'case') {
    router.push(`/case/detail/${row.id}?action=process`)
  } else if (row.taskType) {
    router.push(`/task/${row.taskType}/${row.id}`)
  }
}

function getTodoStatusType(status) {
  if (!status) return 'info'
  if (status.includes('超时') || status.includes('紧急') || status.includes('回退')) return 'danger'
  if (status.includes('待') || status.includes('处置中') || status.includes('核查')) return 'warning'
  if (status.includes('已处置') || status.includes('把关')) return 'success'
  return 'info'
}
</script>

<style lang="scss" scoped>
.dashboard {
  .stat-section {
    margin-bottom: 20px;
  }

  .stat-section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
  }

  .stat-section-title {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
  }

  .stat-cards {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;

    .stat-card {
      flex: 1 1 180px;
      cursor: pointer;
      transition: transform 0.2s ease, box-shadow 0.2s ease;

      &:hover {
        transform: translateY(-2px);
      }

      .stat-content {
        display: flex;
        align-items: center;

        .stat-icon {
          width: 60px;
          height: 60px;
          display: flex;
          align-items: center;
          justify-content: center;
          border-radius: 8px;
          margin-right: 16px;

          .el-icon {
            font-size: 32px;
            color: #fff;
          }

          &.pending {
            background: linear-gradient(135deg, #f5af19, #f12711);
          }

          &.processing {
            background: linear-gradient(135deg, #667eea, #764ba2);
          }

          &.completed {
            background: linear-gradient(135deg, #11998e, #38ef7d);
          }

          &.overdue {
            background: linear-gradient(135deg, #ff416c, #ff4b2b);
          }

          &.cancelled {
            background: linear-gradient(135deg, #757f9a, #424242);
          }
        }

        .stat-info {
          .stat-value {
            font-size: 28px;
            font-weight: bold;
            color: #333;
          }

          .stat-label {
            font-size: 14px;
            color: #999;
          }
        }
      }
    }
  }

  .daily-tip-card, .announcement-card {
    margin-bottom: 20px;

    .tip-item, .announcement-item {
      padding: 8px 0;
      display: flex;
      align-items: center;
      min-height: 36px;

      &.clickable {
        cursor: pointer;
        border-radius: 4px;
        padding-left: 6px;
        padding-right: 6px;

        &:hover {
          background: #f5f7fa;
        }
      }

      .el-icon {
        margin-right: 8px;
        color: #409eff;
        flex-shrink: 0;
      }

      .item-text {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .announcement-item {
      .title {
        flex: 1;
        margin-left: 8px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .date {
        color: #999;
        font-size: 12px;
        flex-shrink: 0;
        margin-left: 8px;
      }
    }

    .header-with-action {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
  }

  .todo-card {
    margin-bottom: 20px;
  }
}
</style>
