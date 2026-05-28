<template>
  <div class="case-detail">
    <el-alert
      v-if="acceptorViewOnlyHint"
      class="view-only-hint"
      type="info"
      :closable="false"
      show-icon
      :title="acceptorViewOnlyHint"
    />
    <el-alert
      v-if="caseInfo.isSuspended === 1"
      class="view-only-hint"
      type="warning"
      :closable="false"
      show-icon
      :title="suspendAlertTitle"
    />
    <el-alert
      v-if="caseInfo.pendingCheckTask || caseInfo.caseStatus === 'checking'"
      class="view-only-hint"
      type="warning"
      :closable="false"
      show-icon
      title="核查任务进行中（可选分支）：请等待采集员提交后再立案或作废；如无需核查可不下发。"
    />
    <el-alert
      v-if="caseInfo.pendingVerifyTask"
      class="view-only-hint"
      type="warning"
      :closable="false"
      show-icon
      title="核实任务进行中（可选分支）：请等待采集员提交后再结案；如无需现场核实可直接结案。"
    />
    <el-alert
      v-if="caseInfo.pendingExtensionApply && canReviewPendingAdjustment"
      class="view-only-hint"
      type="warning"
      :closable="false"
      show-icon
    >
      <template #title>
        待审批延期：{{ caseInfo.pendingExtensionApply.applicantName }} — {{ caseInfo.pendingExtensionApply.reason }}
      </template>
    </el-alert>
    <el-alert
      v-if="caseInfo.pendingSuspendApply && canReviewPendingAdjustment"
      class="view-only-hint"
      type="warning"
      :closable="false"
      show-icon
    >
      <template #title>
        待审批挂账（至 {{ formatAdjustmentDate(caseInfo.pendingSuspendApply.suspendUntil) }}）：
        {{ caseInfo.pendingSuspendApply.applicantName }} — {{ caseInfo.pendingSuspendApply.reason }}
      </template>
    </el-alert>
    <!-- 基本信息 -->
    <el-card class="info-card">
      <template #header>
        <div class="header-with-action">
          <span>案件基本信息</span>
          <div class="actions">
            <el-button
              v-if="canReviewPendingExtension"
              type="success"
              @click="openAdjustmentReview(caseInfo.pendingExtensionApply, true)"
            >
              批准延期
            </el-button>
            <el-button
              v-if="canReviewPendingExtension"
              type="danger"
              plain
              @click="openAdjustmentReview(caseInfo.pendingExtensionApply, false)"
            >
              驳回延期
            </el-button>
            <el-button
              v-if="canReviewPendingSuspend"
              type="success"
              @click="openAdjustmentReview(caseInfo.pendingSuspendApply, true)"
            >
              批准挂账
            </el-button>
            <el-button
              v-if="canReviewPendingSuspend"
              type="danger"
              plain
              @click="openAdjustmentReview(caseInfo.pendingSuspendApply, false)"
            >
              驳回挂账
            </el-button>
            <el-button v-if="canApplyExtension" type="warning" plain @click="openAdjustmentApply('extension')">申请延期</el-button>
            <el-button v-if="canApplySuspend" type="warning" plain @click="openAdjustmentApply('suspend')">申请挂账</el-button>
            <el-button v-if="canSubmitTimeoutAppeal" type="warning" @click="openTimeoutAppealDialog">提起申诉</el-button>
            <el-button v-if="timeoutAppeal?.id" type="primary" link @click="goTimeoutAppealDetail">查看申诉</el-button>
            <el-button v-if="canSendCheck" type="info" plain @click="handleSendCheck">发送核查</el-button>
            <el-button v-if="canSendVerifyTask" type="info" plain @click="handleSendVerifyTask">发送核实</el-button>
            <el-button v-if="canRegister" type="primary" @click="handleRegister">立案</el-button>
            <el-button v-if="canDispatch" type="success" @click="handleDispatch">派遣至处置部门</el-button>
            <el-button v-if="canAssignHandler" type="primary" @click="handleAssignHandler">指派处置人员</el-button>
            <el-button v-if="canDeptRevokeAssign" type="warning" plain @click="handleDeptRevokeAssign">撤销指派</el-button>
            <el-button v-if="canHandle" type="warning" @click="handleHandle">提交处置</el-button>
            <el-button v-if="canDeptReturnHandler" type="warning" @click="handleDeptReturnHandler">打回处置人员</el-button>
            <el-button v-if="canDeptReturn" type="warning" @click="handleDeptReturn">回退派遣员</el-button>
            <el-button v-if="canDeptConfirm" type="success" @click="handleDeptConfirm">批转派遣员</el-button>
            <el-button v-if="canDispatcherReturnAcceptor" type="warning" @click="handleDispatcherReturnAcceptor">回退受理员</el-button>
            <el-button v-if="canDispatcherReturnDept" type="warning" @click="handleDispatcherReturnDept">返工处置部门</el-button>
            <el-button v-if="canDispatcherForwardAcceptor" type="success" @click="handleDispatcherForwardAcceptor">批转受理员</el-button>
            <el-button v-if="canAcceptorReturnDispatcher" type="warning" @click="handleAcceptorReturnDispatcher">回退派遣员返工</el-button>
            <el-button v-if="canClose" type="success" @click="handleClose">结案</el-button>
            <el-button v-if="canReject" type="danger" @click="handleReject">作废</el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="案件编号">{{ caseInfo.caseCode }}</el-descriptions-item>
        <el-descriptions-item label="案件状态">
          <el-tag :type="displayStatusType">
            {{ displayStatusLabel }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="案件来源">{{ sourceLabel }}</el-descriptions-item>
        <el-descriptions-item label="案件大类">{{ caseInfo.bigName }}</el-descriptions-item>
        <el-descriptions-item label="案件小类">{{ caseInfo.smallName }}</el-descriptions-item>
        <el-descriptions-item label="立案条件">{{ caseInfo.conditionDesc || '--' }}</el-descriptions-item>
        <el-descriptions-item label="发生地址" :span="2">{{ caseInfo.address }}</el-descriptions-item>
        <el-descriptions-item label="坐标">{{ lngLatDisplay }}</el-descriptions-item>
        <el-descriptions-item label="上报时间">{{ caseInfo.reportTime }}</el-descriptions-item>
        <el-descriptions-item label="上报人">{{ caseInfo.reporterName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="责任片区">{{ respGridDisplay }}</el-descriptions-item>
        <el-descriptions-item
          v-if="caseInfo.registerOperatorName"
          label="立案受理员"
        >
          {{ caseInfo.registerOperatorName }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="caseInfo.caseStatus === 'pending_dispatch' && caseInfo.currentHandlerName"
          label="指定派遣员"
        >
          {{ caseInfo.currentHandlerName }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="caseInfo.caseStatus === 'pending_check' && caseInfo.currentHandlerName"
          label="收尾受理员"
        >
          {{ caseInfo.currentHandlerName }}
        </el-descriptions-item>
        <el-descriptions-item v-if="caseInfo.handleDeptName" label="处置部门">
          {{ caseInfo.handleDeptName }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="caseInfo.currentHandlerName && ['pending_handle', 'handling', 'handle_finish'].includes(caseInfo.caseStatus)"
          label="处置人员"
        >
          {{ caseInfo.currentHandlerName }}
        </el-descriptions-item>
        <el-descriptions-item label="处置截止时间">{{ caseInfo.deadlineTime || '--' }}</el-descriptions-item>
        <el-descriptions-item v-if="caseInfo.timeRemaining" label="处置时限">
          {{ caseInfo.timeRemaining }}
          <el-tag v-if="caseInfo.handleTimeoutExempt === 1" type="success" size="small" class="exempt-inline-tag">
            不计超时
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item
          v-if="caseInfo.handleStageTimedOut || caseInfo.handleTimeoutExempt === 1"
          label="处置超时"
        >
          <span v-if="caseInfo.handleStageTimedOut">曾超时</span>
          <span v-else>否</span>
          <el-tag v-if="caseInfo.handleTimeoutExempt === 1" type="success" size="small" class="exempt-inline-tag">
            申诉通过
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="caseInfo.extensionApprovedCount > 0" label="已批准延期">
          {{ caseInfo.extensionApprovedCount }} 次
        </el-descriptions-item>
        <el-descriptions-item v-if="caseInfo.dispatchOperatorName" label="派遣员">
          {{ caseInfo.dispatchOperatorName }}
        </el-descriptions-item>
        <el-descriptions-item label="问题描述" :span="3">{{ caseInfo.description }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 地图定位 -->
    <el-card class="map-card">
      <template #header>
        <span>地图定位</span>
      </template>
      <div ref="mapContainerRef" class="map-container" />
      <el-empty v-if="!hasMapCoords" description="暂无坐标，无法展示地图" />
    </el-card>

    <!-- 附件信息 -->
    <el-card class="attachment-card">
      <template #header>
        <span>现场照片/视频</span>
      </template>
      <el-row :gutter="20">
        <el-col v-for="item in attachments" :key="item.key" :span="6">
          <div class="attachment-item-wrap">
            <el-image
              v-if="item.type === 'image'"
              :src="item.url"
              :preview-src-list="imageList"
              fit="cover"
              class="attachment-image"
            />
            <video v-else :src="item.url" class="attachment-video" controls />
            <p v-if="item.label" class="attachment-caption">{{ item.label }}</p>
          </div>
        </el-col>
      </el-row>
      <el-empty v-if="attachments.length === 0" description="暂无附件" />
    </el-card>

    <!-- 流程记录 -->
    <el-card class="flow-card">
      <template #header>
        <span>流程记录</span>
      </template>
      <el-timeline>
        <el-timeline-item
          v-for="item in flowRecords"
          :key="item.id"
          :timestamp="item.operateTime"
          placement="top"
        >
          <el-card>
            <h4>{{ item.nodeName }}</h4>
            <p>操作人：{{ item.operatorName }}</p>
            <p v-if="item.operateOpinion">备注：{{ item.operateOpinion }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="flowRecords.length === 0" description="暂无流程记录" />
    </el-card>

    <!-- 处理对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
    >
      <el-form :model="processForm" label-width="120px">
        <el-form-item v-if="showDispatcherSelect" label="派遣员" required>
          <el-select
            v-model="processForm.dispatcherUserId"
            :placeholder="dispatcherSelectPlaceholder"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="u in dispatcherOptions"
              :key="u.id"
              :label="u.realName || u.username"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="showAcceptorSelect" label="受理员" required>
          <el-select
            v-model="processForm.acceptorUserId"
            placeholder="请选择受理员（批转后由其牵头核实/结案，其他受理员也可协办）"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="u in acceptorOptions"
              :key="u.id"
              :label="acceptorOptionLabel(u)"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <template v-if="showRegisterEdit">
          <el-form-item label="发生地址">
            <el-input v-model="processForm.address" type="textarea" :rows="2" placeholder="可修正地址" />
          </el-form-item>
          <el-form-item label="问题描述">
            <el-input v-model="processForm.description" type="textarea" :rows="3" placeholder="可修正描述" />
          </el-form-item>
          <el-form-item label="立案条件说明">
            <el-input v-model="processForm.conditionDesc" type="textarea" :rows="2" placeholder="可选" />
          </el-form-item>
          <el-form-item label="经度">
            <el-input v-model="processForm.longitude" placeholder="可选，数字" />
          </el-form-item>
          <el-form-item label="纬度">
            <el-input v-model="processForm.latitude" placeholder="可选，数字" />
          </el-form-item>
        </template>
        <el-form-item v-if="showDeptSelect" label="处置部门" prop="departmentId" required>
          <el-tree-select
            v-model="processForm.departmentId"
            :data="deptTree"
            :props="{ label: 'deptName', value: 'id', children: 'children' }"
            placeholder="请选择处置部门（二级平台）"
            check-strictly
            filterable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="showHandlerSelect" label="处置人员" required>
          <el-select
            v-model="processForm.handlerUserId"
            placeholder="请选择本部门处置人员"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="u in handlerOptions"
              :key="u.id"
              :label="u.realName || u.username"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="processForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
        <el-form-item v-if="showAttachmentUpload" label="附件">
          <el-upload
            v-model:file-list="processForm.attachments"
            list-type="picture-card"
            :limit="5"
            accept="image/*,video/*"
            :http-request="uploadProcessFile"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitProcess">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="sendTaskDialogVisible"
      :title="sendTaskDialogTitle"
      width="760px"
      destroy-on-close
      @opened="onSendTaskDialogOpened"
      @closed="destroySendTaskMap"
    >
      <p class="send-task-tip">
        位置为采集员<strong>最近一次上报</strong>坐标，供参考（非实时 GPS）。系统默认推荐距离案发地最近者，您也可在列表或地图上改选。
      </p>
      <div ref="sendTaskMapRef" class="send-task-map" />
      <el-table
        v-loading="sendTaskLoading"
        :data="collectorCandidates"
        highlight-current-row
        max-height="240"
        class="collector-table"
        @current-change="onCollectorRowChange"
      >
        <el-table-column width="48">
          <template #default="{ row }">
            <el-radio v-model="selectedCollectorId" :label="row.userId" @change="focusCollectorOnMap(row)">
              &nbsp;
            </el-radio>
          </template>
        </el-table-column>
        <el-table-column label="采集员" min-width="100">
          <template #default="{ row }">
            {{ row.realName || row.username }}
            <el-tag v-if="row.recommended" type="success" size="small" class="ml-4">推荐</el-tag>
            <el-tag v-if="row.onRespGrid" type="info" size="small" class="ml-4">责任片区</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="距离" width="88">
          <template #default="{ row }">
            {{ row.distanceKm != null ? `${row.distanceKm} km` : '--' }}
          </template>
        </el-table-column>
        <el-table-column label="位置说明" min-width="140" prop="locationHint" show-overflow-tooltip />
      </el-table>
      <el-form label-width="72px" class="send-task-form">
        <el-form-item label="说明">
          <el-input v-model="sendTaskRemark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sendTaskDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="sendTaskSubmitting" @click="submitSendTask">下发</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="adjustmentReviewVisible" :title="adjustmentReviewApproved ? '批准申请' : '驳回申请'" width="480px">
      <p v-if="adjustmentReviewRow" class="adjustment-review-summary">
        {{ adjustmentReviewRow.applyTypeLabel || (adjustmentReviewRow.applyType === 'suspend' ? '挂账' : '延期') }}
        · 申请人 {{ adjustmentReviewRow.applicantName }}
      </p>
      <p v-if="adjustmentReviewRow?.reason" class="adjustment-review-reason">{{ adjustmentReviewRow.reason }}</p>
      <p
        v-if="adjustmentReviewRow?.applyType === 'suspend' && adjustmentReviewRow?.suspendUntil"
        class="adjustment-review-reason"
      >
        挂账截止日期：{{ formatAdjustmentDate(adjustmentReviewRow.suspendUntil) }}
      </p>
      <el-form label-width="80px">
        <el-form-item :label="adjustmentReviewApproved ? '审批意见' : '驳回原因'" :required="!adjustmentReviewApproved">
          <el-input v-model="adjustmentReviewRemark" type="textarea" :rows="3" placeholder="驳回时必填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustmentReviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjustmentReviewSubmitting" @click="submitAdjustmentReview">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="timeoutAppealDialogVisible" title="处置超时申诉" width="520px" destroy-on-close>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="仅已结案且处置阶段曾超时的本部门案件可申诉，一案仅可提交一次。"
        class="appeal-dialog-hint"
      />
      <el-form label-width="88px" class="appeal-form">
        <el-form-item label="申诉说明" required>
          <el-input
            v-model="timeoutAppealForm.appealDesc"
            type="textarea"
            :rows="4"
            placeholder="请说明客观因素"
          />
        </el-form-item>
        <el-form-item label="证明材料">
          <el-upload
            list-type="picture-card"
            :limit="5"
            :http-request="uploadAppealFile"
            v-model:file-list="timeoutAppealFileList"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="timeoutAppealDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="timeoutAppealSubmitting" @click="submitTimeoutAppeal">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="adjustmentDialogVisible"
      :title="adjustmentDialogTitle"
      width="480px"
      :key="'adjustment-' + adjustmentForm.applyType"
    >
      <el-form label-width="110px">
        <el-form-item label="申请原因" required>
          <el-input v-model="adjustmentForm.reason" type="textarea" :rows="3" placeholder="请说明延期/挂账原因" />
        </el-form-item>
        <el-form-item v-if="adjustmentForm.applyType === 'suspend'" label="挂账截止日期" required>
          <el-date-picker
            v-model="adjustmentForm.suspendUntil"
            type="date"
            placeholder="请选择挂账截止日期"
            value-format="YYYY-MM-DD"
            :disabled-date="disableSuspendDate"
            style="width: 100%"
          />
          <p class="adjustment-form-tip">自今日起最长不超过 1 年，到期后案件将自动恢复处置</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustmentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjustmentSubmitting" @click="submitAdjustmentApply">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, onActivated, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getCaseDetail,
  getCollectorCandidates,
  getCaseFlowRecords,
  getCaseAttachments,
  getCheckTaskRecords,
  getVerifyTaskRecords,
  registerCase,
  dispatchCase,
  assignHandlerCase,
  handleCase,
  deptConfirmCase,
  deptReturnCase,
  deptRevokeAssign,
  deptReturnHandler,
  dispatcherForwardToAcceptor,
  dispatcherReturnAcceptor,
  dispatcherReturnDept,
  acceptorReturnDispatcher,
  closeCase,
  rejectCase,
  sendCheckTask,
  sendVerifyTask,
  applyCaseAdjustment,
  getCaseAdjustmentList,
  reviewCaseAdjustment
} from '@/api/case'
import { getTimeoutAppealByCase, submitTimeoutAppeal as apiSubmitTimeoutAppeal } from '@/api/appeal'
import { getRespGridDetail, getRespGridList, checkRespGridLocation } from '@/api/geo'
import { getDeptTree, getUserList } from '@/api/system'
import request from '@/utils/request'
import { fetchFilePreviewBlobUrl, revokeBlobUrls } from '@/utils/fileUrl'
import { useUserStore } from '@/stores/user'
import { RoleCode } from '@/utils/roleAccess'
import { formatCaseStatusLabel, getCaseStatusTagType } from '@/utils/caseStatus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 从待核实/待我结案队列进入时限定按钮：verify=仅核实，close=仅结案 */
const acceptorQueueMode = computed(() => route.query.acceptorMode || '')

const mapContainerRef = ref(null)
const sendTaskMapRef = ref(null)
let caseMap = null
let caseMarker = null
let sendTaskMap = null
const sendTaskMarkers = []

const sendTaskDialogVisible = ref(false)
const sendTaskType = ref('check')
const sendTaskDialogTitle = computed(() =>
  sendTaskType.value === 'check' ? '发送核查 — 选择采集员' : '发送核实 — 选择采集员'
)
const collectorCandidates = ref([])
const selectedCollectorId = ref(null)
const sendTaskRemark = ref('')
const sendTaskLoading = ref(false)
const sendTaskSubmitting = ref(false)

const adjustmentDialogVisible = ref(false)
const adjustmentSubmitting = ref(false)
const adjustmentReviewVisible = ref(false)
const adjustmentReviewSubmitting = ref(false)
const adjustmentReviewApproved = ref(true)
const adjustmentReviewRemark = ref('')
const adjustmentReviewRow = ref(null)
const adjustmentForm = reactive({
  applyType: 'extension',
  reason: '',
  suspendUntil: ''
})
const adjustmentRecords = ref([])
const timeoutAppeal = ref(null)
const timeoutAppealDialogVisible = ref(false)
const timeoutAppealSubmitting = ref(false)
const timeoutAppealFileList = ref([])
const timeoutAppealForm = reactive({
  appealDesc: ''
})

const caseInfo = ref({})
const attachments = ref([])
const flowRecords = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const currentAction = ref('')
const deptTree = ref([])
const dispatcherOptions = ref([])
const acceptorOptions = ref([])
const handlerOptions = ref([])

const processForm = reactive({
  dispatcherUserId: null,
  acceptorUserId: null,
  handlerUserId: null,
  departmentId: '',
  clientUpdateTime: null,
  address: '',
  description: '',
  conditionDesc: '',
  longitude: '',
  latitude: '',
  checkResult: '',
  remark: '',
  attachments: []
})

const imageList = computed(() => attachments.value.filter(a => a.type === 'image').map(a => a.url))

const lngLatDisplay = computed(() => {
  const lng = caseInfo.value.longitude
  const lat = caseInfo.value.latitude
  if (lng == null || lat == null) return '--'
  return `${lng}, ${lat}`
})

const hasMapCoords = computed(() => {
  const lng = caseInfo.value.longitude
  const lat = caseInfo.value.latitude
  return lng != null && lat != null && Number.isFinite(Number(lng)) && Number.isFinite(Number(lat))
})

/** 责任片区名称：东片区、西片区等 */
const respGridDisplay = computed(() => caseInfo.value.respGridName || '--')

const sourceLabel = computed(() => {
  const t = caseInfo.value.sourceType
  const d = caseInfo.value.sourceDesc
  const map = {
    collector: '采集员上报',
    register: '案件登记',
    leader: '领导批示',
    transfer: '部门批转'
  }
  const base = map[t] || t || '--'
  return d ? `${base}（${d}）` : base
})

// 操作权限判断（与后端 CaseStatusConstant 一致）
const REGISTERABLE_STATUSES = ['pending_register', 'pending_verify', 'reported', 'returned']
const ACCEPTOR_POOL_VIEW_STATUSES = [...REGISTERABLE_STATUSES, 'checking']

const currentUserId = computed(
  () => userStore.userInfo?.id ?? userStore.userInfo?.userId
)

function hasRole(code) {
  return (userStore.roles || []).includes(code)
}

/** 受理员：未认领或本人经手（与后端 canAcceptorViewCase 一致） */
function acceptorCanOperate(info) {
  if (!info) return false
  const owner = info.registerOperatorId
  const uid = currentUserId.value
  return owner == null || String(owner) === String(uid)
}

/** 查看：公共池 + 立案人全程 + 核查中 + 被批转收尾的受理员 */
function canAcceptorViewCaseInfo(info) {
  if (!info || isAdminLike.value) return true
  if (!hasRole(RoleCode.ACCEPTOR)) return false
  const uid = currentUserId.value
  const owner = info.registerOperatorId
  const st = info.caseStatus
  if (st === 'checking') {
    if (owner == null) return true
    if (owner != null && String(owner) === String(uid)) return true
    if (info.pendingCheckTask) return true
    return true
  }
  if (owner == null && ACCEPTOR_POOL_VIEW_STATUSES.includes(st)) return true
  if (owner != null && String(owner) === String(uid)) return true
  return st === 'pending_check' && info.currentHandlerId != null && String(info.currentHandlerId) === String(uid)
}

/** 核实/结案：仅 current_handler 为本人（立案人批转他人后只读） */
function acceptorCanCheckClose(info) {
  if (!info) return false
  const uid = currentUserId.value
  return info.currentHandlerId != null && String(info.currentHandlerId) === String(uid)
}

/** 待派遣员把关批转受理员（兼容旧后端未返回 awaitingDispatcherForward） */
function resolveAwaitingDispatcherForward(info) {
  if (!info || info.awaitingDeptConfirm) {
    return false
  }
  if (info.awaitingDispatcherForward === true) {
    return true
  }
  if (info.caseStatus !== 'pending_check' || info.currentHandlerId == null) {
    return false
  }
  if (hasRole(RoleCode.DISPATCHER) && !isAdminLike.value) {
    return String(info.currentHandlerId) === String(currentUserId.value)
  }
  return false
}

const isAdminLike = computed(
  () => hasRole(RoleCode.ADMIN) || hasRole(RoleCode.SUPERVISOR)
)

const canRegister = computed(() => {
  if (!hasRole(RoleCode.ACCEPTOR) && !isAdminLike.value) return false
  const st = caseInfo.value.caseStatus
  if (!REGISTERABLE_STATUSES.includes(st)) return false
  if (isAdminLike.value) return true
  if (st === 'returned') {
    const uid = currentUserId.value
    const reg = caseInfo.value.registerOperatorId
    const cur = caseInfo.value.currentHandlerId
    return (
      (reg != null && String(reg) === String(uid)) ||
      (cur != null && String(cur) === String(uid))
    )
  }
  return acceptorCanOperate(caseInfo.value)
})
/** 立案前可选：下发核查任务 */
const canSendCheck = computed(() => {
  if (!canRegister.value) return false
  if (caseInfo.value.caseStatus === 'checking') return false
  if (caseInfo.value.pendingCheckTask) return false
  return true
})

/** 结案前可选：下发核实任务 */
const canSendVerifyTask = computed(() => {
  if (!canAcceptorCheckClose.value) return false
  if (caseInfo.value.pendingVerifyTask) return false
  return true
})
const canDispatch = computed(() => {
  const status = caseInfo.value.caseStatus
  if (status !== 'pending_dispatch' && status !== 'returned') {
    return false
  }
  if (!hasRole(RoleCode.DISPATCHER) && !isAdminLike.value) {
    return false
  }
  const assignee = caseInfo.value.currentHandlerId
  if (isAdminLike.value) {
    return true
  }
  if (assignee == null) {
    return status === 'pending_dispatch'
  }
  return String(assignee) === String(currentUserId.value)
})

const canDeptRevokeAssign = computed(() => {
  if (isCaseSuspended.value) return false
  if (caseInfo.value.caseStatus !== 'handling' || caseInfo.value.currentHandlerId == null) {
    return false
  }
  if (!hasRole(RoleCode.DEPT) && !isAdminLike.value) return false
  if (isAdminLike.value) return true
  const deptId = caseInfo.value.handleDeptId
  return deptId != null && String(deptId) === String(userDeptId.value)
})

const canDeptReturnHandler = computed(() => {
  if (!caseInfo.value.awaitingDeptConfirm && caseInfo.value.caseStatus !== 'handle_finish') {
    return false
  }
  if (!hasRole(RoleCode.DEPT) && !isAdminLike.value) return false
  if (isAdminLike.value) return true
  const deptId = caseInfo.value.handleDeptId
  return deptId != null && String(deptId) === String(userDeptId.value)
})

const canDeptReturn = computed(() => {
  if (isCaseSuspended.value) return false
  if (caseInfo.value.caseStatus !== 'pending_handle') {
    return false
  }
  if (caseInfo.value.currentHandlerId != null) {
    return false
  }
  if (!hasRole(RoleCode.DEPT) && !isAdminLike.value) {
    return false
  }
  if (isAdminLike.value) {
    return true
  }
  const deptId = caseInfo.value.handleDeptId
  return deptId != null && String(deptId) === String(userDeptId.value)
})
const userDeptId = computed(() => userStore.userInfo?.departmentId)

const isCaseSuspended = computed(() => caseInfo.value.isSuspended === 1)

const suspendAlertTitle = computed(() => {
  const until = caseInfo.value.suspendUntil
  const tail = until ? `，挂账截止 ${String(until).substring(0, 10)}` : ''
  return `案件挂账中，系统内暂不可处置操作${tail}`
})

const adjustmentDialogTitle = computed(() =>
  adjustmentForm.applyType === 'suspend' ? '申请挂账' : '申请延期'
)

const CLOSED_STATUSES = ['closed', 'forced_close']

const canSubmitTimeoutAppeal = computed(() => {
  if (!hasRole(RoleCode.DEPT)) return false
  const c = caseInfo.value
  if (!c?.id || !CLOSED_STATUSES.includes(c.caseStatus)) return false
  if (!c.handleStageTimedOut) return false
  if (c.handleTimeoutExempt === 1) return false
  if (timeoutAppeal.value?.id) return false
  const deptId = userStore.userInfo?.departmentId
  return deptId != null && c.handleDeptId === deptId
})

function goTimeoutAppealDetail() {
  if (timeoutAppeal.value?.id) {
    router.push({ name: 'AppealDetail', params: { id: timeoutAppeal.value.id } })
  }
}

/** 当案派遣员或管理员/值班长可审批延期/挂账 */
const canReviewPendingAdjustment = computed(() => {
  if (isAdminLike.value) return true
  if (!hasRole(RoleCode.DISPATCHER)) return false
  const dispatchOp = caseInfo.value.dispatchOperatorId
  if (dispatchOp == null) return true
  return String(dispatchOp) === String(currentUserId.value)
})

const canReviewPendingExtension = computed(
  () => canReviewPendingAdjustment.value && !!caseInfo.value.pendingExtensionApply
)

const canReviewPendingSuspend = computed(
  () => canReviewPendingAdjustment.value && !!caseInfo.value.pendingSuspendApply
)

const canApplyExtension = computed(() => {
  const st = caseInfo.value.caseStatus
  if (st !== 'pending_handle' && st !== 'handling') return false
  if (isCaseSuspended.value) return false
  if (caseInfo.value.hasPendingExtension) return false
  const count = caseInfo.value.extensionApprovedCount || 0
  if (count >= 2) return false
  if (!hasRole(RoleCode.DEPT) && !isAdminLike.value) return false
  if (isAdminLike.value) return true
  const deptId = caseInfo.value.handleDeptId
  return deptId != null && String(deptId) === String(userDeptId.value)
})

const canApplySuspend = computed(() => {
  const st = caseInfo.value.caseStatus
  if (st !== 'pending_handle' && st !== 'handling') return false
  if (isCaseSuspended.value) return false
  if (caseInfo.value.hasPendingSuspend) return false
  if (caseInfo.value.suspendEverApproved) return false
  if (!hasRole(RoleCode.DEPT) && !isAdminLike.value) return false
  if (isAdminLike.value) return true
  const deptId = caseInfo.value.handleDeptId
  return deptId != null && String(deptId) === String(userDeptId.value)
})

const canAssignHandler = computed(() => {
  if (isCaseSuspended.value) return false
  if (caseInfo.value.caseStatus !== 'pending_handle') {
    return false
  }
  if (caseInfo.value.currentHandlerId != null) {
    return false
  }
  if (!hasRole(RoleCode.DEPT) && !isAdminLike.value) {
    return false
  }
  if (isAdminLike.value) {
    return true
  }
  const deptId = caseInfo.value.handleDeptId
  return deptId != null && String(deptId) === String(userDeptId.value)
})

const canHandle = computed(() => {
  if (caseInfo.value.caseStatus !== 'handling') {
    return false
  }
  if (!hasRole(RoleCode.HANDLER) && !isAdminLike.value) {
    return false
  }
  const assignee = caseInfo.value.currentHandlerId
  if (isAdminLike.value) {
    return true
  }
  return assignee != null && String(assignee) === String(currentUserId.value)
})

const canDeptConfirm = computed(() => {
  if (isCaseSuspended.value) return false
  if (!caseInfo.value.awaitingDeptConfirm) {
    return false
  }
  if (!hasRole(RoleCode.DEPT) && !isAdminLike.value) {
    return false
  }
  if (isAdminLike.value) {
    return true
  }
  const deptId = caseInfo.value.handleDeptId
  return deptId != null && String(deptId) === String(userDeptId.value)
})

const isDeptOnlyOperator = computed(() => {
  const roles = userStore.roles || []
  return roles.includes(RoleCode.DEPT) && !isAdminLike.value && !roles.includes(RoleCode.DISPATCHER)
})

const displayStatusLabel = computed(() => {
  if (caseInfo.value.awaitingDeptConfirm) {
    return '处置人员已处置'
  }
  if (resolveAwaitingDispatcherForward(caseInfo.value)) {
    return '待派遣员把关'
  }
  return formatCaseStatusLabel(caseInfo.value, { detail: true })
})

const displayStatusType = computed(() => {
  if (
    caseInfo.value.awaitingDeptConfirm ||
    resolveAwaitingDispatcherForward(caseInfo.value)
  ) {
    return 'success'
  }
  return getCaseStatusTagType(caseInfo.value.caseStatus)
})

const canDispatcherReturnAcceptor = computed(() => {
  if (caseInfo.value.caseStatus !== 'pending_dispatch') return false
  if (!hasRole(RoleCode.DISPATCHER) && !isAdminLike.value) return false
  if (isAdminLike.value) return true
  const assignee = caseInfo.value.currentHandlerId
  return assignee != null && String(assignee) === String(currentUserId.value)
})

const canDispatcherReturnDept = computed(() => {
  if (!resolveAwaitingDispatcherForward(caseInfo.value)) return false
  if (!hasRole(RoleCode.DISPATCHER) && !isAdminLike.value) return false
  if (isAdminLike.value) return true
  const assignee = caseInfo.value.currentHandlerId
  return assignee != null && String(assignee) === String(currentUserId.value)
})

const canAcceptorReturnDispatcher = computed(() => {
  if (!canAcceptorCheckClose.value) return false
  if (acceptorQueueMode.value === 'verify') return false
  return true
})

const canDispatcherForwardAcceptor = computed(() => {
  if (!resolveAwaitingDispatcherForward(caseInfo.value)) {
    return false
  }
  if (!hasRole(RoleCode.DISPATCHER) && !isAdminLike.value) {
    return false
  }
  if (isAdminLike.value) {
    return true
  }
  const assignee = caseInfo.value.currentHandlerId
  return assignee != null && String(assignee) === String(currentUserId.value)
})

const canAcceptorCheckClose = computed(() => {
  if (
    caseInfo.value.awaitingDeptConfirm ||
    resolveAwaitingDispatcherForward(caseInfo.value)
  ) {
    return false
  }
  if (caseInfo.value.caseStatus !== 'pending_check' || isDeptOnlyOperator.value) {
    return false
  }
  if (isAdminLike.value || hasRole(RoleCode.SUPERVISOR)) {
    return true
  }
  return hasRole(RoleCode.ACCEPTOR) && acceptorCanCheckClose(caseInfo.value)
})

/** 立案人查看已批转他人收尾的案件时提示只读 */
const acceptorViewOnlyHint = computed(() => {
  if (isAdminLike.value || !hasRole(RoleCode.ACCEPTOR)) return ''
  const info = caseInfo.value
  if (!canAcceptorViewCaseInfo(info) || canAcceptorCheckClose.value) return ''
  const uid = currentUserId.value
  if (
    info.registerOperatorId != null &&
    String(info.registerOperatorId) === String(uid) &&
    info.caseStatus === 'pending_check'
  ) {
    const name = info.currentHandlerName || '其他受理员'
    return `您为本案立案人，核实/结案已由 ${name} 负责，此处仅可查看进度。`
  }
  return ''
})

const canClose = computed(() => {
  if (!canAcceptorCheckClose.value) return false
  return true
})
const canReject = computed(
  () =>
    ['pending_verify', 'pending_register', 'reported'].includes(caseInfo.value.caseStatus) &&
    (isAdminLike.value || acceptorCanOperate(caseInfo.value))
)

// 表单显示控制
const showDispatcherSelect = computed(() =>
  ['register', 'dept_confirm', 'acceptor_return_dispatcher'].includes(currentAction.value)
)
const showAcceptorSelect = computed(() =>
  ['dispatcher_forward', 'dispatcher_return_acceptor'].includes(currentAction.value)
)
const dispatcherSelectPlaceholder = computed(() =>
  currentAction.value === 'dept_confirm'
    ? '请选择派遣员（部门确认后批转给其把关）'
    : '请选择派遣员（立案后批转给其处理）'
)
const showDeptSelect = computed(() => currentAction.value === 'dispatch')
const showHandlerSelect = computed(() => currentAction.value === 'assign')
const showRegisterEdit = computed(() => currentAction.value === 'register')
const showAttachmentUpload = computed(() => currentAction.value === 'handle')

async function loadPageData(caseId) {
  if (caseId == null || caseId === '') {
    return
  }
  revokeBlobUrls(attachments.value.map((a) => a.url))
  destroyCaseMap()
  dialogVisible.value = false
  currentAction.value = ''
  caseInfo.value = {}
  attachments.value = []
  flowRecords.value = []

  await loadCaseDetail(caseId)
  await loadSceneAttachments(caseId)
  await loadCaseFlowRecords(caseId)
  await initCaseMap()
}

onMounted(async () => {
  await loadDeptTree()
  await loadPageData(route.params.id)
})

watch(
  () => route.params.id,
  (id, prevId) => {
    if (id != null && String(id) !== String(prevId ?? '')) {
      loadPageData(id)
    }
  }
)

onActivated(async () => {
  const routeId = route.params.id
  const dataId = caseInfo.value?.id
  if (dataId == null || String(dataId) !== String(routeId)) {
    return
  }
  await nextTick()
  if (!caseMap) {
    await initCaseMap()
    return
  }
  requestAnimationFrame(() => {
    try {
      caseMap?.resize()
    } catch {
      initCaseMap()
    }
  })
})

onBeforeUnmount(() => {
  destroyCaseMap()
  destroySendTaskMap()
  revokeBlobUrls(attachments.value.map((a) => a.url))
})

function destroyCaseMap() {
  if (caseMap) {
    try {
      caseMap.destroy()
    } catch {
      /* 已销毁则忽略 */
    }
    caseMap = null
    caseMarker = null
  }
  if (mapContainerRef.value) {
    mapContainerRef.value.innerHTML = ''
  }
}

async function initCaseMap() {
  await nextTick()
  await nextTick()
  const el = mapContainerRef.value
  if (!el || typeof window.AMap === 'undefined') {
    return
  }
  destroyCaseMap()
  const lng = Number(caseInfo.value.longitude)
  const lat = Number(caseInfo.value.latitude)
  const hasCoords = hasMapCoords.value
  try {
    caseMap = new window.AMap.Map(el, {
      zoom: hasCoords ? 16 : 12,
      center: hasCoords ? [lng, lat] : [111.0, 35.03],
      viewMode: '2D'
    })
    if (hasCoords) {
      caseMarker = new window.AMap.Marker({ position: [lng, lat] })
      caseMap.add(caseMarker)
    }
    requestAnimationFrame(() => {
      caseMap?.resize()
    })
  } catch (e) {
    console.error('初始化地图失败:', e)
  }
}

async function loadDeptTree() {
  try {
    const res = await getDeptTree()
    deptTree.value = res.data || []
  } catch (e) {
    console.error('加载部门树失败', e)
  }
}

async function loadHandlerOptions(deptId) {
  if (deptId == null) {
    handlerOptions.value = []
    return
  }
  try {
    const res = await getUserList({
      roleCode: 'HANDLER',
      departmentId: deptId,
      pageNum: 1,
      pageSize: 200,
      status: 1
    })
    handlerOptions.value = res.data?.records || []
  } catch (e) {
    console.error('加载处置人员列表失败', e)
    handlerOptions.value = []
  }
}

async function loadDispatcherOptions() {
  try {
    const res = await getUserList({
      roleCode: 'DISPATCHER',
      pageNum: 1,
      pageSize: 200,
      status: 1
    })
    dispatcherOptions.value = res.data?.records || []
  } catch (e) {
    console.error('加载派遣员列表失败', e)
    dispatcherOptions.value = []
  }
}

async function loadAcceptorOptions() {
  try {
    const res = await getUserList({
      roleCode: 'ACCEPTOR',
      pageNum: 1,
      pageSize: 200,
      status: 1
    })
    acceptorOptions.value = res.data?.records || []
  } catch (e) {
    console.error('加载受理员列表失败', e)
    acceptorOptions.value = []
  }
}

function acceptorOptionLabel(u) {
  const name = u.realName || u.username || ''
  const regId = caseInfo.value.registerOperatorId
  if (regId != null && String(u.id) === String(regId)) {
    return `${name}（本案立案人）`
  }
  return name
}

function resetProcessForm() {
  processForm.dispatcherUserId = null
  processForm.acceptorUserId = null
  processForm.handlerUserId = null
  processForm.departmentId = ''
  processForm.clientUpdateTime = null
  processForm.address = ''
  processForm.description = ''
  processForm.conditionDesc = ''
  processForm.longitude = ''
  processForm.latitude = ''
  processForm.checkResult = ''
  processForm.remark = ''
  processForm.attachments = []
}

function openSendTaskDialog(type) {
  sendTaskType.value = type
  sendTaskRemark.value = ''
  selectedCollectorId.value = null
  collectorCandidates.value = []
  sendTaskDialogVisible.value = true
}

function handleSendCheck() {
  openSendTaskDialog('check')
}

function handleSendVerifyTask() {
  openSendTaskDialog('verify')
}

async function onSendTaskDialogOpened() {
  const caseId = caseInfo.value.id
  if (!caseId) return
  sendTaskLoading.value = true
  try {
    const res = await getCollectorCandidates(caseId)
    collectorCandidates.value = res.data || []
    const rec = collectorCandidates.value.find((c) => c.recommended)
    selectedCollectorId.value = rec?.userId ?? collectorCandidates.value[0]?.userId ?? null
    await nextTick()
    await initSendTaskMap()
  } catch (e) {
    console.error(e)
    ElMessage.error('加载采集员列表失败')
  } finally {
    sendTaskLoading.value = false
  }
}

function onCollectorRowChange(row) {
  if (row?.userId != null) {
    selectedCollectorId.value = row.userId
    focusCollectorOnMap(row)
  }
}

function focusCollectorOnMap(row) {
  if (!sendTaskMap || !row?.longitude || !row?.latitude) return
  try {
    sendTaskMap.setCenter([Number(row.longitude), Number(row.latitude)])
  } catch {
    /* ignore */
  }
}

function destroySendTaskMap() {
  if (sendTaskMap) {
    try {
      sendTaskMap.destroy()
    } catch {
      /* ignore */
    }
    sendTaskMap = null
  }
  sendTaskMarkers.length = 0
  if (sendTaskMapRef.value) {
    sendTaskMapRef.value.innerHTML = ''
  }
}

async function initSendTaskMap() {
  await nextTick()
  const el = sendTaskMapRef.value
  if (!el || typeof window.AMap === 'undefined') return
  destroySendTaskMap()
  const lng = Number(caseInfo.value.longitude)
  const lat = Number(caseInfo.value.latitude)
  const hasCase = hasMapCoords.value
  try {
    sendTaskMap = new window.AMap.Map(el, {
      zoom: hasCase ? 14 : 12,
      center: hasCase ? [lng, lat] : [111.0, 35.03],
      viewMode: '2D'
    })
    if (hasCase) {
      const caseMk = new window.AMap.Marker({
        position: [lng, lat],
        title: '案发位置'
      })
      sendTaskMap.add(caseMk)
      sendTaskMarkers.push(caseMk)
    }
    for (const c of collectorCandidates.value) {
      if (c.longitude == null || c.latitude == null) continue
      const mk = new window.AMap.Marker({
        position: [Number(c.longitude), Number(c.latitude)],
        title: c.realName || c.username || '采集员'
      })
      sendTaskMap.add(mk)
      sendTaskMarkers.push(mk)
    }
    requestAnimationFrame(() => sendTaskMap?.resize())
  } catch (e) {
    console.error('下发任务地图初始化失败', e)
  }
}

async function submitSendTask() {
  const caseId = caseInfo.value.id
  if (!caseId) return
  if (!selectedCollectorId.value) {
    ElMessage.warning('请选择采集员')
    return
  }
  sendTaskSubmitting.value = true
  try {
    const payload = {
      caseId,
      collectorUserId: selectedCollectorId.value,
      remark: sendTaskRemark.value?.trim() || undefined
    }
    if (sendTaskType.value === 'check') {
      await sendCheckTask(payload)
      ElMessage.success('已下发核查任务，采集员完成后可立案或作废')
    } else {
      await sendVerifyTask(payload)
      ElMessage.success('已下发核实任务，采集员完成后您可继续结案或返工')
    }
    sendTaskDialogVisible.value = false
    await loadCaseDetail(caseId)
    await loadCaseFlowRecords(caseId)
  } catch (e) {
    console.error(e)
  } finally {
    sendTaskSubmitting.value = false
  }
}

function parseOptionalCoord(raw) {
  if (raw === '' || raw === null || raw === undefined) return null
  const n = Number(raw)
  return Number.isFinite(n) ? n : NaN
}

async function uploadProcessFile(options) {
  try {
    const formData = new FormData()
    formData.append('file', options.file)
    const res = await request({
      url: '/file/upload',
      method: 'post',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const url = typeof res.data === 'string' ? res.data : ''
    options.onSuccess(res, options.file)
    if (url) {
      options.file.url = url
    }
  } catch (e) {
    options.onError(e)
  }
}

function normalizeUploadUrls(list) {
  if (!list?.length) return []
  return list
    .map((f) => {
      if (typeof f === 'string') return f
      if (f.url) return f.url
      const d = f.response?.data
      if (typeof d === 'string') return d
      return ''
    })
    .filter(Boolean)
}

async function enrichRespGridDisplay() {
  if (caseInfo.value.respGridName) {
    return
  }
  const gridId = caseInfo.value.respGridId
  if (gridId != null) {
    try {
      const g = await getRespGridDetail(gridId)
      if (g.data?.respGridName) {
        caseInfo.value.respGridName = g.data.respGridName
        return
      }
    } catch (e) {
      console.warn('按片区ID补全名称失败', gridId, e)
    }
  }
  const lng = caseInfo.value.longitude
  const lat = caseInfo.value.latitude
  if (lng == null || lat == null) {
    return
  }
  try {
    const listRes = await getRespGridList()
    for (const grid of listRes.data || []) {
      if (!grid.id) continue
      const hit = await checkRespGridLocation(grid.id, lng, lat)
      if (hit.data === true) {
        caseInfo.value.respGridName = grid.respGridName
        break
      }
    }
  } catch (e) {
    console.warn('按坐标反查责任片区失败', e)
  }
}

async function loadTimeoutAppeal(caseId) {
  try {
    const res = await getTimeoutAppealByCase(caseId)
    timeoutAppeal.value = res.data || null
  } catch (_) {
    timeoutAppeal.value = null
  }
}

function openTimeoutAppealDialog() {
  timeoutAppealForm.appealDesc = ''
  timeoutAppealFileList.value = []
  timeoutAppealDialogVisible.value = true
}

async function uploadAppealFile(options) {
  try {
    const formData = new FormData()
    formData.append('file', options.file)
    const res = await request({
      url: '/file/upload',
      method: 'post',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const url = typeof res.data === 'string' ? res.data : ''
    options.onSuccess(res, options.file)
    if (url) options.file.url = url
  } catch (e) {
    options.onError(e)
  }
}

async function submitTimeoutAppeal() {
  if (!timeoutAppealForm.appealDesc.trim()) {
    ElMessage.warning('请填写申诉说明')
    return
  }
  timeoutAppealSubmitting.value = true
  try {
    await apiSubmitTimeoutAppeal({
      caseId: caseInfo.value.id,
      appealDesc: timeoutAppealForm.appealDesc.trim(),
      attachmentPaths: normalizeUploadUrls(timeoutAppealFileList.value)
    })
    ElMessage.success('申诉已提交')
    timeoutAppealDialogVisible.value = false
    await loadTimeoutAppeal(caseInfo.value.id)
    await loadCaseDetail(caseInfo.value.id)
  } finally {
    timeoutAppealSubmitting.value = false
  }
}

async function loadCaseDetail(id) {
  loading.value = true
  try {
    const res = await getCaseDetail(id)
    caseInfo.value = res.data || {}
    await enrichRespGridDisplay()
    await loadTimeoutAppeal(id)
    try {
      const adj = await getCaseAdjustmentList(id)
      adjustmentRecords.value = adj.data || []
    } catch (_) {
      adjustmentRecords.value = []
    }
  } catch (error) {
    console.error('获取案件详情失败:', error)
  } finally {
    loading.value = false
  }
}

function formatAdjustmentDate(val) {
  if (!val) return '—'
  return String(val).substring(0, 10)
}

function openAdjustmentReview(row, approved) {
  if (!row?.id) return
  adjustmentReviewRow.value = row
  adjustmentReviewApproved.value = approved
  adjustmentReviewRemark.value = ''
  adjustmentReviewVisible.value = true
}

async function submitAdjustmentReview() {
  if (!adjustmentReviewApproved.value && !adjustmentReviewRemark.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  adjustmentReviewSubmitting.value = true
  try {
    await reviewCaseAdjustment({
      applyId: adjustmentReviewRow.value.id,
      approved: adjustmentReviewApproved.value,
      reviewRemark: adjustmentReviewRemark.value
    })
    ElMessage.success(adjustmentReviewApproved.value ? '已批准' : '已驳回')
    adjustmentReviewVisible.value = false
    await loadCaseDetail(caseInfo.value.id)
    await loadCaseFlowRecords(caseInfo.value.id)
    window.dispatchEvent(new CustomEvent('cityguard:refresh-lists'))
  } catch (e) {
    console.error(e)
  } finally {
    adjustmentReviewSubmitting.value = false
  }
}

function disableSuspendDate(date) {
  const d = new Date(date)
  d.setHours(0, 0, 0, 0)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const max = new Date(today)
  max.setDate(max.getDate() + 365)
  return d < today || d > max
}

function openAdjustmentApply(type) {
  adjustmentForm.applyType = type
  adjustmentForm.reason = ''
  adjustmentForm.suspendUntil = ''
  adjustmentDialogVisible.value = true
}

async function submitAdjustmentApply() {
  if (!adjustmentForm.reason.trim()) {
    ElMessage.warning('请填写申请原因')
    return
  }
  if (adjustmentForm.applyType === 'suspend' && !adjustmentForm.suspendUntil) {
    ElMessage.warning('请选择挂账截止日期')
    return
  }
  adjustmentSubmitting.value = true
  try {
    await applyCaseAdjustment({
      caseId: caseInfo.value.id,
      applyType: adjustmentForm.applyType,
      reason: adjustmentForm.reason.trim(),
      suspendUntil: adjustmentForm.applyType === 'suspend' ? adjustmentForm.suspendUntil : undefined,
      clientUpdateTime: caseInfo.value.updateTime
    })
    ElMessage.success('申请已提交，等待派遣员审批')
    adjustmentDialogVisible.value = false
    await loadCaseDetail(caseInfo.value.id)
    await loadCaseFlowRecords(caseInfo.value.id)
  } catch (e) {
    console.error(e)
  } finally {
    adjustmentSubmitting.value = false
  }
}

async function buildScenePreviewItem(a, key, label) {
  const ft = (a.fileType || '').toLowerCase()
  const rawPath = a.filePath || a.fileUrl || ''
  const isImage =
    ft === 'image' ||
    ft.includes('image') ||
    /\.(jpe?g|png|gif|webp|bmp)$/i.test(rawPath || a.fileName || '')
  let url = ''
  if (rawPath) {
    try {
      url = await fetchFilePreviewBlobUrl(rawPath)
    } catch (e) {
      console.error('加载附件预览失败:', rawPath, e)
    }
    if (!url && /^https?:\/\//i.test(rawPath)) {
      url = rawPath
    }
  }
  return {
    key,
    id: a.id,
    type: isImage ? 'image' : 'video',
    url,
    filePath: rawPath,
    label: label || ''
  }
}

async function loadSceneAttachments(id) {
  revokeBlobUrls(attachments.value.map((a) => a.url))
  const items = []
  try {
    const res = await getCaseAttachments(id)
    for (const a of res.data || []) {
      items.push(await buildScenePreviewItem(a, `case-${a.id}`, ''))
    }
  } catch (error) {
    console.error('获取案件附件失败:', error)
  }
  try {
    const chkRes = await getCheckTaskRecords(id)
    for (const rec of chkRes.data || []) {
      for (const a of rec.attachments || []) {
        items.push(await buildScenePreviewItem(a, `chk-${rec.id}-${a.id}`, '核查照片'))
      }
    }
  } catch (error) {
    console.error('获取核查照片失败:', error)
  }
  try {
    const vfyRes = await getVerifyTaskRecords(id)
    for (const rec of vfyRes.data || []) {
      for (const a of rec.attachments || []) {
        items.push(await buildScenePreviewItem(a, `vfy-${rec.id}-${a.id}`, '核实照片'))
      }
    }
  } catch (error) {
    console.error('获取核实照片失败:', error)
  }
  attachments.value = items.filter((item) => item.url)
}

async function loadCaseFlowRecords(id) {
  try {
    const res = await getCaseFlowRecords(id)
    flowRecords.value = res.data || []
  } catch (error) {
    console.error('获取流程记录失败:', error)
  }
}

async function handleRegister() {
  const id = caseInfo.value.id
  if (id) {
    await loadCaseDetail(id)
  }
  if (!REGISTERABLE_STATUSES.includes(caseInfo.value.caseStatus)) {
    ElMessage.warning('案件状态已变更，无法立案（可能已被他人处理），请刷新或返回列表')
    return
  }
  resetProcessForm()
  const c = caseInfo.value
  processForm.address = c.address ?? ''
  processForm.description = c.description ?? ''
  processForm.conditionDesc = c.conditionDesc ?? ''
  processForm.longitude = c.longitude != null ? String(c.longitude) : ''
  processForm.latitude = c.latitude != null ? String(c.latitude) : ''
  await loadDispatcherOptions()
  currentAction.value = 'register'
  dialogTitle.value = '立案并批转派遣员'
  dialogVisible.value = true
}

function handleDispatch() {
  resetProcessForm()
  processForm.clientUpdateTime = caseInfo.value.updateTime ?? null
  currentAction.value = 'dispatch'
  dialogTitle.value = '派遣至处置部门'
  dialogVisible.value = true
}

async function handleAssignHandler() {
  resetProcessForm()
  processForm.clientUpdateTime = caseInfo.value.updateTime ?? null
  await loadHandlerOptions(caseInfo.value.handleDeptId)
  currentAction.value = 'assign'
  dialogTitle.value = '指派处置人员'
  dialogVisible.value = true
}

function handleHandle() {
  resetProcessForm()
  currentAction.value = 'handle'
  dialogTitle.value = '提交处置结果'
  dialogVisible.value = true
}

async function handleDeptConfirm() {
  resetProcessForm()
  processForm.clientUpdateTime = caseInfo.value.updateTime ?? null
  await loadDispatcherOptions()
  currentAction.value = 'dept_confirm'
  dialogTitle.value = '批转派遣员把关'
  dialogVisible.value = true
}

function handleDeptReturn() {
  resetProcessForm()
  processForm.clientUpdateTime = caseInfo.value.updateTime ?? null
  currentAction.value = 'dept_return'
  dialogTitle.value = '回退至派遣员'
  dialogVisible.value = true
}

function handleDeptRevokeAssign() {
  resetProcessForm()
  processForm.clientUpdateTime = caseInfo.value.updateTime ?? null
  currentAction.value = 'dept_revoke_assign'
  dialogTitle.value = '撤销指派'
  dialogVisible.value = true
}

function handleDeptReturnHandler() {
  resetProcessForm()
  processForm.clientUpdateTime = caseInfo.value.updateTime ?? null
  currentAction.value = 'dept_return_handler'
  dialogTitle.value = '打回处置人员再处置'
  dialogVisible.value = true
}

async function handleDispatcherReturnAcceptor() {
  resetProcessForm()
  processForm.clientUpdateTime = caseInfo.value.updateTime ?? null
  await loadAcceptorOptions()
  const regId = caseInfo.value.registerOperatorId
  if (regId != null && acceptorOptions.value.some((u) => String(u.id) === String(regId))) {
    processForm.acceptorUserId = regId
  }
  currentAction.value = 'dispatcher_return_acceptor'
  dialogTitle.value = '回退受理员（非本局）'
  dialogVisible.value = true
}

function handleDispatcherReturnDept() {
  resetProcessForm()
  processForm.clientUpdateTime = caseInfo.value.updateTime ?? null
  currentAction.value = 'dispatcher_return_dept'
  dialogTitle.value = '返工处置部门'
  dialogVisible.value = true
}

async function handleAcceptorReturnDispatcher() {
  resetProcessForm()
  processForm.clientUpdateTime = caseInfo.value.updateTime ?? null
  await loadDispatcherOptions()
  currentAction.value = 'acceptor_return_dispatcher'
  dialogTitle.value = '回退派遣员返工'
  dialogVisible.value = true
}

async function handleDispatcherForwardAcceptor() {
  resetProcessForm()
  processForm.clientUpdateTime = caseInfo.value.updateTime ?? null
  await loadAcceptorOptions()
  const regId = caseInfo.value.registerOperatorId
  if (regId != null && acceptorOptions.value.some((u) => String(u.id) === String(regId))) {
    processForm.acceptorUserId = regId
  }
  currentAction.value = 'dispatcher_forward'
  dialogTitle.value = '批转受理员'
  dialogVisible.value = true
}

function handleClose() {
  resetProcessForm()
  currentAction.value = 'close'
  dialogTitle.value = '结案'
  dialogVisible.value = true
}

function handleReject() {
  resetProcessForm()
  currentAction.value = 'reject'
  dialogTitle.value = '作废'
  dialogVisible.value = true
}

async function submitProcess() {
  const cid = caseInfo.value.id
  const remark = processForm.remark || ''
  const attUrls = normalizeUploadUrls(processForm.attachments)

  try {
    switch (currentAction.value) {
      case 'register': {
        if (!processForm.dispatcherUserId) {
          ElMessage.warning('请选择派遣员')
          return
        }
        const lng = parseOptionalCoord(processForm.longitude)
        const lat = parseOptionalCoord(processForm.latitude)
        if (processForm.longitude !== '' && Number.isNaN(lng)) {
          ElMessage.warning('经度须为有效数字')
          return
        }
        if (processForm.latitude !== '' && Number.isNaN(lat)) {
          ElMessage.warning('纬度须为有效数字')
          return
        }
        await registerCase({
          caseId: cid,
          dispatcherUserId: Number(processForm.dispatcherUserId),
          remark,
          clientUpdateTime: caseInfo.value.updateTime,
          address: processForm.address,
          description: processForm.description,
          conditionDesc: processForm.conditionDesc,
          longitude: lng === null || Number.isNaN(lng) ? null : lng,
          latitude: lat === null || Number.isNaN(lat) ? null : lat
        })
        break
      }
      case 'dispatch': {
        if (!processForm.departmentId) {
          ElMessage.warning('请选择处置部门')
          return
        }
        await dispatchCase({
          caseId: cid,
          departmentId: Number(processForm.departmentId),
          remark,
          clientUpdateTime: processForm.clientUpdateTime ?? caseInfo.value.updateTime
        })
        break
      }
      case 'assign': {
        if (!processForm.handlerUserId) {
          ElMessage.warning('请选择处置人员')
          return
        }
        await assignHandlerCase({
          caseId: cid,
          handlerUserId: Number(processForm.handlerUserId),
          remark,
          clientUpdateTime: processForm.clientUpdateTime ?? caseInfo.value.updateTime
        })
        break
      }
      case 'handle':
        await handleCase({ caseId: cid, remark, attachments: attUrls })
        break
      case 'dept_confirm': {
        if (!processForm.dispatcherUserId) {
          ElMessage.warning('请选择派遣员')
          return
        }
        await deptConfirmCase({
          caseId: cid,
          dispatcherUserId: Number(processForm.dispatcherUserId),
          remark,
          clientUpdateTime: processForm.clientUpdateTime ?? caseInfo.value.updateTime
        })
        break
      }
      case 'dept_return':
        if (!remark) {
          ElMessage.warning('请填写回退原因')
          return
        }
        await deptReturnCase({
          caseId: cid,
          remark,
          clientUpdateTime: processForm.clientUpdateTime ?? caseInfo.value.updateTime
        })
        break
      case 'dept_revoke_assign':
        await deptRevokeAssign({
          caseId: cid,
          remark,
          clientUpdateTime: processForm.clientUpdateTime ?? caseInfo.value.updateTime
        })
        break
      case 'dept_return_handler':
        if (!remark) {
          ElMessage.warning('请填写打回原因')
          return
        }
        await deptReturnHandler({
          caseId: cid,
          remark,
          clientUpdateTime: processForm.clientUpdateTime ?? caseInfo.value.updateTime
        })
        break
      case 'dispatcher_return_acceptor':
        if (!remark) {
          ElMessage.warning('请填写回退原因')
          return
        }
        await dispatcherReturnAcceptor({
          caseId: cid,
          acceptorUserId: processForm.acceptorUserId ? Number(processForm.acceptorUserId) : null,
          remark,
          clientUpdateTime: processForm.clientUpdateTime ?? caseInfo.value.updateTime
        })
        break
      case 'dispatcher_return_dept':
        if (!remark) {
          ElMessage.warning('请填写返工说明')
          return
        }
        await dispatcherReturnDept({
          caseId: cid,
          remark,
          clientUpdateTime: processForm.clientUpdateTime ?? caseInfo.value.updateTime
        })
        break
      case 'acceptor_return_dispatcher':
        if (!remark) {
          ElMessage.warning('请填写回退原因')
          return
        }
        if (!processForm.dispatcherUserId) {
          ElMessage.warning('请选择当班派遣员')
          return
        }
        await acceptorReturnDispatcher({
          caseId: cid,
          dispatcherUserId: Number(processForm.dispatcherUserId),
          remark,
          clientUpdateTime: processForm.clientUpdateTime ?? caseInfo.value.updateTime
        })
        break
      case 'dispatcher_forward': {
        if (!processForm.acceptorUserId) {
          ElMessage.warning('请选择受理员')
          return
        }
        await dispatcherForwardToAcceptor({
          caseId: cid,
          acceptorUserId: Number(processForm.acceptorUserId),
          remark,
          clientUpdateTime: processForm.clientUpdateTime ?? caseInfo.value.updateTime
        })
        break
      }
      case 'close':
        await closeCase({ caseId: cid, remark })
        break
      case 'reject': {
        if (!remark) {
          ElMessage.warning('请填写作废原因')
          return
        }
        await rejectCase({ caseId: cid, reason: remark })
        break
      }
      default:
        return
    }

    const action = currentAction.value
    const msgMap = {
      register: '立案成功，已批转派遣员',
      dispatch: '已派遣至处置部门',
      assign: '已指派处置人员',
      handle: '处置结果已提交，待部门确认',
      dept_confirm: '已确认并批转派遣员',
      dept_return: '已回退至派遣员',
      dept_revoke_assign: '已撤销指派',
      dept_return_handler: '已打回处置人员',
      dispatcher_return_acceptor: '已回退受理员',
      dispatcher_return_dept: '已打回处置部门返工',
      acceptor_return_dispatcher: '已回退派遣员，由原部门返工',
      dispatcher_forward: '已批转受理员'
    }
    ElMessage.success(msgMap[action] || '操作成功')
    dialogVisible.value = false
    await loadCaseDetail(cid)
    await loadCaseFlowRecords(cid)
    await loadSceneAttachments(cid)
  } catch (error) {
    console.error('操作失败:', error)
  }
}

</script>

<style lang="scss" scoped>
.adjustment-review-summary {
  margin: 0 0 8px;
  font-weight: 600;
  color: #303133;
}

.adjustment-review-reason {
  margin: 0 0 12px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.exempt-inline-tag {
  margin-left: 6px;
  vertical-align: middle;
}
.appeal-dialog-hint {
  margin-bottom: 12px;
}
.appeal-form {
  margin-top: 8px;
}
.adjustment-form-tip {
  margin: 8px 0 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.case-detail {
  .view-only-hint {
    margin-bottom: 16px;
  }

  .info-card {
    margin-bottom: 20px;

    .header-with-action {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .actions {
        display: flex;
        gap: 10px;
      }
    }
  }

  .map-card {
    margin-bottom: 20px;

    .map-container {
      width: 100%;
      height: 400px;
      min-height: 240px;
    }
  }

  .attachment-card {
    margin-bottom: 20px;

    .attachment-item-wrap {
      margin-bottom: 8px;
    }

    .attachment-caption {
      margin: 6px 0 0;
      font-size: 12px;
      color: var(--el-text-color-secondary);
      text-align: center;
      line-height: 1.4;
    }

    .attachment-image, .attachment-video {
      width: 100%;
      height: 150px;
      object-fit: cover;
    }
  }

  .flow-card {
    .flow-image {
      width: 80px;
      height: 80px;
    }
  }

  .send-task-tip {
    margin: 0 0 12px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
    line-height: 1.5;
  }

  .send-task-map {
    width: 100%;
    height: 220px;
    margin-bottom: 12px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 4px;
  }

  .collector-table {
    margin-bottom: 8px;
  }

  .send-task-form {
    margin-top: 8px;
  }

  .ml-4 {
    margin-left: 4px;
  }

}
</style>