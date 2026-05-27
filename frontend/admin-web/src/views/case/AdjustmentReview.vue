<template>
  <div class="adjustment-review">
    <el-card>
      <template #header>
        <span>延期 / 挂账审批</span>
      </template>
      <el-table v-loading="loading" :data="list" style="width: 100%">
        <el-table-column prop="caseCode" label="案件编号" width="160" />
        <el-table-column prop="applyTypeLabel" label="类型" width="80" />
        <el-table-column prop="handleDeptName" label="处置部门" width="120" />
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="reason" label="申请原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="挂账截止" width="120">
          <template #default="{ row }">
            {{ row.suspendUntil ? formatDate(row.suspendUntil) : '--' }}
          </template>
        </el-table-column>
        <el-table-column label="原截止时间" width="170">
          <template #default="{ row }">
            {{ row.oldDeadlineTime || '--' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="goDetail(row.caseId)">详情</el-button>
            <el-button type="success" size="small" @click="openReview(row, true)">批准</el-button>
            <el-button type="danger" size="small" @click="openReview(row, false)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="reviewVisible" :title="reviewApproved ? '批准申请' : '驳回申请'" width="480px">
      <p class="review-summary">
        案件 {{ currentRow?.caseCode }} · {{ currentRow?.applyTypeLabel }}
      </p>
      <el-form label-width="80px">
        <el-form-item :label="reviewApproved ? '意见' : '驳回原因'" :required="!reviewApproved">
          <el-input v-model="reviewRemark" type="textarea" :rows="3" placeholder="驳回时必填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReview">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCaseAdjustmentPending, reviewCaseAdjustment } from '@/api/case'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const reviewVisible = ref(false)
const reviewApproved = ref(true)
const reviewRemark = ref('')
const currentRow = ref(null)

function formatDate(val) {
  if (!val) return '--'
  return String(val).substring(0, 10)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getCaseAdjustmentPending({ pageNum: pageNum.value, pageSize: pageSize.value })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function goDetail(caseId) {
  router.push(`/case/detail/${caseId}`)
}

function openReview(row, approved) {
  currentRow.value = row
  reviewApproved.value = approved
  reviewRemark.value = ''
  reviewVisible.value = true
}

async function submitReview() {
  if (!reviewApproved.value && !reviewRemark.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  submitting.value = true
  try {
    await reviewCaseAdjustment({
      applyId: currentRow.value.id,
      approved: reviewApproved.value,
      reviewRemark: reviewRemark.value
    })
    ElMessage.success(reviewApproved.value ? '已批准' : '已驳回')
    reviewVisible.value = false
    loadData()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.review-summary {
  margin: 0 0 12px;
  color: #606266;
}
</style>
