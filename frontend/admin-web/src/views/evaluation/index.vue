<template>
  <div class="evaluation-report">
    <el-card class="filter-card">
      <template #header><span>考核统计</span></template>
      <el-form :model="form" label-width="108px" class="filter-form">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :lg="8">
            <el-form-item label="上报时间">
              <div class="date-filter">
                <el-select v-model="form.reportTimeOp" clearable placeholder="条件" style="width: 96px">
                  <el-option v-for="o in dateOps" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
                <el-date-picker
                  v-if="form.reportTimeOp !== 'between'"
                  v-model="form.reportTimeStart"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="日期"
                  clearable
                  style="flex: 1"
                />
                <el-date-picker
                  v-else
                  v-model="form.reportTimeRange"
                  type="daterange"
                  value-format="YYYY-MM-DD"
                  range-separator="至"
                  start-placeholder="开始"
                  end-placeholder="结束"
                  style="flex: 1"
                />
              </div>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="8">
            <el-form-item label="处置截止时间">
              <div class="date-filter">
                <el-select v-model="form.deadlineTimeOp" clearable placeholder="条件" style="width: 96px">
                  <el-option v-for="o in dateOps" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
                <el-date-picker
                  v-if="form.deadlineTimeOp !== 'between'"
                  v-model="form.deadlineTimeStart"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="日期"
                  clearable
                  style="flex: 1"
                />
                <el-date-picker
                  v-else
                  v-model="form.deadlineTimeRange"
                  type="daterange"
                  value-format="YYYY-MM-DD"
                  range-separator="至"
                  start-placeholder="开始"
                  end-placeholder="结束"
                  style="flex: 1"
                />
              </div>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="8">
            <el-form-item label="结案时间">
              <div class="date-filter">
                <el-select v-model="form.closeTimeOp" clearable placeholder="条件" style="width: 96px">
                  <el-option v-for="o in dateOps" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
                <el-date-picker
                  v-if="form.closeTimeOp !== 'between'"
                  v-model="form.closeTimeStart"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="日期"
                  clearable
                  style="flex: 1"
                />
                <el-date-picker
                  v-else
                  v-model="form.closeTimeRange"
                  type="daterange"
                  value-format="YYYY-MM-DD"
                  range-separator="至"
                  start-placeholder="开始"
                  end-placeholder="结束"
                  style="flex: 1"
                />
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :lg="8">
            <el-form-item label="处置部门">
              <el-select v-model="form.handleDeptId" clearable filterable placeholder="筛选部门" style="width: 100%">
                <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="8">
            <el-form-item label="问题来源">
              <el-select v-model="form.sourceTypes" multiple collapse-tags clearable placeholder="多选" style="width: 100%">
                <el-option v-for="s in sourceOptions" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="8">
            <el-form-item label="问题状态">
              <el-select v-model="form.caseStatuses" multiple collapse-tags clearable placeholder="多选" style="width: 100%">
                <el-option v-for="(label, value) in statusOptions" :key="value" :label="label" :value="value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :lg="10">
            <el-form-item label="地址描述">
              <el-input v-model="form.address" placeholder="地址关键词" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="6">
            <el-form-item label="地址匹配">
              <el-select v-model="form.addressMatch" style="width: 100%">
                <el-option label="包含" value="contains" />
                <el-option label="等于" value="eq" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="10">
            <el-form-item label="问题描述">
              <el-input v-model="form.description" placeholder="描述关键词" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="6">
            <el-form-item label="描述匹配">
              <el-select v-model="form.descriptionMatch" style="width: 100%">
                <el-option label="包含" value="contains" />
                <el-option label="等于" value="eq" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadStatistics">
            <el-icon><Search /></el-icon>
            统计
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="viewMode === 'statistics'" v-loading="loading" class="table-card">
      <el-table :data="tableRows" border stripe style="width: 100%" max-height="560">
        <el-table-column prop="handleDeptName" label="处置部门" width="140" fixed />
        <el-table-column
          v-for="col in metricColumns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          width="108"
          align="center"
        >
          <template #default="{ row }">
            <span
              v-if="row.handleDeptId != null && row[col.prop] > 0"
              class="drill-link"
              @click="openDrill(row, col.metricKey, col.label)"
            >{{ row[col.prop] }}</span>
            <span v-else>{{ row[col.prop] ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="caseRatio" label="案件占比%" width="100" align="center" fixed="right">
          <template #default="{ row }">{{ formatRatio(row.caseRatio) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-else v-loading="drillLoading" class="table-card drill-list-card">
      <template #header>
        <div class="drill-header">
          <span>{{ drillTitle }}</span>
          <el-button type="primary" link @click="backToStatistics">返回统计表</el-button>
        </div>
      </template>
      <el-table :data="drillList" style="width: 100%" @row-click="goCaseDetail">
        <el-table-column prop="caseCode" label="任务号" width="150" />
        <el-table-column prop="smallName" label="小类" width="110" show-overflow-tooltip />
        <el-table-column prop="caseStatus" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getCaseStatusTagType(row.caseStatus)">{{ formatCaseStatusLabel(row.caseStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handleDeptName" label="处置部门" width="120" show-overflow-tooltip />
        <el-table-column prop="address" label="地址" min-width="160" show-overflow-tooltip />
        <el-table-column prop="reportTime" label="上报时间" width="165">
          <template #default="{ row }">{{ formatDateTime(row.reportTime) }}</template>
        </el-table-column>
        <el-table-column prop="deadlineTime" label="处置截止" width="165">
          <template #default="{ row }">{{ formatDateTime(row.deadlineTime) }}</template>
        </el-table-column>
        <el-table-column prop="closeTime" label="结案时间" width="165">
          <template #default="{ row }">{{ formatDateTime(row.closeTime) }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="drillPageNum"
        v-model:page-size="drillPageSize"
        class="pagination"
        :total="drillTotal"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadDrill"
        @current-change="loadDrill"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCaseReportStatistics, drillCaseReport } from '@/api/case'
import { getDeptTree } from '@/api/system'
import { formatDateTime } from '@/utils/dateFormat'
import { formatCaseStatusLabel, getCaseStatusTagType, CASE_STATUS_LABELS } from '@/utils/caseStatus'

const router = useRouter()

const dateOps = [
  { label: '等于', value: 'eq' },
  { label: '大于', value: 'gt' },
  { label: '小于', value: 'lt' },
  { label: '介于', value: 'between' }
]

const sourceOptions = [
  { label: '采集员上报', value: 'collector' },
  { label: '案件登记', value: 'register' },
  { label: '领导批示', value: 'leader' },
  { label: '部门批转', value: 'transfer' },
  { label: '公众举报', value: 'public' },
  { label: '视频上报', value: 'video' }
]

const statusOptions = CASE_STATUS_LABELS

const metricColumns = [
  { prop: 'registeredCount', label: '立案数', metricKey: 'registered' },
  { prop: 'dispatchCount', label: '派遣数', metricKey: 'dispatch' },
  { prop: 'shouldHandleCount', label: '应处置数', metricKey: 'shouldHandle' },
  { prop: 'handledCount', label: '处置数', metricKey: 'handled' },
  { prop: 'pendingHandleCount', label: '待处置数', metricKey: 'pendingHandle' },
  { prop: 'onTimePendingCount', label: '按时待处置', metricKey: 'onTimePending' },
  { prop: 'overduePendingCount', label: '超时待处置', metricKey: 'overduePending' },
  { prop: 'onTimeHandleCount', label: '按时处置', metricKey: 'onTimeHandle' },
  { prop: 'overdueHandleCount', label: '超时处置', metricKey: 'overdueHandle' },
  { prop: 'extensionCount', label: '延期数', metricKey: 'extension' },
  { prop: 'suspendCount', label: '挂账数', metricKey: 'suspend' },
  { prop: 'reworkCount', label: '返工数', metricKey: 'rework' },
  { prop: 'shouldCloseCount', label: '应结案数', metricKey: 'shouldClose' },
  { prop: 'onTimeCloseCount', label: '按时结案', metricKey: 'onTimeClose' },
  { prop: 'closedCount', label: '结案数', metricKey: 'closed' },
  { prop: 'overdueCloseCount', label: '超时结案', metricKey: 'overdueClose' },
  { prop: 'appealOnTimeCloseCount', label: '申诉后按时结案', metricKey: 'appealOnTimeClose' },
  { prop: 'appealOverdueCloseCount', label: '申诉后超时结案', metricKey: 'appealOverdueClose' }
]

const loading = ref(false)
const statRows = ref([])
const totalRow = ref(null)
const deptOptions = ref([])

const defaultForm = () => ({
  reportTimeOp: '',
  reportTimeStart: '',
  reportTimeRange: [],
  deadlineTimeOp: '',
  deadlineTimeStart: '',
  deadlineTimeRange: [],
  closeTimeOp: '',
  closeTimeStart: '',
  closeTimeRange: [],
  handleDeptId: null,
  sourceTypes: [],
  caseStatuses: [],
  address: '',
  addressMatch: 'contains',
  description: '',
  descriptionMatch: 'contains'
})

const form = reactive(defaultForm())

const tableRows = computed(() => {
  const list = [...statRows.value]
  if (totalRow.value) {
    list.push({ ...totalRow.value, handleDeptId: null })
  }
  return list
})

const viewMode = ref('statistics')
const drillLoading = ref(false)
const drillTitle = ref('')
const drillList = ref([])
const drillTotal = ref(0)
const drillPageNum = ref(1)
const drillPageSize = ref(10)
const drillContext = reactive({ metricKey: '', handleDeptId: null, deptName: '' })

function buildDateFilter(op, start, range) {
  if (!op) return undefined
  if (op === 'between') {
    if (!range?.length || range.length < 2) return undefined
    return { op, start: range[0], end: range[1] }
  }
  if (!start) return undefined
  return { op, start }
}

function buildCriteria(extra = {}) {
  return {
    reportTime: buildDateFilter(form.reportTimeOp, form.reportTimeStart, form.reportTimeRange),
    closeTime: buildDateFilter(form.closeTimeOp, form.closeTimeStart, form.closeTimeRange),
    deadlineTime: buildDateFilter(form.deadlineTimeOp, form.deadlineTimeStart, form.deadlineTimeRange),
    sourceTypes: form.sourceTypes?.length ? form.sourceTypes : undefined,
    caseStatuses: form.caseStatuses?.length ? form.caseStatuses : undefined,
    handleDeptId: form.handleDeptId || undefined,
    address: form.address?.trim() || undefined,
    addressMatch: form.addressMatch,
    description: form.description?.trim() || undefined,
    descriptionMatch: form.descriptionMatch,
    ...extra
  }
}

function flattenDept(nodes, out = []) {
  if (!nodes) return out
  for (const n of nodes) {
    if (n.id != null) out.push({ id: n.id, deptName: n.deptName })
    if (n.children?.length) flattenDept(n.children, out)
  }
  return out
}

async function loadDeptOptions() {
  try {
    const res = await getDeptTree()
    deptOptions.value = flattenDept(res.data || [])
  } catch (e) {
    console.warn(e)
  }
}

async function loadStatistics() {
  viewMode.value = 'statistics'
  loading.value = true
  try {
    const res = await getCaseReportStatistics(buildCriteria())
    statRows.value = res.data?.rows || []
    totalRow.value = res.data?.totalRow || null
    if (!statRows.value.length) {
      ElMessage.info('无符合条件的数据')
    }
  } catch (e) {
    ElMessage.error(e.message || '统计失败')
    statRows.value = []
    totalRow.value = null
  } finally {
    loading.value = false
  }
}

function handleReset() {
  Object.assign(form, defaultForm())
  statRows.value = []
  totalRow.value = null
  viewMode.value = 'statistics'
  drillList.value = []
  drillTotal.value = 0
}

function formatRatio(v) {
  if (v == null || v === '') return '0.00'
  return typeof v === 'number' ? v.toFixed(2) : String(v)
}

function openDrill(row, metricKey, label) {
  if (row.handleDeptId == null) return
  drillContext.metricKey = metricKey
  drillContext.handleDeptId = row.handleDeptId
  drillContext.deptName = row.handleDeptName
  drillTitle.value = `${row.handleDeptName} · ${label}（案件列表）`
  drillPageNum.value = 1
  viewMode.value = 'drill'
  loadDrill()
  nextTick(() => {
    document.querySelector('.drill-list-card')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

function backToStatistics() {
  viewMode.value = 'statistics'
  drillList.value = []
  drillTotal.value = 0
}

async function loadDrill() {
  drillLoading.value = true
  try {
    const res = await drillCaseReport(
      buildCriteria({
        metricKey: drillContext.metricKey,
        drillHandleDeptId: drillContext.handleDeptId,
        pageNum: drillPageNum.value,
        pageSize: drillPageSize.value
      })
    )
    const page = res.data || {}
    drillList.value = page.records || []
    drillTotal.value = page.total ?? 0
  } catch (e) {
    ElMessage.error(e.message || '反查失败')
    drillList.value = []
    drillTotal.value = 0
  } finally {
    drillLoading.value = false
  }
}

function goCaseDetail(row) {
  if (!row?.id) return
  router.push(`/case/detail/${row.id}`)
}

loadDeptOptions()
</script>

<style scoped>
.evaluation-report .filter-card {
  margin-bottom: 16px;
}
.date-filter {
  display: flex;
  gap: 8px;
  width: 100%;
}
.filter-form :deep(.el-form-item) {
  margin-bottom: 12px;
}
.table-card {
  overflow-x: auto;
}
.drill-link {
  color: var(--el-color-primary);
  cursor: pointer;
  text-decoration: underline;
}
.drill-link:hover {
  opacity: 0.85;
}
.drill-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
.drill-list-card :deep(.el-table__row) {
  cursor: pointer;
}
</style>
