<template>
  <div class="task-ledger">
    <el-card class="hint-card">
      <p class="page-hint">
        立案前<strong>核查</strong>、结案前<strong>核实</strong>为可选支线；采集员在移动端执行，受理员在「案件管理」待核实/待核查/待我结案中推进案件。
        本页供值班长、管理员<strong>督办查询</strong>，不可在此代执行。
      </p>
    </el-card>

    <el-card>
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="核查任务" name="check" />
        <el-tab-pane label="核实任务" name="verify" />
      </el-tabs>

      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="任务状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option
              v-for="opt in TASK_STATUS_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="案件编号">
          <el-input v-model="searchForm.caseCode" placeholder="模糊匹配" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="taskList" stripe style="width: 100%">
        <el-table-column prop="taskCode" label="任务编号" width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.taskCode || row.taskNo || '--' }}</template>
        </el-table-column>
        <el-table-column prop="caseCode" label="关联案件" width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button v-if="row.caseId" type="primary" link @click="goCase(row.caseId)">
              {{ row.caseCode || row.caseNo || row.caseId }}
            </el-button>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column prop="smallName" label="小类" width="120" show-overflow-tooltip />
        <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
        <el-table-column prop="collectorName" label="采集员" width="100" />
        <el-table-column label="下发人" width="100">
          <template #default="{ row }">{{ row.assignerName || row.creatorName || '--' }}</template>
        </el-table-column>
        <el-table-column label="指派时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.assignTime || row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="截止时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.deadlineTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="taskStatusTagType(row.taskStatus)" size="small">
              {{ taskStatusLabel(row.taskStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="110">
          <template #default="{ row }">
            {{ activeTab === 'check' ? checkResultLabel(row.checkResult) : verifyResultLabel(row.verifyResult) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">任务详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        class="pagination"
        layout="total, sizes, prev, pager, next"
        @size-change="loadList"
        @current-change="loadList"
      />
    </el-card>

    <TaskLedgerDetailDialog
      v-model:visible="detailVisible"
      :task-kind="activeTab"
      :task-id="detailTaskId"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCheckTaskList, getVerifyTaskList } from '@/api/task'
import { formatDateTime } from '@/utils/dateFormat'
import {
  TASK_STATUS_OPTIONS,
  taskStatusLabel,
  taskStatusTagType,
  checkResultLabel,
  verifyResultLabel
} from '@/utils/taskLedger'
import TaskLedgerDetailDialog from '@/components/TaskLedgerDetailDialog.vue'

const route = useRoute()
const router = useRouter()

const activeTab = ref(route.query.tab === 'verify' ? 'verify' : 'check')
const loading = ref(false)
const taskList = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  status: '',
  caseCode: ''
})

const detailVisible = ref(false)
const detailTaskId = ref(null)

function syncRouteTab() {
  const q = { ...route.query, tab: activeTab.value }
  router.replace({ path: route.path, query: q })
}

function onTabChange() {
  pageNum.value = 1
  searchForm.status = ''
  searchForm.caseCode = ''
  syncRouteTab()
  loadList()
}

async function loadList() {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      allScope: true
    }
    if (searchForm.status) params.status = searchForm.status
    const code = (searchForm.caseCode || '').trim()
    if (code) params.caseCode = code
    const fetcher = activeTab.value === 'check' ? getCheckTaskList : getVerifyTaskList
    const res = await fetcher(params)
    taskList.value = res.data?.records || []
    total.value = res.data?.total ?? 0
  } catch (e) {
    console.error(e)
    taskList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadList()
}

function handleReset() {
  searchForm.status = ''
  searchForm.caseCode = ''
  pageNum.value = 1
  loadList()
}

function openDetail(row) {
  detailTaskId.value = row.id
  detailVisible.value = true
}

function goCase(caseId) {
  router.push(`/case/detail/${caseId}`)
}

watch(
  () => route.query.tab,
  (tab) => {
    const next = tab === 'verify' ? 'verify' : 'check'
    if (next !== activeTab.value) {
      activeTab.value = next
      pageNum.value = 1
      loadList()
    }
  }
)

onMounted(() => {
  loadList()
})
</script>

<style lang="scss" scoped>
.task-ledger {
  .hint-card {
    margin-bottom: 16px;

    .page-hint {
      margin: 0;
      font-size: 13px;
      color: #606266;
      line-height: 1.7;
    }
  }

  .search-form {
    margin-bottom: 12px;
  }

  .pagination {
    margin-top: 16px;
    justify-content: flex-end;
  }
}
</style>
