<template>
  <div class="appeal-submit-page">
    <van-nav-bar title="超时申诉" left-arrow @click-left="goBack" />

    <van-cell-group v-if="caseInfo.id" title="案件信息" inset>
      <van-cell title="案件编号" :value="caseInfo.caseCode" />
      <van-cell title="案件小类" :value="caseInfo.smallName" />
      <van-cell title="发生地址" :value="caseInfo.address" />
    </van-cell-group>

    <van-cell-group title="申诉描述" inset>
      <van-field
        v-model="appealDesc"
        rows="4"
        autosize
        type="textarea"
        placeholder="请描述超时原因（必填）"
      />
    </van-cell-group>

    <van-cell-group title="申诉附件" inset>
      <van-uploader
        v-model="fileList"
        multiple
        :max-count="3"
        :after-read="afterRead"
        @delete="onDelete"
      />
    </van-cell-group>

    <div class="submit-btn">
      <van-button round block type="primary" :loading="submitting" @click="handleSubmit">
        提交申诉
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { getCaseDetail } from '@/api/case'
import { submitTimeoutAppeal } from '@/api/appeal'
import { uploadFile } from '@/api/file'

const router = useRouter()
const route = useRoute()
const caseInfo = ref({})
const appealDesc = ref('')
const fileList = ref([])
const attachments = ref([])
const submitting = ref(false)

function goBack() {
  router.back()
}

function syncAttachments() {
  attachments.value = fileList.value
    .map(f => f.url)
    .filter(url => typeof url === 'string' && url.trim() && !url.startsWith('blob:') && !url.startsWith('data:'))
}

async function afterRead(file) {
  const files = Array.isArray(file) ? file : [file]
  showLoadingToast({ message: '上传中...', forbidClick: true, duration: 0 })
  try {
    for (const f of files) {
      const res = await uploadFile(f.file)
      const url = typeof res.data === 'string' ? res.data : res.data?.url
      if (!url) throw new Error('上传失败')
      f.url = url
    }
    syncAttachments()
  } catch {
    for (const f of files) {
      const idx = fileList.value.indexOf(f)
      if (idx >= 0) fileList.value.splice(idx, 1)
    }
    syncAttachments()
    showToast('附件上传失败')
  } finally {
    closeToast()
  }
}

function onDelete() {
  syncAttachments()
}

async function handleSubmit() {
  if (!appealDesc.value.trim()) {
    showToast('请填写申诉描述')
    return
  }
  submitting.value = true
  try {
    syncAttachments()
    await submitTimeoutAppeal({
      caseId: Number(route.params.caseId),
      appealDesc: appealDesc.value.trim(),
      attachmentPaths: attachments.value
    })
    showToast('申诉已提交')
    router.back()
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const caseId = route.params.caseId
  if (!caseId) return
  try {
    const res = await getCaseDetail(caseId)
    caseInfo.value = res.data || {}
  } catch {
    // ignore
  }
})
</script>

<style scoped>
.appeal-submit-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.submit-btn {
  padding: 20px 16px;
}
</style>
