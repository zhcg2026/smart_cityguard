<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <span>全部待办</span>
      </template>
      <el-table v-loading="loading" :data="todoList" style="width: 100%">
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 'case' ? 'primary' : 'success'" size="small">
              {{ row.type === 'case' ? '案件' : '任务' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="140">
          <template #default="{ row }">
            <el-tag :type="getTodoStatusType(row.status)" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="截止时间" width="170">
          <template #default="{ row }">
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
      <el-empty v-if="!loading && todoList.length === 0" description="暂无待办" />
      <div class="pager">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { getCaseDashboardTodosPage } from '@/api/case'
import { formatDateTime } from '@/utils/dateFormat'

const router = useRouter()

const loading = ref(false)
const todoList = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

function getTodoStatusType(status) {
  if (!status) return 'info'
  if (status.includes('超时') || status.includes('紧急') || status.includes('回退')) return 'danger'
  if (status.includes('待') || status.includes('处置中') || status.includes('核查')) return 'warning'
  if (status.includes('已处置') || status.includes('把关')) return 'success'
  return 'info'
}

function handleTodo(row) {
  if (row.type === 'case') {
    router.push(`/case/detail/${row.id}?action=process`)
  } else if (row.taskType) {
    router.push(`/task/${row.taskType}/${row.id}`)
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await getCaseDashboardTodosPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    todoList.value = res.data?.records || []
    total.value = res.data?.total ?? 0
  } catch (error) {
    console.error('获取全部待办失败:', error)
    todoList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onRefreshLists() {
  pageNum.value = 1
  loadList()
}

onMounted(() => {
  loadList()
  window.addEventListener('cityguard:refresh-lists', onRefreshLists)
})

onBeforeUnmount(() => {
  window.removeEventListener('cityguard:refresh-lists', onRefreshLists)
})
</script>

<style scoped>
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
