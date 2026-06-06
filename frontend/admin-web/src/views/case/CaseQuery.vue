<template>
  <div class="case-query">
    <el-card class="search-card">
      <template #header>
        <span>综合查询</span>
      </template>
      <el-form :model="form" label-width="96px" class="query-form">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :lg="8">
            <el-form-item label="任务号">
              <el-input v-model="form.caseCode" placeholder="案件编号" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="8">
            <el-form-item label="编号匹配">
              <el-select v-model="form.caseCodeMatch" style="width: 100%">
                <el-option label="精确" value="exact" />
                <el-option label="前缀" value="prefix" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="8">
            <el-form-item label="问题状态">
              <el-select v-model="form.caseStatuses" multiple collapse-tags clearable placeholder="多选" style="width: 100%">
                <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

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
          <el-col :xs="24" :sm="12" :lg="8">
            <el-form-item label="问题来源">
              <el-select v-model="form.caseOrigins" multiple collapse-tags clearable placeholder="多选" style="width: 100%">
                <el-option v-for="s in originOptions" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :lg="6">
            <el-form-item label="事部件类型">
              <el-select
                v-model="form.categoryType"
                clearable
                placeholder="部件/事件"
                style="width: 100%"
                @change="onCategoryTypeChange"
              >
                <el-option label="部件" value="component" />
                <el-option label="事件" value="event" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="6">
            <el-form-item label="大类">
              <el-select
                v-model="form.categoryBigId"
                clearable
                filterable
                placeholder="先选类型"
                :disabled="!form.categoryType"
                style="width: 100%"
                @change="onCategoryBigChange"
              >
                <el-option
                  v-for="b in bigOptions"
                  :key="b.id"
                  :label="`${b.bigCode || ''} ${b.bigName}`"
                  :value="b.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="6">
            <el-form-item label="问题小类">
              <el-select
                v-model="form.smallIds"
                multiple
                filterable
                collapse-tags
                clearable
                placeholder="先选大类"
                :disabled="!form.categoryBigId"
                style="width: 100%"
              >
                <el-option
                  v-for="s in smallOptions"
                  :key="s.id"
                  :label="`${s.smallCode || ''} ${s.smallName}`"
                  :value="s.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="6">
            <el-form-item label="责任网格">
              <el-select v-model="form.respGridIds" multiple collapse-tags clearable placeholder="多选" style="width: 100%">
                <el-option v-for="g in respGridOptions" :key="g.id" :label="g.respGridName" :value="g.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :lg="8">
            <el-form-item label="处置部门">
              <el-select v-model="form.handleDeptId" clearable filterable placeholder="请选择" style="width: 100%">
                <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="8">
            <el-form-item label="采集员">
              <el-select v-model="form.reporterId" clearable filterable placeholder="上报人" style="width: 100%">
                <el-option v-for="u in collectorOptions" :key="u.id" :label="userLabel(u)" :value="u.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="8">
            <el-form-item label="受理员">
              <el-select v-model="form.registerOperatorId" clearable filterable placeholder="立案人" style="width: 100%">
                <el-option v-for="u in acceptorOptions" :key="u.id" :label="userLabel(u)" :value="u.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="8">
            <el-form-item label="派遣员">
              <el-select v-model="form.dispatchOperatorId" clearable filterable placeholder="派遣人" style="width: 100%">
                <el-option v-for="u in dispatcherOptions" :key="u.id" :label="userLabel(u)" :value="u.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
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
          <el-col :xs="24" :sm="24" :lg="8">
            <el-form-item label-width="0">
              <el-button type="primary" :loading="loading" @click="handleSearch">
                <el-icon><Search /></el-icon>
                查询
              </el-button>
              <el-button @click="handleReset">
                <el-icon><Refresh /></el-icon>
                重置
              </el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card class="list-card">
      <el-table v-loading="loading" :data="caseList" style="width: 100%" @row-click="openDetail">
        <el-table-column prop="caseCode" label="任务号" width="150" />
        <el-table-column prop="smallName" label="小类" width="120" show-overflow-tooltip />
        <el-table-column prop="caseStatus" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getCaseStatusTagType(row.caseStatus)">
              {{ formatCaseStatusLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sourceType" label="来源" width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ caseOriginLabelFromRow(row) }}</template>
        </el-table-column>
        <el-table-column prop="description" label="问题描述" min-width="140" show-overflow-tooltip />
        <el-table-column prop="respGridName" label="责任网格" width="100" show-overflow-tooltip />
        <el-table-column prop="handleDeptName" label="处置部门" width="120" show-overflow-tooltip />
        <el-table-column prop="reporterName" label="采集员" width="90" />
        <el-table-column prop="registerOperatorName" label="受理员" width="90" />
        <el-table-column prop="dispatchOperatorName" label="派遣员" width="90" />
        <el-table-column prop="address" label="地址" min-width="160" show-overflow-tooltip />
        <el-table-column prop="reportTime" label="上报时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.reportTime) }}</template>
        </el-table-column>
        <el-table-column prop="closeTime" label="结案时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.closeTime) }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        class="pagination"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="runQuery"
        @current-change="runQuery"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { queryCases } from '@/api/case'
import { getCategoryBigList, getCategorySmallList } from '@/api/config'
import { getRespGridList } from '@/api/geo'
import { getUserList, getDeptTree } from '@/api/system'
import { RoleCode } from '@/utils/roleAccess'
import { formatDateTime } from '@/utils/dateFormat'
import { formatCaseStatusLabel, getCaseStatusTagType } from '@/utils/caseStatus'
import {
  CASE_QUERY_STATUS_OPTIONS,
  CASE_QUERY_ORIGIN_OPTIONS,
  expandCaseStatusesForQuery,
  caseOriginLabelFromRow,
  categoryApiType
} from '@/utils/caseQuery'

const router = useRouter()

const dateOps = [
  { label: '等于', value: 'eq' },
  { label: '大于', value: 'gt' },
  { label: '小于', value: 'lt' },
  { label: '介于', value: 'between' }
]

const originOptions = CASE_QUERY_ORIGIN_OPTIONS
const statusOptions = CASE_QUERY_STATUS_OPTIONS

const loading = ref(false)
const caseList = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const bigOptions = ref([])
const smallOptions = ref([])
const respGridOptions = ref([])
const deptOptions = ref([])
const collectorOptions = ref([])
const acceptorOptions = ref([])
const dispatcherOptions = ref([])

const defaultForm = () => ({
  caseCode: '',
  caseCodeMatch: 'exact',
  reportTimeOp: '',
  reportTimeStart: '',
  reportTimeRange: [],
  closeTimeOp: '',
  closeTimeStart: '',
  closeTimeRange: [],
  caseOrigins: [],
  respGridIds: [],
  caseStatuses: [],
  categoryType: '',
  categoryBigId: null,
  smallIds: [],
  description: '',
  descriptionMatch: 'contains',
  handleDeptId: null,
  reporterId: null,
  registerOperatorId: null,
  dispatchOperatorId: null,
  address: '',
  addressMatch: 'contains'
})

const form = reactive(defaultForm())

function userLabel(u) {
  return u.realName || u.username || String(u.id)
}

async function onCategoryTypeChange() {
  form.categoryBigId = null
  form.smallIds = []
  bigOptions.value = []
  smallOptions.value = []
  if (!form.categoryType) return
  try {
    const res = await getCategoryBigList({ type: categoryApiType(form.categoryType) })
    bigOptions.value = res.data || []
  } catch (e) {
    console.warn(e)
    bigOptions.value = []
  }
}

async function onCategoryBigChange(bigId) {
  form.smallIds = []
  smallOptions.value = []
  if (!bigId) return
  try {
    const res = await getCategorySmallList(bigId)
    smallOptions.value = res.data || []
  } catch (e) {
    console.warn(e)
    smallOptions.value = []
  }
}

function buildDateFilter(op, start, range) {
  if (!op) return undefined
  if (op === 'between') {
    if (!range?.length || range.length < 2) return undefined
    return { op, start: range[0], end: range[1] }
  }
  if (!start) return undefined
  return { op, start }
}

function buildCriteria() {
  const criteria = {
    caseCode: form.caseCode?.trim() || undefined,
    caseCodeMatch: form.caseCodeMatch,
    reportTime: buildDateFilter(form.reportTimeOp, form.reportTimeStart, form.reportTimeRange),
    closeTime: buildDateFilter(form.closeTimeOp, form.closeTimeStart, form.closeTimeRange),
    caseOrigins: form.caseOrigins?.length ? form.caseOrigins : undefined,
    respGridIds: form.respGridIds?.length ? form.respGridIds : undefined,
    caseStatuses: expandCaseStatusesForQuery(form.caseStatuses),
    categoryType: form.categoryType || undefined,
    smallIds: form.smallIds?.length ? form.smallIds : undefined,
    description: form.description?.trim() || undefined,
    descriptionMatch: form.descriptionMatch,
    handleDeptId: form.handleDeptId || undefined,
    reporterId: form.reporterId || undefined,
    registerOperatorId: form.registerOperatorId || undefined,
    dispatchOperatorId: form.dispatchOperatorId || undefined,
    address: form.address?.trim() || undefined,
    addressMatch: form.addressMatch,
    pageNum: pageNum.value,
    pageSize: pageSize.value
  }
  if (!criteria.caseCode) {
    delete criteria.caseCode
    delete criteria.caseCodeMatch
  }
  if (!criteria.address) {
    delete criteria.address
    delete criteria.addressMatch
  }
  if (!criteria.description) {
    delete criteria.description
    delete criteria.descriptionMatch
  }
  return criteria
}

function flattenDept(nodes, out = []) {
  if (!nodes) return out
  for (const n of nodes) {
    if (n.id != null) out.push({ id: n.id, deptName: n.deptName })
    if (n.children?.length) flattenDept(n.children, out)
  }
  return out
}

async function loadUsers(roleCode, target) {
  try {
    const res = await getUserList({ pageNum: 1, pageSize: 500, roleCode, status: 1 })
    target.value = res.data?.records || res.data?.list || []
  } catch (e) {
    console.warn('加载用户失败', roleCode, e)
    target.value = []
  }
}

async function loadOptions() {
  try {
    const gridRes = await getRespGridList()
    respGridOptions.value = gridRes.data || []
  } catch (e) {
    console.warn(e)
  }
  try {
    const deptRes = await getDeptTree()
    deptOptions.value = flattenDept(deptRes.data || [])
  } catch (e) {
    console.warn(e)
  }
  await Promise.all([
    loadUsers(RoleCode.COLLECTOR, collectorOptions),
    loadUsers(RoleCode.ACCEPTOR, acceptorOptions),
    loadUsers(RoleCode.DISPATCHER, dispatcherOptions)
  ])
}

async function runQuery() {
  loading.value = true
  try {
    const res = await queryCases(buildCriteria())
    const page = res.data || {}
    caseList.value = page.records || []
    total.value = page.total ?? 0
  } catch (e) {
    ElMessage.error(e.message || '查询失败')
    caseList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  runQuery()
}

function handleReset() {
  Object.assign(form, defaultForm())
  bigOptions.value = []
  smallOptions.value = []
  pageNum.value = 1
  caseList.value = []
  total.value = 0
}

function openDetail(row) {
  if (!row?.id) return
  router.push(`/case/detail/${row.id}`)
}

onMounted(() => {
  loadOptions()
})
</script>

<style scoped>
.case-query .search-card {
  margin-bottom: 16px;
}
.date-filter {
  display: flex;
  gap: 8px;
  width: 100%;
}
.query-form :deep(.el-form-item) {
  margin-bottom: 14px;
}
.list-card .pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
:deep(.el-table__row) {
  cursor: pointer;
}
</style>
