<template>
  <div class="page-container category-manage">
    <el-card class="intro-card">
      <template #header>
        <div class="card-header">
          <span>案件分类</span>
          <el-button type="primary" link @click="goStandardImport">批量导入（立结案标准）</el-button>
        </div>
      </template>
      <el-alert
        type="info"
        show-icon
        :closable="false"
        title="维护「部件/事件 → 大类 → 小类 → 立案条件」目录，供采集上报、受理登记、立案计时使用。大批量初始化请使用「立结案标准」导入 muban.xlsx。"
      />
    </el-card>

    <el-card class="toolbar-card">
      <el-radio-group v-model="categoryType" @change="onTypeChange">
        <el-radio-button :label="1">部件</el-radio-button>
        <el-radio-button :label="2">事件</el-radio-button>
      </el-radio-group>
      <el-button class="ml-2" @click="loadBigList">刷新</el-button>
    </el-card>

    <div class="panels">
      <!-- 大类 -->
      <el-card class="panel">
        <template #header>
          <div class="panel-header">
            <span>大类</span>
            <el-button v-if="isAdmin" type="primary" size="small" @click="openBigDialog()">新增</el-button>
          </div>
        </template>
        <el-table
          v-loading="bigLoading"
          :data="bigList"
          highlight-current-row
          height="420"
          @current-change="onBigSelect"
        >
          <el-table-column prop="bigCode" label="编码" width="72" />
          <el-table-column prop="bigName" label="名称" min-width="100" show-overflow-tooltip />
          <el-table-column label="状态" width="72" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="isAdmin" label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click.stop="openBigDialog(row)">编辑</el-button>
              <el-button type="danger" link size="small" @click.stop="removeBig(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 小类 -->
      <el-card class="panel">
        <template #header>
          <div class="panel-header">
            <span>小类{{ selectedBig ? `（${selectedBig.bigName}）` : '' }}</span>
            <el-button
              v-if="isAdmin"
              type="primary"
              size="small"
              :disabled="!selectedBig"
              @click="openSmallDialog()"
            >
              新增
            </el-button>
          </div>
        </template>
        <el-empty v-if="!selectedBig" description="请先选择大类" :image-size="64" />
        <el-table
          v-else
          v-loading="smallLoading"
          :data="smallList"
          highlight-current-row
          height="420"
          @current-change="onSmallSelect"
        >
          <el-table-column prop="smallCode" label="编码" width="72" />
          <el-table-column prop="smallName" label="名称" min-width="100" show-overflow-tooltip />
          <el-table-column prop="responsibilitySubject" label="责任主体" min-width="90" show-overflow-tooltip />
          <el-table-column label="状态" width="72" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="isAdmin" label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click.stop="openSmallDialog(row)">编辑</el-button>
              <el-button type="danger" link size="small" @click.stop="removeSmall(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 立案条件 -->
      <el-card class="panel panel-wide">
        <template #header>
          <div class="panel-header">
            <span>立案条件{{ selectedSmall ? `（${selectedSmall.smallName}）` : '' }}</span>
            <el-button
              v-if="isAdmin"
              type="primary"
              size="small"
              :disabled="!selectedSmall"
              @click="openStandardDialog()"
            >
              新增
            </el-button>
          </div>
        </template>
        <el-empty v-if="!selectedSmall" description="请先选择小类" :image-size="64" />
        <el-table v-else v-loading="standardLoading" :data="standardList" height="420">
          <el-table-column label="立案条件" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.conditionContent || row.conditionDesc }}
            </template>
          </el-table-column>
          <el-table-column prop="closeCondition" label="结案条件" width="100" show-overflow-tooltip />
          <el-table-column label="处置时限" width="110">
            <template #default="{ row }">
              {{ formatTimeLimit(row) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="72" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="isAdmin" label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openStandardDialog(row)">编辑</el-button>
              <el-button type="danger" link size="small" @click="removeStandard(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 大类对话框 -->
    <el-dialog v-model="bigVisible" :title="bigForm.id ? '编辑大类' : '新增大类'" width="480px" destroy-on-close>
      <el-form ref="bigFormRef" :model="bigForm" :rules="bigRules" label-width="100px">
        <el-form-item label="事部件类型">
          <el-tag>{{ categoryType === 1 ? '部件' : '事件' }}</el-tag>
        </el-form-item>
        <el-form-item label="大类编码" prop="bigCode">
          <el-input v-model="bigForm.bigCode" :disabled="!!bigForm.id" maxlength="20" placeholder="如 01" />
        </el-form-item>
        <el-form-item label="大类名称" prop="bigName">
          <el-input v-model="bigForm.bigName" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="bigForm.sortOrder" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="bigForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="bigForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bigVisible = false">取消</el-button>
        <el-button type="primary" :loading="bigSaving" @click="submitBig">保存</el-button>
      </template>
    </el-dialog>

    <!-- 小类对话框 -->
    <el-dialog v-model="smallVisible" :title="smallForm.id ? '编辑小类' : '新增小类'" width="560px" destroy-on-close>
      <el-form ref="smallFormRef" :model="smallForm" :rules="smallRules" label-width="100px">
        <el-form-item label="所属大类">
          <span>{{ selectedBig?.bigName || '—' }}</span>
        </el-form-item>
        <el-form-item label="小类编码" prop="smallCode">
          <el-input v-model="smallForm.smallCode" :disabled="!!smallForm.id" maxlength="20" />
        </el-form-item>
        <el-form-item label="小类名称" prop="smallName">
          <el-input v-model="smallForm.smallName" maxlength="100" />
        </el-form-item>
        <el-form-item label="责任主体">
          <el-input v-model="smallForm.responsibilitySubject" placeholder="处置单位" />
        </el-form-item>
        <el-form-item label="监管主体">
          <el-input v-model="smallForm.superviseSubject" placeholder="主管部门" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="smallForm.sortOrder" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="smallForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="smallForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="smallVisible = false">取消</el-button>
        <el-button type="primary" :loading="smallSaving" @click="submitSmall">保存</el-button>
      </template>
    </el-dialog>

    <!-- 立案条件对话框 -->
    <el-dialog
      v-model="standardVisible"
      :title="standardForm.id ? '编辑立案条件' : '新增立案条件'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="standardFormRef" :model="standardForm" :rules="standardRules" label-width="100px">
        <el-form-item label="立案条件" prop="conditionDesc">
          <el-input v-model="standardForm.conditionDesc" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="结案条件" prop="closeCondition">
          <el-input v-model="standardForm.closeCondition" maxlength="200" />
        </el-form-item>
        <el-form-item label="时限类型" prop="handleTimeType">
          <el-select v-model="standardForm.handleTimeType" style="width: 100%">
            <el-option
              v-for="opt in TIME_LIMIT_TYPE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="时限数值" prop="handleTimeValue">
          <el-input-number v-model="standardForm.handleTimeValue" :min="1" :max="999" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="standardForm.sortOrder" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="standardForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="standardVisible = false">取消</el-button>
        <el-button type="primary" :loading="standardSaving" @click="submitStandard">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { RoleCode } from '@/utils/roleAccess'
import {
  getCategoryBigManageList,
  saveCategoryBig,
  deleteCategoryBig,
  getCategorySmallManageList,
  saveCategorySmall,
  deleteCategorySmall,
  getStandardManageList,
  saveCaseStandard,
  deleteCaseStandard
} from '@/api/config'

const TIME_LIMIT_TYPE_OPTIONS = [
  { value: 'urgent_hour', label: '紧急工作时' },
  { value: 'work_hour', label: '工作时' },
  { value: 'work_day', label: '工作日' },
  { value: 'natural_day', label: '自然日' }
]

const TYPE_NAME_MAP = Object.fromEntries(TIME_LIMIT_TYPE_OPTIONS.map((o) => [o.value, o.label]))

const router = useRouter()
const userStore = useUserStore()
const isAdmin = computed(() => (userStore.roles || []).includes(RoleCode.ADMIN))

const categoryType = ref(2)
const bigLoading = ref(false)
const bigList = ref([])
const selectedBig = ref(null)

const smallLoading = ref(false)
const smallList = ref([])
const selectedSmall = ref(null)

const standardLoading = ref(false)
const standardList = ref([])

const bigVisible = ref(false)
const bigSaving = ref(false)
const bigFormRef = ref()
const bigForm = reactive({
  id: null,
  bigCode: '',
  bigName: '',
  description: '',
  sortOrder: 0,
  status: 1
})
const bigRules = {
  bigCode: [{ required: true, message: '请填写大类编码', trigger: 'blur' }],
  bigName: [{ required: true, message: '请填写大类名称', trigger: 'blur' }]
}

const smallVisible = ref(false)
const smallSaving = ref(false)
const smallFormRef = ref()
const smallForm = reactive({
  id: null,
  bigId: null,
  smallCode: '',
  smallName: '',
  responsibilitySubject: '',
  superviseSubject: '',
  description: '',
  sortOrder: 0,
  status: 1
})
const smallRules = {
  smallCode: [{ required: true, message: '请填写小类编码', trigger: 'blur' }],
  smallName: [{ required: true, message: '请填写小类名称', trigger: 'blur' }]
}

const standardVisible = ref(false)
const standardSaving = ref(false)
const standardFormRef = ref()
const standardForm = reactive({
  id: null,
  smallId: null,
  conditionDesc: '',
  closeCondition: '',
  handleTimeType: 'work_hour',
  handleTimeValue: 4,
  sortOrder: 0,
  status: 1
})
const standardRules = {
  conditionDesc: [{ required: true, message: '请填写立案条件', trigger: 'blur' }],
  closeCondition: [{ required: true, message: '请填写结案条件', trigger: 'blur' }],
  handleTimeType: [{ required: true, message: '请选择时限类型', trigger: 'change' }],
  handleTimeValue: [{ required: true, message: '请填写时限数值', trigger: 'blur' }]
}

onMounted(() => {
  loadBigList()
})

function goStandardImport() {
  router.push('/config/standard')
}

function formatTimeLimit(row) {
  if (row.handleTimeLimit) return row.handleTimeLimit
  const label = TYPE_NAME_MAP[row.handleTimeType] || row.handleTimeType
  return row.handleTimeValue != null ? `${row.handleTimeValue}${label}` : label || '—'
}

async function loadBigList() {
  bigLoading.value = true
  selectedBig.value = null
  selectedSmall.value = null
  smallList.value = []
  standardList.value = []
  try {
    const res = await getCategoryBigManageList({ type: categoryType.value })
    bigList.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    bigLoading.value = false
  }
}

function onTypeChange() {
  loadBigList()
}

function onBigSelect(row) {
  selectedBig.value = row || null
  selectedSmall.value = null
  standardList.value = []
  if (row?.id) {
    loadSmallList(row.id)
  } else {
    smallList.value = []
  }
}

async function loadSmallList(bigId) {
  smallLoading.value = true
  try {
    const res = await getCategorySmallManageList(bigId)
    smallList.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    smallLoading.value = false
  }
}

function onSmallSelect(row) {
  selectedSmall.value = row || null
  if (row?.id) {
    loadStandardList(row.id)
  } else {
    standardList.value = []
  }
}

async function loadStandardList(smallId) {
  standardLoading.value = true
  try {
    const res = await getStandardManageList(smallId)
    standardList.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    standardLoading.value = false
  }
}

function openBigDialog(row) {
  bigForm.id = row?.id ?? null
  bigForm.bigCode = row?.bigCode ?? ''
  bigForm.bigName = row?.bigName ?? ''
  bigForm.description = row?.description ?? ''
  bigForm.sortOrder = row?.sortOrder ?? 0
  bigForm.status = row?.status ?? 1
  bigVisible.value = true
}

async function submitBig() {
  await bigFormRef.value?.validate()
  bigSaving.value = true
  try {
    const categoryTypeStr = categoryType.value === 1 ? 'component' : 'event'
    await saveCategoryBig({
      id: bigForm.id,
      categoryType: categoryTypeStr,
      bigCode: bigForm.bigCode,
      bigName: bigForm.bigName,
      description: bigForm.description,
      sortOrder: bigForm.sortOrder,
      status: bigForm.status
    })
    ElMessage.success('保存成功')
    bigVisible.value = false
    await loadBigList()
  } catch (e) {
    console.error(e)
  } finally {
    bigSaving.value = false
  }
}

async function removeBig(row) {
  try {
    await ElMessageBox.confirm(`确定删除大类「${row.bigName}」？`, '提示', { type: 'warning' })
    await deleteCategoryBig(row.id)
    ElMessage.success('已删除')
    await loadBigList()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

function openSmallDialog(row) {
  if (!selectedBig.value && !row) return
  smallForm.id = row?.id ?? null
  smallForm.bigId = selectedBig.value?.id ?? row?.bigId
  smallForm.smallCode = row?.smallCode ?? ''
  smallForm.smallName = row?.smallName ?? ''
  smallForm.responsibilitySubject = row?.responsibilitySubject ?? ''
  smallForm.superviseSubject = row?.superviseSubject ?? ''
  smallForm.description = row?.description ?? ''
  smallForm.sortOrder = row?.sortOrder ?? 0
  smallForm.status = row?.status ?? 1
  smallVisible.value = true
}

async function submitSmall() {
  await smallFormRef.value?.validate()
  smallSaving.value = true
  try {
    await saveCategorySmall({
      id: smallForm.id,
      bigId: smallForm.bigId,
      smallCode: smallForm.smallCode,
      smallName: smallForm.smallName,
      responsibilitySubject: smallForm.responsibilitySubject,
      superviseSubject: smallForm.superviseSubject,
      description: smallForm.description,
      sortOrder: smallForm.sortOrder,
      status: smallForm.status
    })
    ElMessage.success('保存成功')
    smallVisible.value = false
    if (selectedBig.value?.id) {
      await loadSmallList(selectedBig.value.id)
    }
  } catch (e) {
    console.error(e)
  } finally {
    smallSaving.value = false
  }
}

async function removeSmall(row) {
  try {
    await ElMessageBox.confirm(`确定删除小类「${row.smallName}」？`, '提示', { type: 'warning' })
    await deleteCategorySmall(row.id)
    ElMessage.success('已删除')
    selectedSmall.value = null
    standardList.value = []
    if (selectedBig.value?.id) {
      await loadSmallList(selectedBig.value.id)
    }
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

function openStandardDialog(row) {
  if (!selectedSmall.value && !row) return
  standardForm.id = row?.id ?? null
  standardForm.smallId = selectedSmall.value?.id ?? row?.smallId
  standardForm.conditionDesc = row?.conditionContent || row?.conditionDesc || ''
  standardForm.closeCondition = row?.closeCondition ?? ''
  standardForm.handleTimeType = row?.handleTimeType ?? 'work_hour'
  standardForm.handleTimeValue = row?.handleTimeValue ?? 4
  standardForm.sortOrder = row?.sortOrder ?? 0
  standardForm.status = row?.status ?? 1
  standardVisible.value = true
}

async function submitStandard() {
  await standardFormRef.value?.validate()
  standardSaving.value = true
  try {
    await saveCaseStandard({
      id: standardForm.id,
      smallId: standardForm.smallId,
      conditionDesc: standardForm.conditionDesc,
      closeCondition: standardForm.closeCondition,
      handleTimeType: standardForm.handleTimeType,
      handleTimeValue: standardForm.handleTimeValue,
      sortOrder: standardForm.sortOrder,
      status: standardForm.status
    })
    ElMessage.success('保存成功')
    standardVisible.value = false
    if (selectedSmall.value?.id) {
      await loadStandardList(selectedSmall.value.id)
    }
  } catch (e) {
    console.error(e)
  } finally {
    standardSaving.value = false
  }
}

async function removeStandard(row) {
  const name = row.conditionContent || row.conditionDesc || '该条'
  try {
    await ElMessageBox.confirm(`确定删除立案条件「${name}」？`, '提示', { type: 'warning' })
    await deleteCaseStandard(row.id)
    ElMessage.success('已删除')
    if (selectedSmall.value?.id) {
      await loadStandardList(selectedSmall.value.id)
    }
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}
</script>

<style lang="scss" scoped>
.category-manage {
  .intro-card {
    margin-bottom: 16px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .toolbar-card {
    margin-bottom: 16px;

    .ml-2 {
      margin-left: 8px;
    }
  }

  .panels {
    display: grid;
    grid-template-columns: 1fr 1fr 1.4fr;
    gap: 16px;
  }

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  @media (max-width: 1200px) {
    .panels {
      grid-template-columns: 1fr;
    }
  }
}
</style>
