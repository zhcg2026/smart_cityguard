<template>
  <div class="check-task-page">
    <van-nav-bar title="核查任务详情" left-arrow @click-left="goBack" />

    <van-notice-bar
      v-if="timerBanner.show"
      wrapable
      :scrollable="false"
      left-icon="clock-o"
      :color="timerBanner.overdue ? '#c45656' : '#1989fa'"
      :background="timerBanner.overdue ? '#fef0f0' : '#ecf5ff'"
      :text="timerBanner.text"
    />

    <van-cell-group title="案件信息" inset>
      <van-cell title="案件编号" :value="taskInfo.caseNo || taskInfo.caseCode" />
      <van-cell title="案件小类" :value="taskInfo.categorySmallName || taskInfo.smallName" />
      <van-cell title="发生地址" :value="taskInfo.address" />
      <van-cell title="问题描述" :value="taskInfo.description" />
    </van-cell-group>

    <van-cell-group v-if="taskInfo.assignRemark" title="核查要求" inset>
      <van-cell>
        <template #title>
          <div class="assign-remark">{{ taskInfo.assignRemark }}</div>
          <div v-if="taskInfo.assignerName" class="assign-meta">下发人：{{ taskInfo.assignerName }}</div>
        </template>
      </van-cell>
    </van-cell-group>

    <van-cell-group title="核查结果" inset>
      <van-radio-group v-model="result">
        <van-cell-group inset>
          <van-cell title="问题确认存在" clickable @click="result = 'confirmed'">
            <template #right-icon>
              <van-radio name="confirmed" />
            </template>
          </van-cell>
          <van-cell title="未发现问题" clickable @click="result = 'not_found'">
            <template #right-icon>
              <van-radio name="not_found" />
            </template>
          </van-cell>
          <van-cell title="无法核查" clickable @click="result = 'unable'">
            <template #right-icon>
              <van-radio name="unable" />
            </template>
          </van-cell>
        </van-cell-group>
      </van-radio-group>
    </van-cell-group>

    <van-cell-group title="核查意见" inset>
      <van-field v-model="remark" rows="2" autosize type="textarea" placeholder="请填写核查意见（选填）" />
    </van-cell-group>

    <van-cell-group title="核查照片" inset>
      <van-notice-bar
        v-if="uploadError"
        wrapable
        :scrollable="false"
        color="#c45656"
        background="#fef0f0"
        left-icon="warning-o"
        :text="uploadError"
      />
      <van-uploader
        v-model="fileList"
        multiple
        :max-count="3"
        :after-read="afterRead"
        @delete="onDeletePhoto"
      />
      <van-cell v-if="attachments.length" :title="`已上传 ${attachments.length} 张`" />
    </van-cell-group>

    <div class="submit-btn">
      <van-button round block type="primary" :loading="submitting" :disabled="taskInfo.taskStatus === 'done'" @click="submit">
        提交核查结果
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { buildTaskTimerBanner } from '@/utils/taskTimer'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showLoadingToast } from 'vant'
import { getCheckTaskDetail, executeCheckTask } from '@/api/task'
import { uploadFile as uploadCaseFile } from '@/api/case'
import {
  showUploadFailure,
  showUploadSuccess,
  showMissingPhotosDialog
} from '@/utils/uploadFeedback'

const router = useRouter()
const route = useRoute()
const taskInfo = ref({})
const result = ref('')
const remark = ref('')
const fileList = ref([])
const attachments = ref([])
const uploadError = ref('')
const submitting = ref(false)

const timerBanner = computed(() => buildTaskTimerBanner(taskInfo.value))

onMounted(async () => {
  try {
    const res = await getCheckTaskDetail(route.params.id)
    taskInfo.value = res.data || {}
  } catch {
    showToast('获取详情失败')
  }
})

function resolveUploadUrl(res) {
  return typeof res?.data === 'string' ? res.data : (res?.data?.url || '')
}

function syncAttachmentsFromFileList() {
  const urls = fileList.value
    .map((f) => f.url)
    .filter(
      (url) =>
        typeof url === 'string' &&
        url.trim() &&
        !url.startsWith('blob:') &&
        !url.startsWith('data:')
    )
  attachments.value = [...new Set(urls)]
}

function hasPendingLocalPhotos() {
  return fileList.value.some(
    (f) => f.url && (String(f.url).startsWith('blob:') || String(f.url).startsWith('data:'))
  )
}

function onDeletePhoto() {
  syncAttachmentsFromFileList()
  if (attachments.value.length === 0) {
    uploadError.value = ''
  }
}

function removeFailedFiles(files) {
  for (const f of files) {
    const idx = fileList.value.indexOf(f)
    if (idx >= 0) {
      fileList.value.splice(idx, 1)
    }
  }
}

async function afterRead(file) {
  uploadError.value = ''
  showLoadingToast({ message: '上传中...', forbidClick: true, duration: 0 })
  const files = Array.isArray(file) ? file : [file]
  try {
    for (const f of files) {
      const res = await uploadCaseFile(f.file)
      const url = resolveUploadUrl(res)
      if (!url) {
        throw new Error('服务器未返回文件地址')
      }
      f.url = url
    }
    syncAttachmentsFromFileList()
    showUploadSuccess()
  } catch (error) {
    removeFailedFiles(files)
    syncAttachmentsFromFileList()
    const { text } = await showUploadFailure(error)
    uploadError.value = text.replace(/\n+/g, ' ')
  }
}

async function submit() {
  if (!result.value) {
    showToast('请选择核查结果')
    return
  }
  syncAttachmentsFromFileList()
  const urls = attachments.value.filter((u) => typeof u === 'string' && u.trim())
  if (result.value === 'confirmed') {
    if (hasPendingLocalPhotos() || urls.length === 0) {
      await showMissingPhotosDialog('核查')
      return
    }
  }
  submitting.value = true
  try {
    await executeCheckTask({
      taskId: Number(route.params.id),
      result: result.value,
      remark: remark.value,
      attachments: urls
    })
    showToast('核查完成')
    router.push('/task')
  } catch {
    showToast('提交失败')
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.back()
}
</script>

<style scoped>
.check-task-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 80px;
}
.submit-btn {
  padding: 16px;
}
.assign-remark {
  white-space: pre-wrap;
  line-height: 1.5;
  color: #323233;
}
.assign-meta {
  margin-top: 6px;
  font-size: 12px;
  color: #969799;
}
</style>
