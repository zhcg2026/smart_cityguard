<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon pending">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.pendingCases }}</div>
              <div class="stat-label">待处理案件</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon processing">
              <el-icon><Loading /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.processingCases }}</div>
              <div class="stat-label">处理中案件</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon completed">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.completedCases }}</div>
              <div class="stat-label">已完成案件</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon overdue">
              <el-icon><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.overdueCases }}</div>
              <div class="stat-label">超时案件</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <el-card class="quick-actions">
      <template #header>
        <span>快捷操作</span>
      </template>
      <el-row :gutter="20">
        <el-col :span="4">
          <el-button type="primary" @click="goTo('/case/pending')">
            <el-icon><Document /></el-icon>
            案件登记
          </el-button>
        </el-col>
        <el-col :span="4">
          <el-button type="success" @click="goTo('/task/verify')">
            <el-icon><Search /></el-icon>
            核查任务
          </el-button>
        </el-col>
        <el-col :span="4">
          <el-button type="warning" @click="goTo('/task/check')">
            <el-icon><Finished /></el-icon>
            核实任务
          </el-button>
        </el-col>
        <el-col :span="4">
          <el-button type="info" @click="goTo('/case/list')">
            <el-icon><List /></el-icon>
            案件列表
          </el-button>
        </el-col>
        <el-col :span="4">
          <el-button @click="goTo('/appeal/list')">
            <el-icon><ChatDotSquare /></el-icon>
            申诉处理
          </el-button>
        </el-col>
        <el-col :span="4">
          <el-button @click="goTo('/evaluation/index')">
            <el-icon><DataAnalysis /></el-icon>
            考核统计
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 今日提示 & 公文通告 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="daily-tip-card">
          <template #header>
            <span>今日提示</span>
          </template>
          <el-scrollbar height="200px">
            <div v-for="tip in dailyTips" :key="tip.id" class="tip-item">
              <el-icon><Bell /></el-icon>
              <span>{{ tip.content }}</span>
            </div>
            <el-empty v-if="dailyTips.length === 0" description="暂无提示" />
          </el-scrollbar>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="announcement-card">
          <template #header>
            <div class="header-with-action">
              <span>公文通告</span>
              <el-button type="primary" size="small" @click="goTo('/config/announcement')">
                更多
              </el-button>
            </div>
          </template>
          <el-scrollbar height="200px">
            <div v-for="item in announcements" :key="item.id" class="announcement-item">
              <el-tag :type="item.type === 'urgent' ? 'danger' : 'info'" size="small">
                {{ item.type === 'urgent' ? '紧急' : '普通' }}
              </el-tag>
              <span class="title">{{ item.title }}</span>
              <span class="date">{{ item.publishTime }}</span>
            </div>
            <el-empty v-if="announcements.length === 0" description="暂无通告" />
          </el-scrollbar>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待办事项 -->
    <el-card class="todo-card">
      <template #header>
        <span>待办事项</span>
      </template>
      <el-table :data="todoList" style="width: 100%">
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 'case' ? 'primary' : 'success'">
              {{ row.type === 'case' ? '案件' : '任务' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deadline" label="截止时间" width="180" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleTodo(row)">
              处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAnnouncementList, getDailyTip } from '@/api/config'

const router = useRouter()

const statistics = reactive({
  pendingCases: 0,
  processingCases: 0,
  completedCases: 0,
  overdueCases: 0
})

const dailyTips = ref([])
const announcements = ref([])
const todoList = ref([])

onMounted(async () => {
  await loadStatistics()
  await loadDailyTips()
  await loadAnnouncements()
  await loadTodoList()
})

async function loadStatistics() {
  // TODO: 调用API获取统计数据
  statistics.pendingCases = 12
  statistics.processingCases = 28
  statistics.completedCases = 156
  statistics.overdueCases = 3
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
    announcements.value = res.data?.list || []
  } catch (error) {
    console.error('获取通告失败:', error)
  }
}

async function loadTodoList() {
  // TODO: 调用API获取待办事项
  todoList.value = []
}

function goTo(path) {
  router.push(path)
}

function handleTodo(row) {
  if (row.type === 'case') {
    router.push(`/case/detail/${row.id}`)
  } else {
    router.push(`/task/${row.taskType}/${row.id}`)
  }
}

function getStatusType(status) {
  const statusMap = {
    '待处理': 'warning',
    '进行中': 'primary',
    '已完成': 'success',
    '超时': 'danger'
  }
  return statusMap[status] || 'info'
}
</script>

<style lang="scss" scoped>
.dashboard {
  .stat-cards {
    margin-bottom: 20px;

    .stat-card {
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

  .quick-actions {
    margin-bottom: 20px;

    .el-button {
      width: 100%;
    }
  }

  .daily-tip-card, .announcement-card {
    margin-bottom: 20px;

    .tip-item, .announcement-item {
      padding: 10px 0;
      display: flex;
      align-items: center;

      .el-icon {
        margin-right: 8px;
        color: #409eff;
      }
    }

    .announcement-item {
      .title {
        flex: 1;
        margin-left: 8px;
      }

      .date {
        color: #999;
        font-size: 12px;
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