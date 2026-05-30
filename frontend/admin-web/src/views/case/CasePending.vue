<template>
  <div class="case-pending">
    <el-card v-if="showTabBar" class="filter-card">
      <el-radio-group v-model="currentTab" @change="handleTabChange">
        <el-radio-button label="pending_dispatch">待派遣</el-radio-button>
        <el-radio-button label="handler_dept_todo">部门待指派</el-radio-button>
        <el-radio-button label="dept_confirm_todo">处置人员已处置</el-radio-button>
        <el-radio-button label="handling">处置中</el-radio-button>
        <el-radio-button label="pending_check">待批转受理员</el-radio-button>
      </el-radio-group>
    </el-card>

    <el-card v-else-if="pageHint" class="filter-card hint-card">
      <span class="page-hint">{{ pageHint }}</span>
    </el-card>

    <!-- 案件列表 -->
    <el-card class="list-card">
      <el-table v-loading="loading" :data="caseList" style="width: 100%">
        <el-table-column prop="caseCode" label="案件编号" width="150" />
        <el-table-column prop="bigName" label="大类" width="100" />
        <el-table-column prop="smallName" label="小类" width="120" />
        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column v-if="showStatusColumn" prop="caseStatus" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getCaseStatusTagType(row.caseStatus)" size="small">
              {{ formatCaseStatusLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reportTime" label="上报时间" width="180" />
        <el-table-column prop="reporterName" label="上报人" width="100" />
        <el-table-column
          v-if="['pending_dispatch', 'dispatcher_pending_dispatch', 'dispatcher_returned'].includes(currentTab)"
          prop="currentHandlerName"
          label="派遣员"
          width="110"
        />
        <el-table-column
          v-if="['handler_dept_todo', 'dept_confirm_todo', 'dispatcher_returned'].includes(currentTab)"
          prop="handleDeptName"
          label="原处置部门"
          width="120"
        />
        <el-table-column
          v-if="['handler_dept_todo', 'dept_confirm_todo', 'pending_handle', 'handling'].includes(currentTab)"
          prop="handleDeptName"
          label="处置部门"
          width="120"
        />
        <el-table-column
          v-if="['handling', 'dept_confirm_todo'].includes(currentTab)"
          prop="currentHandlerName"
          label="处置人员"
          width="100"
        />
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
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="goDetail(row.id)">查看</el-button>
            <el-button
              v-if="canProcess(row)"
              type="success"
              size="small"
              @click="handleProcess(row)"
            >
              处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPendingCaseList } from '@/api/case'
import { useUserStore } from '@/stores/user'
import { defaultPendingTabForRoles, RoleCode } from '@/utils/roleAccess'
import { formatDateTime } from '@/utils/dateFormat'
import { formatCaseStatusLabel, getCaseStatusTagType } from '@/utils/caseStatus'
import { isRowStageOverdue, rowStageDeadline } from '@/utils/caseTimer'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const presetStatus = computed(() => route.meta.presetStatus || '')
const showTabBar = computed(() => !presetStatus.value)
const pageHint = computed(() => {
  if (!presetStatus.value) return ''
  const hints = {
    acceptor_pending_register: '尚未被受理员接手的新上报案件（立案后仅本人可见）',
    acceptor_pending_verify: '含上报待核实、核实任务，以及批转后待核查（请点「核实」）',
    acceptor_collect_check: '您下发的核查任务（含立案前现场核查、处置后复核）；采集员提交前会显示在此',
    acceptor_pending_close: '派遣员已批转给您、待核查的案件（请点「结案」）',
    acceptor_registered: '本人已立案并进入主流程的案件；作废案件请见「作废案件」菜单',
    not_accepted: '不符合立案标准已作废的案件',
    dispatcher_pending_dispatch: '受理员立案后批转、尚未派遣至处置部门的案件',
    dispatcher_pending_review: '处置部门确认完成并批转，待把关后批转受理员',
    dispatcher_returned: '处置部门认为非本部门职责、已回退的案件',
    dispatcher_handled: '本人参与派遣、批转等操作的案件（含已结案；已批转受理员后不在「待办审核」中显示）'
  }
  return hints[presetStatus.value] || route.meta.title || ''
})
const showStatusColumn = computed(() => Boolean(presetStatus.value))

const loading = ref(false)
const caseList = ref([])
const currentTab = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

function resolveInitialTab() {
  return presetStatus.value || defaultPendingTabForRoles(userStore.roles || [])
}

function onRefreshLists() {
  pageNum.value = 1
  loadCaseList()
}

onMounted(async () => {
  currentTab.value = resolveInitialTab()
  await loadCaseList()
  window.addEventListener('cityguard:refresh-lists', onRefreshLists)
})

onBeforeUnmount(() => {
  window.removeEventListener('cityguard:refresh-lists', onRefreshLists)
})

watch(
  () => route.meta.presetStatus,
  async (next, prev) => {
    if (next === prev) return
    currentTab.value = resolveInitialTab()
    pageNum.value = 1
    await loadCaseList()
  }
)

async function loadCaseList() {
  loading.value = true
  try {
    const res = await getPendingCaseList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: currentTab.value
    })
    caseList.value = res.data?.records || []
    total.value = res.data?.total ?? 0
  } catch (error) {
    console.error('获取案件列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  pageNum.value = 1
  loadCaseList()
}

function handleSizeChange(size) {
  pageSize.value = size
  loadCaseList()
}

function handleCurrentChange(page) {
  pageNum.value = page
  loadCaseList()
}

function goDetail(id) {
  const mode = route.meta.acceptorActionMode
  router.push({
    path: `/case/detail/${id}`,
    query: mode ? { acceptorMode: mode } : {}
  })
}

function acceptorCanViewRow(row) {
  const uid = userStore.userInfo?.id ?? userStore.userInfo?.userId
  const owner = row.registerOperatorId
  const st = row.caseStatus
  if (owner == null && ['reported', 'pending_register', 'pending_verify'].includes(st)) {
    return true
  }
  if (owner != null && String(owner) === String(uid)) return true
  return st === 'pending_check' && row.currentHandlerId != null && String(row.currentHandlerId) === String(uid)
}

function acceptorCanCheckCloseRow(row) {
  const uid = userStore.userInfo?.id ?? userStore.userInfo?.userId
  if (row.awaitingDeptConfirm || row.awaitingDispatcherForward) return false
  return row.caseStatus === 'pending_check' && row.currentHandlerId != null && String(row.currentHandlerId) === String(uid)
}

function canProcess(row) {
  const status = row.caseStatus
  const roles = userStore.roles || []
  const isAdmin = roles.includes(RoleCode.ADMIN) || roles.includes(RoleCode.SUPERVISOR)

  if (presetStatus.value === 'not_accepted') {
    return false
  }
  if (presetStatus.value === 'acceptor_collect_check') {
    return false
  }
  if (presetStatus.value === 'acceptor_registered') {
    return false
  }
  if (presetStatus.value === 'acceptor_pending_verify') {
    if (isAdmin) return true
    if (status === 'pending_check') return acceptorCanCheckCloseRow(row)
    if (!roles.includes(RoleCode.ACCEPTOR)) return false
    return acceptorCanViewRow(row)
  }
  if (presetStatus.value === 'acceptor_pending_close') {
    if (isAdmin) return true
    return acceptorCanCheckCloseRow(row)
  }
  if (presetStatus.value === 'dispatcher_returned') {
    return isAdmin || roles.includes(RoleCode.DISPATCHER)
  }
  if (presetStatus.value === 'dispatcher_pending_dispatch') {
    return isAdmin || roles.includes(RoleCode.DISPATCHER)
  }
  if (presetStatus.value === 'dispatcher_pending_review') {
    if (isAdmin || roles.includes(RoleCode.SUPERVISOR)) return true
    if (!roles.includes(RoleCode.DISPATCHER)) return false
    const uid = userStore.userInfo?.id ?? userStore.userInfo?.userId
    const assignee = row.currentHandlerId
    const awaiting =
      row.awaitingDispatcherForward === true ||
      (assignee != null && String(assignee) === String(uid))
    return awaiting
  }

  if (['reported', 'pending_register', 'pending_verify'].includes(status)) {
    if (isAdmin) return true
    if (!roles.includes(RoleCode.ACCEPTOR)) return false
    return acceptorCanViewRow(row)
  }
  if (status === 'pending_dispatch') {
    if (isAdmin) return true
    if (!roles.includes(RoleCode.DISPATCHER)) return false
    const assignee = row.currentHandlerId
    if (assignee == null) return true
    return String(assignee) === String(userStore.userInfo?.id ?? userStore.userInfo?.userId)
  }
  if (status === 'pending_handle' && row.currentHandlerId == null) {
    if (isAdmin) return true
    if (!roles.includes(RoleCode.DEPT)) return false
    return String(row.handleDeptId) === String(userStore.userInfo?.departmentId)
  }
  if (row.awaitingDeptConfirm || status === 'handle_finish') {
    if (isAdmin) return true
    if (!roles.includes(RoleCode.DEPT)) return false
    return String(row.handleDeptId) === String(userStore.userInfo?.departmentId)
  }
  if (status === 'handling') {
    if (isAdmin) return true
    if (!roles.includes(RoleCode.HANDLER)) return false
    const assignee = row.currentHandlerId
    if (assignee == null) return false
    return String(assignee) === String(userStore.userInfo?.id ?? userStore.userInfo?.userId)
  }
  if (status === 'pending_check') {
    if (row.awaitingDeptConfirm) return false
    if (roles.includes(RoleCode.DEPT) && !isAdmin && !roles.includes(RoleCode.DISPATCHER)) {
      return false
    }
    const uid = userStore.userInfo?.id ?? userStore.userInfo?.userId
    if (isAdmin || roles.includes(RoleCode.SUPERVISOR)) return true
    const assignee = row.currentHandlerId
    const awaitingDispatcherForward =
      row.awaitingDispatcherForward === true ||
      (assignee != null && String(assignee) === String(uid) && roles.includes(RoleCode.DISPATCHER))
    if (roles.includes(RoleCode.DISPATCHER) && awaitingDispatcherForward) {
      return true
    }
    if (roles.includes(RoleCode.ACCEPTOR)) {
      if (awaitingDispatcherForward) return false
      if (presetStatus.value === 'acceptor_pending_close') {
        return acceptorCanCheckCloseRow(row)
      }
      if (presetStatus.value === 'acceptor_pending_verify') {
        return acceptorCanCheckCloseRow(row)
      }
      return acceptorCanCheckCloseRow(row)
    }
    return false
  }
  return false
}

function handleProcess(row) {
  router.push(`/case/detail/${row.id}?action=process`)
}

</script>

<style lang="scss" scoped>
.case-pending {
  .filter-card {
    margin-bottom: 20px;
  }

  .hint-card .page-hint {
    font-size: 14px;
    color: #606266;
  }

  .list-card {
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
