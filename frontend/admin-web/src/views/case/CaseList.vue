<template>
  <div class="case-list">
    <el-alert
      v-if="statGroupLabel"
      class="stat-group-alert"
      :title="statFilterAlertTitle"
      type="info"
      show-icon
      closable
      @close="clearStatGroup"
    />
    <!-- 搜索表单 -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="案件编号">
          <el-input v-model="searchForm.caseCode" placeholder="请输入案件编号" clearable />
        </el-form-item>
        <el-form-item label="案件状态">
          <el-select v-model="searchForm.caseStatus" placeholder="请选择状态" clearable>
            <el-option label="已上报" value="reported" />
            <el-option label="待核实" value="pending_verify" />
            <el-option label="待立案" value="pending_register" />
            <el-option label="待派遣" value="pending_dispatch" />
            <el-option label="待指派" value="pending_handle" />
            <el-option label="处置中" value="handling" />
            <el-option label="处置人员已处置" value="handle_finish" />
            <el-option label="待核查" value="pending_check" />
            <el-option label="已结案" value="closed" />
            <el-option label="部门回退" value="returned" />
            <el-option label="作废" value="not_accepted" />
          </el-select>
        </el-form-item>
        <el-form-item label="案件大类">
          <el-select v-model="searchForm.categoryBigId" placeholder="请选择大类" clearable>
            <el-option v-for="item in categoryBigList" :key="item.id" :label="item.bigName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="上报时间">
          <el-date-picker
            v-model="searchForm.reportTime"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 案件列表 -->
    <el-card class="list-card">
      <div v-if="isAdmin" class="table-toolbar">
        <el-button
          type="danger"
          :disabled="selectedRows.length === 0"
          @click="handleBatchDelete"
        >
          批量删除
        </el-button>
        <span v-if="selectedRows.length > 0" class="selection-hint">已选 {{ selectedRows.length }} 条</span>
      </div>
      <el-table
        v-loading="loading"
        :data="caseList"
        style="width: 100%"
        @row-click="handleRowClick"
        @selection-change="handleSelectionChange"
      >
        <el-table-column v-if="isAdmin" type="selection" width="48" />
        <el-table-column prop="caseCode" label="案件编号" width="150" />
        <el-table-column prop="bigName" label="大类" width="100" />
        <el-table-column prop="smallName" label="小类" width="120" />
        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="caseStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getCaseStatusTagType(row.caseStatus)">
              {{ formatCaseStatusLabel(row.caseStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reportTime" label="上报时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.reportTime) }}</template>
        </el-table-column>
        <el-table-column prop="reporterName" label="上报人" width="100" />
        <el-table-column prop="timerStageName" label="计时阶段" width="88">
          <template #default="{ row }">
            {{ row.timerStageName || '--' }}
          </template>
        </el-table-column>
        <el-table-column prop="timeRemaining" label="剩余时限" width="110">
          <template #default="{ row }">
            <span :class="{ overdue: isRowStageOverdue(row) }">
              {{ row.timeRemaining || '--' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="阶段截止" width="170">
          <template #default="{ row }">
            <span :class="{ overdue: isRowStageOverdue(row) }">
              {{ formatDateTime(rowStageDeadline(row)) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="isAdmin ? 140 : 100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click.stop="goDetail(row.id)">
              查看
            </el-button>
            <el-button
              v-if="isAdmin"
              type="danger"
              size="small"
              link
              @click.stop="handleDeleteOne(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCaseList, deleteCases } from '@/api/case'
import { getCategoryBigList as getCategoryList } from '@/api/config'
import { useUserStore } from '@/stores/user'
import { RoleCode } from '@/utils/roleAccess'
import { formatDateTime } from '@/utils/dateFormat'
import { isRowStageOverdue, rowStageDeadline } from '@/utils/caseTimer'
import { formatCaseStatusLabel, getCaseStatusTagType } from '@/utils/caseStatus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const STAT_GROUP_LABELS = {
  pending: '待处理案件',
  processing: '处置中案件',
  completed: '已完成案件',
  overdue: '超时案件',
  cancelled: '作废案件'
}

const STAT_PERIOD_LABELS = {
  day: '今日',
  week: '本周',
  month: '本月',
  year: '本年'
}

const statGroup = computed(() => {
  const g = route.query.statGroup
  return typeof g === 'string' ? g : ''
})

const dashboardPeriod = computed(() => {
  const p = route.query.period
  if (typeof p !== 'string') return ''
  return ['day', 'week', 'month', 'year'].includes(p) ? p : ''
})

const statGroupLabel = computed(() => STAT_GROUP_LABELS[statGroup.value] || '')

const statFilterAlertTitle = computed(() => {
  if (!statGroupLabel.value) return ''
  const periodLabel = STAT_PERIOD_LABELS[dashboardPeriod.value]
  return periodLabel
    ? `当前筛选：${periodLabel}${statGroupLabel.value}`
    : `当前筛选：${statGroupLabel.value}`
})

const isAdmin = computed(() => (userStore.roles || []).includes(RoleCode.ADMIN))

const loading = ref(false)
const caseList = ref([])
const selectedRows = ref([])
const categoryBigList = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  caseCode: '',
  caseStatus: '',
  categoryBigId: '',
  reportTime: []
})

function formatDateParam(d) {
  if (!d) return undefined
  if (typeof d === 'string') return d.length >= 10 ? d.slice(0, 10) : d
  const dt = new Date(d)
  if (Number.isNaN(dt.getTime())) return undefined
  const y = dt.getFullYear()
  const m = String(dt.getMonth() + 1).padStart(2, '0')
  const day = String(dt.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function onRefreshLists() {
  loadCaseList()
}

onMounted(async () => {
  await loadCategoryBigList()
  await loadCaseList()
  window.addEventListener('cityguard:refresh-lists', onRefreshLists)
})

watch([statGroup, dashboardPeriod], () => {
  pageNum.value = 1
  loadCaseList()
})

onBeforeUnmount(() => {
  window.removeEventListener('cityguard:refresh-lists', onRefreshLists)
})

async function loadCategoryBigList() {
  try {
    const res = await getCategoryList()
    categoryBigList.value = res.data || []
  } catch (error) {
    console.error('获取大类列表失败:', error)
  }
}

async function loadCaseList() {
  loading.value = true
  try {
    const useDashboardPeriod = statGroup.value && dashboardPeriod.value
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      caseCode: searchForm.caseCode || undefined,
      caseStatus: statGroup.value ? undefined : (searchForm.caseStatus || undefined),
      statGroup: statGroup.value || undefined,
      period: useDashboardPeriod ? dashboardPeriod.value : undefined,
      categoryBigId: searchForm.categoryBigId || undefined,
      startTime: useDashboardPeriod ? undefined : formatDateParam(searchForm.reportTime?.[0]),
      endTime: useDashboardPeriod ? undefined : formatDateParam(searchForm.reportTime?.[1])
    }
    const res = await getCaseList(params)
    caseList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取案件列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadCaseList()
}

function handleReset() {
  searchForm.caseCode = ''
  searchForm.caseStatus = ''
  searchForm.categoryBigId = ''
  searchForm.reportTime = []
  pageNum.value = 1
  if (statGroup.value) {
    clearStatGroup()
    return
  }
  loadCaseList()
}

function clearStatGroup() {
  router.replace({ path: '/case/list', query: {} })
}

function handleSizeChange(size) {
  pageSize.value = size
  loadCaseList()
}

function handleCurrentChange(page) {
  pageNum.value = page
  loadCaseList()
}

function handleRowClick(row) {
  goDetail(row.id)
}

function goDetail(id) {
  router.push(`/case/detail/${id}`)
}

function handleSelectionChange(rows) {
  selectedRows.value = rows || []
}

async function confirmAndDelete(ids) {
  const list = [...new Set(ids.filter((id) => id != null))]
  if (list.length === 0) {
    ElMessage.warning('请选择要删除的案件')
    return
  }
  const label =
    list.length === 1
      ? `确定删除案件（ID: ${list[0]}）？删除后不可在列表中恢复。`
      : `确定删除选中的 ${list.length} 条案件？删除后不可在列表中恢复。`
  await ElMessageBox.confirm(label, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  const res = await deleteCases(list)
  const n = res.data ?? list.length
  ElMessage.success(`已删除 ${n} 条案件`)
  selectedRows.value = []
  await loadCaseList()
}

async function handleDeleteOne(row) {
  try {
    await confirmAndDelete([row.id])
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除案件失败:', e)
    }
  }
}

async function handleBatchDelete() {
  try {
    await confirmAndDelete(selectedRows.value.map((r) => r.id))
  } catch (e) {
    if (e !== 'cancel') {
      console.error('批量删除失败:', e)
    }
  }
}

</script>

<style lang="scss" scoped>
.case-list {
  .stat-group-alert {
    margin-bottom: 16px;
  }

  .search-card {
    margin-bottom: 20px;

    .el-form-item {
      margin-bottom: 0;
    }
  }

  .list-card {
    .table-toolbar {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;

      .selection-hint {
        font-size: 13px;
        color: #909399;
      }
    }

    .el-pagination {
      margin-top: 20px;
      justify-content: flex-end;
    }

    .overdue {
      color: #f56c6c;
    }
  }
}
</style>