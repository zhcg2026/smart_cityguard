<template>
  <div class="page-container appeal-detail" v-loading="loading">
    <el-page-header @back="goBack" content="申诉详情" class="page-header" />

    <el-card v-if="detail" class="section-card">
      <template #header>申诉信息</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="申诉编号">{{ detail.appeal?.appealCode }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getAppealStatusTagType(detail.appeal?.appealStatus)" size="small">
            {{ formatAppealStatusLabel(detail.appeal?.appealStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="案件编号">
          <el-link type="primary" @click="goCase(detail.caseId)">{{ detail.caseCode }}</el-link>
        </el-descriptions-item>
        <el-descriptions-item label="申请部门">{{ detail.appeal?.applyDeptName }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detail.appeal?.applyUserName }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ detail.appeal?.applyTime }}</el-descriptions-item>
        <el-descriptions-item label="申诉说明" :span="2">{{ detail.appeal?.appealDesc }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.appeal?.finalOpinion" label="终审意见" :span="2">
          {{ detail.appeal.finalOpinion }}
        </el-descriptions-item>
      </el-descriptions>
      <div v-if="attachmentUrls.length" class="attachments">
        <p class="sub-title">附件</p>
        <el-image
          v-for="(url, idx) in attachmentUrls"
          :key="idx"
          :src="url"
          :preview-src-list="attachmentUrls"
          fit="cover"
          class="att-img"
        />
      </div>
    </el-card>

    <el-card v-if="detail" class="section-card">
      <template #header>关联案件</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="地址" :span="2">{{ detail.address || '--' }}</el-descriptions-item>
        <el-descriptions-item label="处置部门">{{ detail.handleDeptName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="结案时间">{{ detail.closeTime || '--' }}</el-descriptions-item>
        <el-descriptions-item label="处置截止">{{ detail.handleDeadlineTime || '--' }}</el-descriptions-item>
        <el-descriptions-item label="批转完成">{{ detail.handleFinishTime || '--' }}</el-descriptions-item>
        <el-descriptions-item label="处置超时">
          <span v-if="detail.handleStageTimedOut">曾超时</span>
          <span v-else>否</span>
          <el-tag v-if="detail.handleTimeoutExempt" type="success" size="small" class="exempt-tag">
            不计超时（申诉通过）
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.handleTimeoutSeconds" label="超时时长">
          {{ formatTimeoutSeconds(detail.handleTimeoutSeconds) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="detail?.reviews?.length" class="section-card">
      <template #header>审核记录</template>
      <el-timeline>
        <el-timeline-item
          v-for="r in detail.reviews"
          :key="r.id"
          :timestamp="r.reviewTime"
          placement="top"
        >
          <p><strong>{{ r.reviewNodeName }}</strong> — {{ r.reviewerName }}</p>
          <p>
            结果：
            <el-tag :type="r.reviewResult === 'approved' ? 'success' : 'danger'" size="small">
              {{ r.reviewResult === 'approved' ? '通过' : '打回' }}
            </el-tag>
          </p>
          <p v-if="r.reviewOpinion">意见：{{ r.reviewOpinion }}</p>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <el-card v-if="canReview" class="section-card">
      <template #header>{{ reviewTitle }}</template>
      <el-form label-width="88px">
        <el-form-item label="审核结果" required>
          <el-radio-group v-model="reviewForm.approved">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">打回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="reviewForm.approved ? '审核意见' : '打回原因'" :required="!reviewForm.approved">
          <el-input v-model="reviewForm.opinion" type="textarea" :rows="3" placeholder="打回时必填" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">提交审核</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getTimeoutAppealDetail,
  deptReviewTimeoutAppeal,
  dispatcherReviewTimeoutAppeal,
  acceptorReviewTimeoutAppeal
} from '@/api/appeal'
import { fetchFilePreviewBlobUrl, revokeBlobUrls } from '@/utils/fileUrl'
import { formatAppealStatusLabel, getAppealStatusTagType } from '@/utils/appealStatus'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref(null)
const attachmentUrls = ref([])
const reviewSubmitting = ref(false)
const reviewForm = reactive({
  approved: true,
  opinion: ''
})

const canReview = computed(
  () => detail.value?.canDeptReview || detail.value?.canDispatcherReview || detail.value?.canAcceptorReview
)

const reviewTitle = computed(() => {
  if (detail.value?.canDeptReview) return '部门审核'
  if (detail.value?.canDispatcherReview) return '派遣员初审'
  if (detail.value?.canAcceptorReview) return '受理员二审'
  return '审核'
})

function formatTimeoutSeconds(sec) {
  if (!sec || sec <= 0) return '--'
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  if (h > 0) return `${h}小时${m}分`
  return `${m}分钟`
}

async function loadDetail() {
  const id = route.params.id
  if (!id) return
  loading.value = true
  revokeBlobUrls(attachmentUrls.value)
  attachmentUrls.value = []
  try {
    const res = await getTimeoutAppealDetail(id)
    detail.value = res.data
    const paths = (res.data?.attachments || []).map((a) => a.filePath).filter(Boolean)
    attachmentUrls.value = await Promise.all(paths.map((p) => fetchFilePreviewBlobUrl(p)))
  } finally {
    loading.value = false
  }
}

async function submitReview() {
  if (!reviewForm.approved && !reviewForm.opinion.trim()) {
    ElMessage.warning('打回时请填写意见')
    return
  }
  reviewSubmitting.value = true
  try {
    const payload = {
      appealId: detail.value.appeal.id,
      approved: reviewForm.approved,
      opinion: reviewForm.opinion.trim() || undefined
    }
    if (detail.value.canDeptReview) {
      await deptReviewTimeoutAppeal(payload)
    } else if (detail.value.canDispatcherReview) {
      await dispatcherReviewTimeoutAppeal(payload)
    } else {
      await acceptorReviewTimeoutAppeal(payload)
    }
    ElMessage.success('审核已提交')
    reviewForm.opinion = ''
    await loadDetail()
  } finally {
    reviewSubmitting.value = false
  }
}

function goBack() {
  router.push({ name: 'AppealList' })
}

function goCase(caseId) {
  router.push({ path: `/case/detail/${caseId}` })
}

onMounted(loadDetail)
</script>

<style scoped>
.page-header {
  margin-bottom: 16px;
}
.section-card {
  margin-bottom: 16px;
}
.attachments {
  margin-top: 12px;
}
.sub-title {
  margin: 0 0 8px;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}
.att-img {
  width: 96px;
  height: 96px;
  margin-right: 8px;
  border-radius: 4px;
}
.exempt-tag {
  margin-left: 8px;
}
</style>
