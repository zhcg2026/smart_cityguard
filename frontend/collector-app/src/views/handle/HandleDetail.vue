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

    <van-cell-group title="上报照片" inset>
      <van-grid v-if="reportImages.length" :column-num="3" :border="false">
        <van-grid-item v-for="(url, idx) in reportImages" :key="idx">
          <van-image
            :src="url"
            fit="cover"
            width="100%"
            height="80"
            @click="previewSingleImage(url)"
          />
        </van-grid-item>
      </van-grid>
      <van-loading v-else-if="imagesLoading" class="img-loading" size="24px">加载照片中...</van-loading>
      <van-empty v-else description="暂无上报照片" image-size="60" />
    </van-cell-group>

    <van-cell-group v-if="handlePhotoBatches.length" title="处置照片" inset>
      <div class="handle-photo-scroll">
        <div
          v-for="batch in handlePhotoBatches"
          :key="batch.timeKey"
          class="handle-photo-batch"
        >
          <p class="handle-photo-batch-time">{{ batch.timeLabel }}</p>
          <div class="handle-photo-batch-images">
            <van-image
              v-for="(url, idx) in batch.images"
              :key="batch.timeKey + '-' + idx"
              :src="url"
              fit="cover"
              width="112"
              height="84"
              class="handle-photo-thumb"
              @click="previewSingleImage(url)"
            />
          </div>
        </div>
      </div>
    </van-cell-group>

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

    <div class="submit-btn" v-if="canSubmit">
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
    <van-notice-bar v-else color="#ed6a0c" background="#fffbe8" text="该案件未指派给您，无法提交处置结果" />

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
import { showToast, showImagePreview, showLoadingToast, closeToast, showSuccessToast } from 'vant'
import {
  getCaseDetail,
  getCaseAttachments,
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
const reportImages = ref([])
const handlePhotoBatches = ref([])
const imagesLoading = ref(false)
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
    handle_finish: '处置人员已处置',
    pending_check: '待核实',
    closed: '已结案'
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

/** 上报阶段附件（排除处置完成上传的） */
function isReportAttachment(a) {
  const code = a.nodeCode
  if (!code) return true
  if (code === 'reported') return true
  if (code === 'handle_finish') return false
  return code !== 'handle_finish'
}

function isPreviousHandleAttachment(a) {
  return a.nodeCode === 'handle_finish'
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

/** 同一批上传（间隔 1 分钟内）归为一组 */
function groupHandleAttachments(attachments) {
  const list = (attachments || [])
    .filter(isPreviousHandleAttachment)
    .filter(isImageAttachment)
    .sort((a, b) => parseUploadTime(a.createTime) - parseUploadTime(b.createTime))

  const batches = []
  for (const item of list) {
    const t = parseUploadTime(item.createTime)
    let batch = batches[batches.length - 1]
    if (!batch || t - batch.endTime > 60000) {
      batch = {
        timeKey: `${t}-${item.id}`,
        timeLabel: formatUploadTime(item.createTime),
        endTime: t,
        items: []
      }
      batches.push(batch)
    } else {
      batch.endTime = t
    }
    batch.items.push(item)
  }
  return batches
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

async function loadReportImages(attachments) {
  revokeBlobUrls(reportImages.value)
  reportImages.value = []
  const list = (attachments || []).filter((a) => isReportAttachment(a) && isImageAttachment(a))
  if (!list.length) return

  imagesLoading.value = true
  try {
    const urls = []
    for (const a of list) {
      const url = await loadImagePreviewUrl(a)
      if (url) urls.push(url)
    }
    reportImages.value = urls
  } finally {
    imagesLoading.value = false
  }
}

function revokeHandlePhotoBatches() {
  for (const batch of handlePhotoBatches.value) {
    revokeBlobUrls(batch.images)
  }
}

async function loadHandlePhotoBatches(attachments) {
  revokeHandlePhotoBatches()
  handlePhotoBatches.value = []
  if (caseInfo.value.caseStatus !== 'handling') return

  const groups = groupHandleAttachments(attachments)
  const batches = []
  for (const group of groups) {
    const images = []
    for (const item of group.items) {
      const url = await loadImagePreviewUrl(item)
      if (url) images.push(url)
    }
    if (images.length) {
      batches.push({
        timeKey: group.timeKey,
        timeLabel: group.timeLabel,
        images
      })
    }
  }
  handlePhotoBatches.value = batches
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
    await Promise.all([loadReportImages(attList), loadHandlePhotoBatches(attList)])
  } catch {
    showToast('获取案件详情失败')
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
    showToast('上传失败')
  }
}

async function submit() {
  if (!remark.value?.trim()) {
    showToast('请填写处置说明')
    return
  }
  if (attachmentUrls.value.length === 0) {
    showToast('请至少上传一张处置照片')
    return
  }

  submitting.value = true
  try {
    await handleCase({
      caseId: Number(route.params.id),
      remark: remark.value.trim(),
      attachments: attachmentUrls.value
    })
    showSuccessToast('已提交，等待处置部门确认')
    router.replace('/handle')
  } catch {
    showToast('提交失败')
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
    showToast('请填写申请原因')
    return false
  }
  if (extensionApplyType.value === 'suspend' && !extensionSuspendUntil.value) {
    showToast('请选择挂账截止日期')
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
    showSuccessToast('已提交至处置部门审核')
    extensionDialogVisible.value = false
    await loadDetail()
    return true
  } catch (e) {
    showToast(e?.message || '申请失败')
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
    showToast('请填写回退原因')
    return false
  }
  returning.value = true
  try {
    await handlerReturnDept({
      caseId: Number(route.params.id),
      remark: text,
      clientUpdateTime: caseInfo.value.updateTime ?? null
    })
    showSuccessToast('已回退至处置部门')
    returnDialogVisible.value = false
    router.replace('/handle')
    return true
  } catch {
    showToast('回退失败')
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
  revokeBlobUrls(reportImages.value)
  revokeHandlePhotoBatches()
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
</style>
