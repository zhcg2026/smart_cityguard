<template>
  <div class="case-detail">
    <!-- 基本信息 -->
    <el-card class="info-card">
      <template #header>
        <div class="header-with-action">
          <span>案件基本信息</span>
          <div class="actions">
            <el-button v-if="canRegister" type="primary" @click="handleRegister">立案</el-button>
            <el-button v-if="canDispatch" type="success" @click="handleDispatch">派遣</el-button>
            <el-button v-if="canHandle" type="warning" @click="handleHandle">处置</el-button>
            <el-button v-if="canVerify" type="info" @click="handleVerify">核查</el-button>
            <el-button v-if="canCheck" type="primary" @click="handleCheck">核实</el-button>
            <el-button v-if="canClose" type="success" @click="handleClose">结案</el-button>
            <el-button v-if="canReject" type="danger" @click="handleReject">不受理</el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="案件编号">{{ caseInfo.caseNo }}</el-descriptions-item>
        <el-descriptions-item label="案件状态">
          <el-tag :type="getStatusType(caseInfo.status)">
            {{ getStatusLabel(caseInfo.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="案件来源">{{ caseInfo.sourceName }}</el-descriptions-item>
        <el-descriptions-item label="案件大类">{{ caseInfo.categoryBigName }}</el-descriptions-item>
        <el-descriptions-item label="案件小类">{{ caseInfo.categorySmallName }}</el-descriptions-item>
        <el-descriptions-item label="立案条件">{{ caseInfo.conditionName }}</el-descriptions-item>
        <el-descriptions-item label="发生地址" :span="2">{{ caseInfo.address }}</el-descriptions-item>
        <el-descriptions-item label="坐标">{{ caseInfo.lngLat }}</el-descriptions-item>
        <el-descriptions-item label="上报时间">{{ caseInfo.reportTime }}</el-descriptions-item>
        <el-descriptions-item label="上报人">{{ caseInfo.collectorName }}</el-descriptions-item>
        <el-descriptions-item label="所属网格">{{ caseInfo.gridName }}</el-descriptions-item>
        <el-descriptions-item label="问题描述" :span="3">{{ caseInfo.description }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 地图定位 -->
    <el-card class="map-card">
      <template #header>
        <span>地图定位</span>
      </template>
      <div id="map-container" class="map-container"></div>
    </el-card>

    <!-- 附件信息 -->
    <el-card class="attachment-card">
      <template #header>
        <span>现场照片/视频</span>
      </template>
      <el-row :gutter="20">
        <el-col v-for="item in attachments" :key="item.id" :span="6">
          <el-image
            v-if="item.type === 'image'"
            :src="item.url"
            :preview-src-list="imageList"
            fit="cover"
            class="attachment-image"
          />
          <video v-else :src="item.url" class="attachment-video" controls />
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
            <h4>{{ item.operateName }}</h4>
            <p>操作人：{{ item.operatorName }}</p>
            <p v-if="item.remark">备注：{{ item.remark }}</p>
            <el-row v-if="item.attachments?.length" :gutter="10">
              <el-col v-for="att in item.attachments" :key="att.id" :span="4">
                <el-image :src="att.url" fit="cover" class="flow-image" />
              </el-col>
            </el-row>
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
      <el-form ref="formRef" :model="processForm" :rules="processRules" label-width="100px">
        <el-form-item v-if="showDeptSelect" label="派遣部门" prop="departmentId">
          <el-tree-select
            v-model="processForm.departmentId"
            :data="deptTree"
            :props="{ label: 'name', value: 'id' }"
            placeholder="请选择派遣部门"
            check-strictly
          />
        </el-form-item>
        <el-form-item v-if="showResultSelect" label="核查结果" prop="verifyResult">
          <el-radio-group v-model="processForm.verifyResult">
            <el-radio label="confirmed">确认存在</el-radio>
            <el-radio label="not_found">未发现问题</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="showCheckResult" label="核实结果" prop="checkResult">
          <el-radio-group v-model="processForm.checkResult">
            <el-radio label="passed">处置到位</el-radio>
            <el-radio label="not_passed">处置不到位</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="processForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
        <el-form-item label="附件" prop="attachments">
          <el-upload
            v-model:file-list="processForm.attachments"
            action="/api/file/upload"
            list-type="picture-card"
            :limit="5"
            accept="image/*,video/*"
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCaseDetail, getCaseFlowRecords, getCaseAttachments, registerCase, dispatchCase, handleCase, verifyCase, checkCase, closeCase, rejectCase } from '@/api/case'

const route = useRoute()

const caseInfo = ref({})
const attachments = ref([])
const flowRecords = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const currentAction = ref('')
const deptTree = ref([])

const formRef = ref()
const processForm = reactive({
  departmentId: '',
  verifyResult: '',
  checkResult: '',
  remark: '',
  attachments: []
})

const processRules = {
  departmentId: [{ required: true, message: '请选择派遣部门', trigger: 'change' }],
  verifyResult: [{ required: true, message: '请选择核查结果', trigger: 'change' }],
  checkResult: [{ required: true, message: '请选择核实结果', trigger: 'change' }]
}

const imageList = computed(() => attachments.value.filter(a => a.type === 'image').map(a => a.url))

// 操作权限判断
const canRegister = computed(() => caseInfo.value.status === 'PENDING_REGISTER')
const canDispatch = computed(() => caseInfo.value.status === 'PENDING_DISPATCH')
const canHandle = computed(() => caseInfo.value.status === 'PENDING_HANDLE')
const canVerify = computed(() => caseInfo.value.status === 'PENDING_VERIFY')
const canCheck = computed(() => caseInfo.value.status === 'PENDING_CHECK')
const canClose = computed(() => caseInfo.value.status === 'PENDING_CLOSE')
const canReject = computed(() => ['PENDING_VERIFY', 'PENDING_REGISTER'].includes(caseInfo.value.status))

// 表单显示控制
const showDeptSelect = computed(() => currentAction.value === 'dispatch')
const showResultSelect = computed(() => currentAction.value === 'verify')
const showCheckResult = computed(() => currentAction.value === 'check')

onMounted(async () => {
  const caseId = route.params.id
  await loadCaseDetail(caseId)
  await loadCaseAttachments(caseId)
  await loadCaseFlowRecords(caseId)
})

async function loadCaseDetail(id) {
  loading.value = true
  try {
    const res = await getCaseDetail(id)
    caseInfo.value = res.data || {}
  } catch (error) {
    console.error('获取案件详情失败:', error)
  } finally {
    loading.value = false
  }
}

async function loadCaseAttachments(id) {
  try {
    const res = await getCaseAttachments(id)
    attachments.value = res.data || []
  } catch (error) {
    console.error('获取附件失败:', error)
  }
}

async function loadCaseFlowRecords(id) {
  try {
    const res = await getCaseFlowRecords(id)
    flowRecords.value = res.data || []
  } catch (error) {
    console.error('获取流程记录失败:', error)
  }
}

function handleRegister() {
  currentAction.value = 'register'
  dialogTitle.value = '立案'
  dialogVisible.value = true
}

function handleDispatch() {
  currentAction.value = 'dispatch'
  dialogTitle.value = '派遣'
  dialogVisible.value = true
}

function handleHandle() {
  currentAction.value = 'handle'
  dialogTitle.value = '处置'
  dialogVisible.value = true
}

function handleVerify() {
  currentAction.value = 'verify'
  dialogTitle.value = '核查'
  dialogVisible.value = true
}

function handleCheck() {
  currentAction.value = 'check'
  dialogTitle.value = '核实'
  dialogVisible.value = true
}

function handleClose() {
  currentAction.value = 'close'
  dialogTitle.value = '结案'
  dialogVisible.value = true
}

function handleReject() {
  currentAction.value = 'reject'
  dialogTitle.value = '不受理'
  dialogVisible.value = true
}

async function submitProcess() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    const data = {
      caseId: caseInfo.value.id,
      ...processForm
    }

    switch (currentAction.value) {
      case 'register':
        await registerCase(data)
        break
      case 'dispatch':
        await dispatchCase(data)
        break
      case 'handle':
        await handleCase(data)
        break
      case 'verify':
        await verifyCase(data)
        break
      case 'check':
        await checkCase(data)
        break
      case 'close':
        await closeCase(data)
        break
      case 'reject':
        await rejectCase(data)
        break
    }

    ElMessage.success('操作成功')
    dialogVisible.value = false
    await loadCaseDetail(caseInfo.value.id)
    await loadCaseFlowRecords(caseInfo.value.id)
  } catch (error) {
    console.error('操作失败:', error)
  }
}

function getStatusType(status) {
  const statusTypeMap = {
    'PENDING_VERIFY': 'warning',
    'PENDING_REGISTER': 'warning',
    'PENDING_DISPATCH': 'info',
    'PENDING_HANDLE': 'primary',
    'PENDING_CHECK': 'primary',
    'CLOSED': 'success',
    'REJECTED': 'danger'
  }
  return statusTypeMap[status] || 'info'
}

function getStatusLabel(status) {
  const statusLabelMap = {
    'PENDING_VERIFY': '待核查',
    'PENDING_REGISTER': '待立案',
    'PENDING_DISPATCH': '待派遣',
    'PENDING_HANDLE': '待处置',
    'PENDING_CHECK': '待核实',
    'CLOSED': '已结案',
    'REJECTED': '不受理'
  }
  return statusLabelMap[status] || status
}
</script>

<style lang="scss" scoped>
.case-detail {
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
    }
  }

  .attachment-card {
    margin-bottom: 20px;

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
}
</style>