<template>
  <div class="handle-detail-page">
    <van-nav-bar title="案件处置" left-arrow @click-left="goBack" />

    <van-notice-bar
      v-if="handlerDeptNoticeText"
      wrapable
      color="#1989fa"
      background="#ecf9ff"
      left-icon="info-o"
      :text="handlerDeptNoticeText"
    />

    <van-notice-bar
      v-if="pendingAdjustmentTip && (caseInfo.hasPendingExtension || caseInfo.hasPendingSuspend)"
      color="#1989fa"
      background="#ecf9ff"
      :text="pendingAdjustmentTip"
    />

    <van-notice-bar
      v-if="handleTimer.show && caseInfo.caseStatus === 'handling'"
      wrapable
      :color="handleTimer.overdue ? '#ee0a24' : '#ed6a0c'"
      :background="handleTimer.overdue ? '#fff1f0' : '#fff7e8'"
      left-icon="clock-o"
      :text="handleTimerBannerText"
    />

    <van-cell-group v-if="handleTimer.show" title="处置时限" inset>
      <van-cell v-if="handleTimer.limitLabel" title="时限规则" :value="handleTimer.limitLabel" />
      <van-cell title="截止时间" :value="handleTimer.deadlineText || '—'" />
      <van-cell title="剩余时间">
        <template #value>
          <span :class="{ 'text-overdue': handleTimer.overdue }">{{ handleTimer.remainingText || '—' }}</span>
        </template>
      </van-cell>
    </van-cell-group>

    <van-cell-group title="案件信息" inset>
      <van-cell title="案件编号" :value="caseInfo.caseCode" />
      <van-cell title="案件状态" :value="statusLabel" />
      <van-cell title="大类" :value="caseInfo.bigName" />
      <van-cell title="小类" :value="caseInfo.smallName" />
      <van-cell title="处置部门" :value="caseInfo.handleDeptName" />
      <van-cell title="发生地址" :value="caseInfo.address" />
      <van-cell title="问题描述" :value="caseInfo.description" />
    </van-cell-group>

    <van-cell-group title="位置" inset>
      <CaseLocationMap
        :key="locationMapKey"
        :longitude="caseInfo.longitude"
        :latitude="caseInfo.latitude"
        :address="caseInfo.address"
      />
    </van-cell-group>

    <van-cell-group v-if="flowRecords.length" title="流转记录" inset>
      <div class="flow-timeline">
        <div
          v-for="(item, index) in flowRecords"
          :key="item.id"
          class="flow-item"
        >
          <div class="flow-dot" :class="{ 'flow-dot--latest': index === flowRecords.length - 1 }" />
          <div class="flow-content">
            <div class="flow-header">
              <span class="flow-node">{{ item.nodeName || '—' }}</span>
              <span class="flow-time">{{ formatFlowTime(item.operateTime) }}</span>
            </div>
            <div class="flow-meta">
              <span>操作人：{{ item.operatorName || '—' }}</span>
              <span v-if="formatFlowReceiver(item)"> · 接收人：{{ formatFlowReceiver(item) }}</span>
            </div>
            <div v-if="item.operateOpinion?.trim()" class="flow-opinion">
              备注：{{ item.operateOpinion.trim() }}
            </div>
          </div>
        </div>
      </div>
    </van-cell-group>

    <van-cell-group v-if="imagesLoading" title="现场照片" inset>
      <van-loading class="img-loading" size="24px">加载照片中...</van-loading>
    </van-cell-group>
    <template v-for="group in stagePhotoGroups" :key="group.nodeCode">
      <van-cell-group :title="group.label" inset>
        <div class="handle-photo-scroll">
          <div
            v-for="item in group.items"
            :key="item.url"
            class="handle-photo-batch"
          >
            <p class="handle-photo-batch-time">{{ item.time }}</p>
            <div class="handle-photo-batch-images">
              <van-image
                :src="item.url"
                fit="cover"
                width="112"
                height="84"
                class="handle-photo-thumb"
                @click="previewSingleImage(item.url)"
              />
            </div>
          </div>
        </div>
      </van-cell-group>
    </template>
    <van-empty v-if="!imagesLoading && stagePhotoGroups.length === 0" description="暂无现场照片" image-size="60" />

    <template v-if="canSubmit">
      <van-cell-group title="处置说明" inset>
        <van-field
          v-model="remark"
          rows="3"
          autosize
          type="textarea"
          maxlength="500"
          show-word-limit
          placeholder="请填写现场处置情况..."
        />
      </van-cell-group>

      <van-cell-group title="处置照片" inset>
        <van-uploader v-model="fileList" multiple :max-count="6" :after-read="afterRead" />
      </van-cell-group>

      <div class="submit-btn">
        <van-button round block type="primary" :loading="submitting" @click="submit">
          提交处置结果
        </van-button>
        <van-button
          v-if="canApplyExtension"
          round
          block
          plain
          type="primary"
          class="return-btn"
          :loading="extensionSubmitting"
          @click="openExtensionDialog('extension')"
        >
          申请延期
        </van-button>
        <van-button
          v-if="canApplySuspend"
          round
          block
          plain
          type="primary"
          class="return-btn"
          :loading="extensionSubmitting"
          @click="openExtensionDialog('suspend')"
        >
          申请挂账
        </van-button>
        <van-button
          round
          block
          plain
          type="warning"
          class="return-btn"
          :loading="returning"
          @click="openReturnDialog"
        >
          回退处置部门
        </van-button>
      </div>
    </template>
    <van-notice-bar v-else-if="caseInfo.caseStatus === 'handling'" color="#ed6a0c" background="#fffbe8" text="该案件未指派给您，无法提交处置结果" />

    <van-dialog
      v-model:show="extensionDialogVisible"
      :title="extensionDialogTitle"
      show-cancel-button
      :before-close="onExtensionDialogBeforeClose"
    >
      <van-field
        v-model="extensionReason"
        rows="3"
        autosize
        type="textarea"
        maxlength="500"
        placeholder="请说明原因（必填）"
      />
      <van-field
        v-if="extensionApplyType === 'suspend'"
        v-model="extensionSuspendUntil"
        label="挂账截止"
        placeholder="选择日期"
        readonly
        is-link
        @click="showSuspendDatePicker = true"
      />
      <p class="extension-tip">{{ extensionDialogTip }}</p>
    </van-dialog>
    <van-popup v-model:show="showSuspendDatePicker" position="bottom">
      <van-date-picker
        title="挂账截止日期"
        :min-date="suspendMinDate"
        :max-date="suspendMaxDate"
        @confirm="onSuspendDateConfirm"
        @cancel="showSuspendDatePicker = false"
      />
    </van-popup>

    <van-dialog
      v-model:show="returnDialogVisible"
      title="回退处置部门"
      show-cancel-button
      :before-close="onReturnDialogBeforeClose"
    >
      <van-field
        v-model="returnRemark"
        rows="3"
        autosize
        type="textarea"
        maxlength="500"
        placeholder="请填写回退原因（必填）"
      />
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showImagePreview, showLoadingToast, closeToast } from 'vant'
import {
  extractRequestErrorMessage,
  showAppFailToast,
  showAppSuccessToast,
  showAppToast
} from '@/utils/toastFeedback'
import {
  getCaseDetail,
  getCaseAttachments,
  getCaseFlowRecords,
  handleCase,
  handlerReturnDept,
  applyCaseAdjustment,
  uploadFile
} from '@/api/case'
import { useUserStore } from '@/stores/user'
import CaseLocationMap from '@/components/CaseLocationMap.vue'
import { fetchFilePreviewBlobUrl, revokeBlobUrls } from '@/utils/fileUrl'
import { buildHandleTimerDisplay, isHandleStageOverdue } from '@/utils/caseTimer'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const caseInfo = ref({})
const locationMapKey = ref(0)
const stagePhotoGroups = ref([])
const imagesLoading = ref(false)
const flowRecords = ref([])
const remark = ref('')
const fileList = ref([])
const attachmentUrls = ref([])
const submitting = ref(false)
const returning = ref(false)
const returnDialogVisible = ref(false)
const returnRemark = ref('')
const extensionDialogVisible = ref(false)
const extensionApplyType = ref('extension')
const extensionReason = ref('')
const extensionSuspendUntil = ref('')
const extensionSubmitting = ref(false)
const showSuspendDatePicker = ref(false)

const suspendMinDate = new Date()
const suspendMaxDate = (() => {
  const d = new Date()
  d.setFullYear(d.getFullYear() + 1)
  return d
})()

const handleTimer = computed(() => buildHandleTimerDisplay(caseInfo.value))

const handleTimerBannerText = computed(() => {
  if (!handleTimer.value.show) return ''
  const parts = []
  if (handleTimer.value.remainingText) parts.push(handleTimer.value.remainingText)
  if (handleTimer.value.deadlineText) parts.push(`截止 ${handleTimer.value.deadlineText}`)
  return parts.join(' · ')
})

const statusLabel = computed(() => {
  const map = {
    handling: '处置中',
    handle_finish: '待部门确认',
    pending_check: '待核实',
    checking: '核查中',
    check_pass: '核查通过',
    pending_close: '待结案',
    closed: '已结案',
    forced_close: '已结案',
    pending_handle: '待处置',
    returned: '已回退'
  }
  return map[caseInfo.value.caseStatus] || caseInfo.value.caseStatus || '—'
})

const canSubmit = computed(() => {
  if (caseInfo.value.caseStatus !== 'handling') return false
  const uid = userStore.userInfo?.id ?? userStore.userInfo?.userId
  if (!uid) return false
  const assignee = caseInfo.value.currentHandlerId
  return assignee != null && String(assignee) === String(uid)
})

const handlerDeptNoticeText = computed(() => {
  if (caseInfo.value.pendingDeptExtensionApply || caseInfo.value.pendingDeptSuspendApply) {
    return ''
  }
  const n = caseInfo.value.handlerDeptNotice
  if (!n?.content?.trim()) return ''
  const title = n.title || '部门提示'
  return `${title}：${n.content.trim()}`
})

const pendingAdjustmentTip = computed(() => {
  if (caseInfo.value.hasPendingExtension && caseInfo.value.hasPendingSuspend) {
    return '延期/挂账申请待部门或派遣员审批，审批前请继续按原时限处置'
  }
  if (caseInfo.value.pendingDeptExtensionApply || caseInfo.value.pendingDeptSuspendApply) {
    return '已提交至处置部门审核，部门同意后将报送派遣员审批'
  }
  return '申请审批中，审批前请继续按原时限处置'
})

const isHandleOverdue = computed(() => isHandleStageOverdue(caseInfo.value))

const canApplyExtension = computed(() => {
  if (!canSubmit.value) return false
  if (isHandleOverdue.value) return false
  if (caseInfo.value.isSuspended === 1) return false
  if (caseInfo.value.hasPendingExtension) return false
  const count = caseInfo.value.extensionApprovedCount || 0
  return count < 2
})

const canApplySuspend = computed(() => {
  if (!canSubmit.value) return false
  if (isHandleOverdue.value) return false
  if (caseInfo.value.isSuspended === 1) return false
  if (caseInfo.value.hasPendingSuspend) return false
  if (caseInfo.value.suspendEverApproved) return false
  return true
})

const extensionDialogTitle = computed(() =>
  extensionApplyType.value === 'suspend' ? '申请挂账' : '申请延期'
)

const extensionDialogTip = computed(() =>
  extensionApplyType.value === 'suspend'
    ? '提交至本部门审核；部门可驳回（如协调解决）或同意报送派遣员审批'
    : '提交至本部门审核；部门同意后将报送派遣员，批准后在当前截止时间上延长一个处置时限'
)

function attachmentUrl(a) {
  return a.filePath || a.url || ''
}

function isImageAttachment(a) {
  if (a.fileType === 'image') return true
  const path = attachmentUrl(a) || a.fileName || ''
  return /\.(jpe?g|png|gif|webp|bmp)$/i.test(path)
}

const stageNameMap = {
  reported: '上报阶段',
  handle_finish: '处置阶段',
  pending_dispatch: '立案阶段',
  handling: '指派阶段',
  pending_check: '核查阶段',
  checking: '核查阶段',
  closed: '结案阶段',
  returned: '回退阶段'
}

function getStageLabel(code) {
  return stageNameMap[code] || code || '其他'
}

function formatUploadTime(value) {
  if (!value) return '—'
  const s = String(value)
  return s.length >= 19 ? s.slice(0, 19).replace('T', ' ') : s.replace('T', ' ')
}

function parseUploadTime(value) {
  if (!value) return 0
  const t = new Date(String(value).replace(' ', 'T')).getTime()
  return Number.isNaN(t) ? 0 : t
}

async function loadImagePreviewUrl(a) {
  const raw = attachmentUrl(a)
  if (!raw) return ''
  try {
    const displayUrl = await fetchFilePreviewBlobUrl(raw)
    if (displayUrl) return displayUrl
    if (raw.startsWith('http://') || raw.startsWith('https://')) return raw
  } catch (e) {
    console.warn('加载附件预览失败:', raw, e)
  }
  return ''
}

function revokeStagePhotos() {
  for (const group of stagePhotoGroups.value) {
    for (const item of group.items) {
      if (item.url?.startsWith('blob:')) revokeBlobUrls([item.url])
    }
  }
}

async function loadStagePhotos(attachments) {
  revokeStagePhotos()
  stagePhotoGroups.value = []
  const imageAttachments = (attachments || []).filter(isImageAttachment)
  if (!imageAttachments.length) return

  imagesLoading.value = true
  try {
    const grouped = {}
    for (const a of imageAttachments) {
      const code = a.nodeCode || 'other'
      if (!grouped[code]) grouped[code] = []
      grouped[code].push(a)
    }

    const order = ['reported', 'pending_dispatch', 'handling', 'handle_finish', 'pending_check', 'checking', 'closed', 'returned']
    const sortedCodes = Object.keys(grouped).sort((a, b) => {
      const ia = order.indexOf(a)
      const ib = order.indexOf(b)
      const va = ia >= 0 ? ia : order.length
      const vb = ib >= 0 ? ib : order.length
      return va - vb
    })

    const groups = []
    for (const code of sortedCodes) {
      const items = grouped[code]
        .sort((a, b) => parseUploadTime(a.createTime) - parseUploadTime(b.createTime))
      const resolved = []
      for (const a of items) {
        const url = await loadImagePreviewUrl(a)
        if (url) {
          resolved.push({ url, time: formatUploadTime(a.createTime) })
        }
      }
      if (resolved.length) {
        groups.push({ nodeCode: code, label: getStageLabel(code), items: resolved })
      }
    }
    stagePhotoGroups.value = groups
  } finally {
    imagesLoading.value = false
  }
}

function formatFlowTime(value) {
  if (!value) return ''
  const s = String(value)
  return s.length >= 16 ? s.slice(0, 16).replace('T', ' ') : s
}

function formatFlowReceiver(item) {
  if (!item.receiverName) return ''
  const parts = [item.receiverName]
  if (item.receiverDeptName) parts.push(`（${item.receiverDeptName}）`)
  return parts.join('')
}

async function loadFlowRecords(id) {
  try {
    const res = await getCaseFlowRecords(id)
    flowRecords.value = res.data || []
  } catch {
    flowRecords.value = []
  }
}

async function loadDetail() {
  const id = route.params.id
  try {
    const [detailRes, attRes] = await Promise.all([
      getCaseDetail(id),
      getCaseAttachments(id).catch(() => ({ data: [] }))
    ])
    caseInfo.value = detailRes.data || {}
    locationMapKey.value += 1
    const attList = attRes.data || []
    await loadStagePhotos(attList)
    await loadFlowRecords(id)
  } catch {
    showAppFailToast('获取案件详情失败')
  }
}

function previewSingleImage(url) {
  if (!url) return
  showImagePreview({ images: [url], startPosition: 0 })
}

async function afterRead(file) {
  showLoadingToast({ message: '上传中...', forbidClick: true })
  try {
    const files = Array.isArray(file) ? file : [file]
    for (const f of files) {
      const res = await uploadFile(f.file)
      const url = typeof res.data === 'string' ? res.data : res.data?.url || ''
      if (url) {
        attachmentUrls.value.push(url)
        f.url = url
      }
    }
    closeToast()
  } catch {
    closeToast()
    showAppFailToast('上传失败')
  }
}

async function submit() {
  if (!remark.value?.trim()) {
    showAppToast('请填写处置说明')
    return
  }
  if (attachmentUrls.value.length === 0) {
    showAppToast('请至少上传一张处置照片')
    return
  }

  submitting.value = true
  try {
    await handleCase({
      caseId: Number(route.params.id),
      remark: remark.value.trim(),
      attachments: attachmentUrls.value
    })
    showAppSuccessToast('已提交，等待处置部门确认')
    router.replace('/handle')
  } catch {
    showAppFailToast('提交失败')
  } finally {
    submitting.value = false
  }
}

function formatPickerDate(selectedValues) {
  const [y, m, d] = selectedValues
  return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
}

function onSuspendDateConfirm({ selectedValues }) {
  extensionSuspendUntil.value = formatPickerDate(selectedValues)
  showSuspendDatePicker.value = false
}

function openExtensionDialog(type) {
  if (type === 'extension' && caseInfo.value.hasPendingExtension) {
    showAppToast('已有延期申请在审批中，请等待处理')
    return
  }
  if (type === 'suspend' && caseInfo.value.hasPendingSuspend) {
    showAppToast('已有挂账申请在审批中，请等待处理')
    return
  }
  extensionApplyType.value = type
  extensionReason.value = ''
  extensionSuspendUntil.value = ''
  extensionDialogVisible.value = true
}

async function onExtensionDialogBeforeClose(action) {
  if (action !== 'confirm') {
    return true
  }
  const text = extensionReason.value?.trim()
  if (!text) {
    showAppToast('请填写申请原因')
    return false
  }
  if (extensionApplyType.value === 'suspend' && !extensionSuspendUntil.value) {
    showAppToast('请选择挂账截止日期')
    return false
  }
  extensionSubmitting.value = true
  try {
    await applyCaseAdjustment({
      caseId: Number(route.params.id),
      applyType: extensionApplyType.value,
      reason: text,
      suspendUntil: extensionApplyType.value === 'suspend' ? extensionSuspendUntil.value : undefined
    })
    showAppSuccessToast('已提交至处置部门审核')
    extensionDialogVisible.value = false
    await loadDetail()
    return true
  } catch (e) {
    showAppFailToast(extractRequestErrorMessage(e, '申请失败'))
    return false
  } finally {
    extensionSubmitting.value = false
  }
}

function openReturnDialog() {
  returnRemark.value = ''
  returnDialogVisible.value = true
}

async function onReturnDialogBeforeClose(action) {
  if (action !== 'confirm') {
    return true
  }
  const text = returnRemark.value?.trim()
  if (!text) {
    showAppToast('请填写回退原因')
    return false
  }
  returning.value = true
  try {
    await handlerReturnDept({
      caseId: Number(route.params.id),
      remark: text,
      clientUpdateTime: caseInfo.value.updateTime ?? null
    })
    showAppSuccessToast('已回退至处置部门')
    returnDialogVisible.value = false
    router.replace('/handle')
    return true
  } catch {
    showAppFailToast('回退失败')
    return false
  } finally {
    returning.value = false
  }
}

function goBack() {
  router.back()
}

onMounted(loadDetail)

onBeforeUnmount(() => {
  revokeStagePhotos()
})
</script>

<style scoped>
.handle-detail-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 100px;
}

.img-loading {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}

.submit-btn {
  padding: 16px;
}

.return-btn {
  margin-top: 12px;
}

.extension-tip {
  margin: 0 16px 12px;
  font-size: 12px;
  color: #969799;
  line-height: 1.5;
}

.text-overdue {
  color: #ee0a24;
  font-weight: 600;
}

.handle-photo-scroll {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding: 0 16px 12px;
  -webkit-overflow-scrolling: touch;
}

.handle-photo-batch {
  flex: 0 0 auto;
  width: 112px;
}

.handle-photo-batch-time {
  margin: 0 0 8px;
  font-size: 11px;
  line-height: 1.4;
  color: #969799;
  white-space: nowrap;
}

.handle-photo-batch-images {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.handle-photo-thumb {
  display: block;
  border-radius: 4px;
  overflow: hidden;
}

.flow-timeline {
  padding: 12px 16px;
}

.flow-item {
  display: flex;
  gap: 12px;
  padding-bottom: 16px;
  position: relative;
}

.flow-item:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 5px;
  top: 14px;
  bottom: 0;
  width: 1px;
  background: #ebedf0;
}

.flow-dot {
  flex-shrink: 0;
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: #dcdee0;
  margin-top: 3px;
}

.flow-dot--latest {
  background: #1989fa;
}

.flow-content {
  flex: 1;
  min-width: 0;
}

.flow-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.flow-node {
  font-size: 14px;
  font-weight: 600;
  color: #323233;
}

.flow-time {
  font-size: 12px;
  color: #969799;
  flex-shrink: 0;
}

.flow-meta {
  font-size: 12px;
  color: #646566;
  margin-bottom: 2px;
}

.flow-opinion {
  font-size: 12px;
  color: #969799;
  margin-top: 4px;
  line-height: 1.5;
}
</style>
