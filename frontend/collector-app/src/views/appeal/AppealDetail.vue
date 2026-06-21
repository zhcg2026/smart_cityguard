<template>
  <div class="appeal-detail-page">
    <van-nav-bar title="申诉详情" left-arrow @click-left="goBack" />

    <van-cell-group title="案件信息" inset>
      <van-cell title="案件编号" :value="detail.caseCode" />
      <van-cell title="案件小类" :value="detail.case?.smallName || detail.smallName" />
      <van-cell title="发生地址" :value="detail.case?.address || detail.address" />
    </van-cell-group>

    <van-cell-group title="申诉信息" inset>
      <van-cell title="申诉状态" :value="statusLabel(detail.appeal?.appealStatus)" />
      <van-cell title="申诉描述" :value="detail.appeal?.appealDesc || '—'" />
      <van-cell title="申诉人" :value="detail.appeal?.applyUserName || '—'" />
      <van-cell v-if="detail.appeal?.applyTime" title="提交时间" :value="formatTime(detail.appeal.applyTime)" />
    </van-cell-group>

    <van-cell-group v-if="detail.attachments?.length" title="申诉附件" inset>
      <div class="attachment-list">
        <van-image
          v-for="(att, idx) in detail.attachments"
          :key="idx"
          width="80"
          height="80"
          fit="cover"
          :src="att.filePath"
          radius="4"
          @click="previewImage(att.filePath)"
        />
      </div>
    </van-cell-group>

    <van-cell-group v-if="detail.reviews?.length" title="审核记录" inset>
      <van-cell
        v-for="review in detail.reviews"
        :key="review.id"
        :title="review.reviewNodeName"
        :label="review.reviewOpinion || '—'"
        :value="review.reviewResult === 'approved' ? '通过' : '驳回'"
      />
    </van-cell-group>

    <div v-if="detail.canDeptReview" class="review-btns">
      <van-button round block type="primary" @click="onReview(true)">审核通过</van-button>
      <van-button round block type="danger" plain class="mt-12" @click="onReview(false)">驳回</van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showDialog, showImagePreview } from 'vant'
import { getTimeoutAppealDetail, deptReviewTimeoutAppeal } from '@/api/appeal'

const router = useRouter()
const route = useRoute()
const detail = ref({})

function goBack() {
  router.back()
}

function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}

function statusLabel(s) {
  return {
    pending_dept: '待部门审核',
    pending_dispatcher: '待派遣员审核',
    pending_acceptor: '待受理员审核',
    approved: '已通过',
    rejected: '已驳回'
  }[s] || s || '—'
}

function previewImage(url) {
  showImagePreview([url])
}

async function onReview(approved) {
  if (!approved) {
    try {
      const { value } = await showDialog({
        title: '驳回意见',
        showCancelButton: true,
        input: true,
        inputPlaceholder: '请填写驳回原因'
      })
      if (!value?.trim()) {
        showToast('请填写驳回原因')
        return
      }
      await doReview(approved, value.trim())
    } catch {
      // user cancelled
    }
  } else {
    await doReview(approved, '')
  }
}

async function doReview(approved, opinion) {
  try {
    await deptReviewTimeoutAppeal({
      appealId: detail.value.appeal?.id,
      approved,
      opinion
    })
    showToast(approved ? '审核已通过' : '已驳回')
    loadDetail()
  } catch {
    // error handled by interceptor
  }
}

async function loadDetail() {
  const id = route.params.id
  if (!id) return
  try {
    const res = await getTimeoutAppealDetail(id)
    detail.value = res.data || {}
  } catch {
    // ignore
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.appeal-detail-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.attachment-list {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
}

.review-btns {
  padding: 20px 16px;
}

.mt-12 {
  margin-top: 12px;
}
</style>
