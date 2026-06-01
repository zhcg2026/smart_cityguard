<template>
  <div class="handle-detail-page">
    <van-nav-bar title="案件处置" left-arrow @click-left="goBack" />

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
            @click="previewImages(reportImages, idx)"
          />
        </van-grid-item>
      </van-grid>
      <van-loading v-else-if="imagesLoading" class="img-loading" size="24px">加载照片中...</van-loading>
      <van-empty v-else description="暂无上报照片" image-size="60" />
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
import { getCaseDetail, getCaseAttachments, handleCase, handlerReturnDept, uploadFile } from '@/api/case'
import { useUserStore } from '@/stores/user'
import CaseLocationMap from '@/components/CaseLocationMap.vue'
import { fetchFilePreviewBlobUrl, revokeBlobUrls } from '@/utils/fileUrl'
import { buildHandleTimerDisplay } from '@/utils/caseTimer'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const caseInfo = ref({})
const reportImages = ref([])
const imagesLoading = ref(false)
const remark = ref('')
const fileList = ref([])
const attachmentUrls = ref([])
const submitting = ref(false)
const returning = ref(false)
const returnDialogVisible = ref(false)
const returnRemark = ref('')

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

async function loadReportImages(attachments) {
  revokeBlobUrls(reportImages.value)
  reportImages.value = []
  const list = (attachments || []).filter((a) => isReportAttachment(a) && isImageAttachment(a))
  if (!list.length) return

  imagesLoading.value = true
  const urls = []
  try {
    for (const a of list) {
      const raw = attachmentUrl(a)
      if (!raw) continue
      try {
        const displayUrl = await fetchFilePreviewBlobUrl(raw)
        if (displayUrl) {
          urls.push(displayUrl)
        } else if (raw.startsWith('http://') || raw.startsWith('https://')) {
          urls.push(raw)
        }
      } catch (e) {
        console.warn('加载附件预览失败:', raw, e)
      }
    }
    reportImages.value = urls
  } finally {
    imagesLoading.value = false
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
    await loadReportImages(attRes.data || [])
  } catch {
    showToast('获取案件详情失败')
  }
}

function previewImages(urls, startPosition) {
  showImagePreview({ images: urls, startPosition })
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

.text-overdue {
  color: #ee0a24;
  font-weight: 600;
}
</style>
