<template>
  <div class="page-container content-publish">
    <el-card>
      <template #header><span>内容发布</span></template>

      <el-tabs v-model="activeTab">
        <!-- 公文通告 -->
        <el-tab-pane label="公文通告" name="announcement">
          <div class="toolbar">
            <el-button type="primary" @click="openAnnouncementDialog()">发布通告</el-button>
          </div>
          <el-table v-loading="announcementLoading" :data="announcementList" border stripe>
            <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
            <el-table-column label="类型" width="100">
              <template #default="{ row }">
                {{ announcementTypeLabel(row.announcementType) }}
              </template>
            </el-table-column>
            <el-table-column label="可见范围" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">{{ formatReceiverScope(row) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 'published' ? 'success' : 'info'" size="small">
                  {{ row.status === 'published' ? '已发布' : '草稿' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="publishTime" label="发布时间" width="170" />
            <el-table-column prop="expireTime" label="过期时间" width="170">
              <template #default="{ row }">{{ row.expireTime || '—' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="openAnnouncementDialog(row)">编辑</el-button>
                <el-button type="danger" link @click="handleDeleteAnnouncement(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 今日提示 -->
        <el-tab-pane label="今日提示" name="dailytip">
          <div class="toolbar">
            <el-button type="primary" @click="openDailyTipDialog()">发布提示</el-button>
          </div>
          <el-table v-loading="dailyTipLoading" :data="dailyTipList" border stripe>
            <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
            <el-table-column prop="content" label="内容摘要" min-width="220" show-overflow-tooltip />
            <el-table-column label="可见范围" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">{{ formatReceiverScope(row) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 'published' ? 'success' : 'info'" size="small">
                  {{ row.status === 'published' ? '已发布' : '草稿' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="publishTime" label="发布时间" width="170" />
            <el-table-column prop="expireTime" label="过期时间" width="170">
              <template #default="{ row }">{{ row.expireTime || '—' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="openDailyTipDialog(row)">编辑</el-button>
                <el-button type="danger" link @click="handleDeleteDailyTip(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 通告表单 -->
    <el-dialog v-model="announcementDialogVisible" :title="announcementForm.id ? '编辑通告' : '发布通告'" width="640px" destroy-on-close>
      <el-form ref="announcementFormRef" :model="announcementForm" :rules="formRules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="announcementForm.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="类型" prop="announcementType">
          <el-select v-model="announcementForm.announcementType" style="width: 100%">
            <el-option label="系统通知" value="system" />
            <el-option label="业务通告" value="business" />
            <el-option label="紧急通告" value="urgent" />
          </el-select>
        </el-form-item>
        <el-form-item label="文号">
          <el-input v-model="announcementForm.docNumber" placeholder="可选" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="announcementForm.content" type="textarea" :rows="5" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="可见范围" prop="receiverType">
          <el-radio-group v-model="announcementForm.receiverType">
            <el-radio value="all">全部用户</el-radio>
            <el-radio value="role">指定角色</el-radio>
            <el-radio value="user">指定用户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="announcementForm.receiverType === 'role'" label="选择角色" prop="receiverRoleCodes">
          <el-select v-model="announcementForm.receiverRoleCodes" multiple filterable style="width: 100%" placeholder="选择可见角色">
            <el-option v-for="role in roleOptions" :key="role.roleCode" :label="role.roleName" :value="role.roleCode" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="announcementForm.receiverType === 'user'" label="选择用户" prop="receiverUserIds">
          <el-select v-model="announcementForm.receiverUserIds" multiple filterable style="width: 100%" placeholder="选择可见用户">
            <el-option
              v-for="user in userOptions"
              :key="user.id"
              :label="`${user.realName || user.username}（${user.username}）`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="announcementForm.expireTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="留空表示长期有效"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="announcementForm.isTop" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="announcementDialogVisible = false">取消</el-button>
        <el-button :loading="announcementSaving" @click="submitAnnouncement(false)">存草稿</el-button>
        <el-button type="primary" :loading="announcementSaving" @click="submitAnnouncement(true)">发布</el-button>
      </template>
    </el-dialog>

    <!-- 今日提示表单 -->
    <el-dialog v-model="dailyTipDialogVisible" :title="dailyTipForm.id ? '编辑提示' : '发布提示'" width="640px" destroy-on-close>
      <el-form ref="dailyTipFormRef" :model="dailyTipForm" :rules="formRules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="dailyTipForm.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="dailyTipForm.content" type="textarea" :rows="5" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="可见范围" prop="receiverType">
          <el-radio-group v-model="dailyTipForm.receiverType">
            <el-radio value="all">全部用户</el-radio>
            <el-radio value="role">指定角色</el-radio>
            <el-radio value="user">指定用户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="dailyTipForm.receiverType === 'role'" label="选择角色" prop="receiverRoleCodes">
          <el-select v-model="dailyTipForm.receiverRoleCodes" multiple filterable style="width: 100%" placeholder="选择可见角色">
            <el-option v-for="role in roleOptions" :key="role.roleCode" :label="role.roleName" :value="role.roleCode" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="dailyTipForm.receiverType === 'user'" label="选择用户" prop="receiverUserIds">
          <el-select v-model="dailyTipForm.receiverUserIds" multiple filterable style="width: 100%" placeholder="选择可见用户">
            <el-option
              v-for="user in userOptions"
              :key="user.id"
              :label="`${user.realName || user.username}（${user.username}）`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="dailyTipForm.expireTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="留空表示长期有效"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dailyTipDialogVisible = false">取消</el-button>
        <el-button :loading="dailyTipSaving" @click="submitDailyTip(false)">存草稿</el-button>
        <el-button type="primary" :loading="dailyTipSaving" @click="submitDailyTip(true)">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAnnouncementAdminList,
  createAnnouncement,
  updateAnnouncement,
  deleteAnnouncement,
  getDailyTipAdminList,
  createDailyTip,
  updateDailyTip,
  deleteDailyTip
} from '@/api/config'
import { getRoleList, getUserList } from '@/api/system'
import { roleNameMap } from '@/utils/roleAccess'

const activeTab = ref('announcement')
const announcementLoading = ref(false)
const dailyTipLoading = ref(false)
const announcementSaving = ref(false)
const dailyTipSaving = ref(false)
const announcementList = ref([])
const dailyTipList = ref([])
const roleOptions = ref([])
const userOptions = ref([])

const announcementDialogVisible = ref(false)
const dailyTipDialogVisible = ref(false)
const announcementFormRef = ref()
const dailyTipFormRef = ref()

const announcementForm = reactive(createEmptyAnnouncementForm())
const dailyTipForm = reactive(createEmptyDailyTipForm())

const formRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

function createEmptyAnnouncementForm() {
  return {
    id: null,
    title: '',
    content: '',
    announcementType: 'business',
    docNumber: '',
    receiverType: 'all',
    receiverRoleCodes: [],
    receiverUserIds: [],
    expireTime: '',
    isTop: 0
  }
}

function createEmptyDailyTipForm() {
  return {
    id: null,
    title: '',
    content: '',
    receiverType: 'all',
    receiverRoleCodes: [],
    receiverUserIds: [],
    expireTime: ''
  }
}

onMounted(async () => {
  await Promise.all([loadMetaOptions(), loadAnnouncementList(), loadDailyTipList()])
})

async function loadMetaOptions() {
  try {
    const [roleRes, userRes] = await Promise.all([
      getRoleList(),
      getUserList({ pageNum: 1, pageSize: 200 })
    ])
    roleOptions.value = roleRes.data || []
    userOptions.value = userRes.data?.records || []
  } catch (error) {
    console.error('加载角色/用户失败:', error)
  }
}

async function loadAnnouncementList() {
  announcementLoading.value = true
  try {
    const res = await getAnnouncementAdminList()
    announcementList.value = res.data || []
  } catch (error) {
    console.error('加载通告列表失败:', error)
  } finally {
    announcementLoading.value = false
  }
}

async function loadDailyTipList() {
  dailyTipLoading.value = true
  try {
    const res = await getDailyTipAdminList()
    dailyTipList.value = res.data || []
  } catch (error) {
    console.error('加载今日提示失败:', error)
  } finally {
    dailyTipLoading.value = false
  }
}

function announcementTypeLabel(type) {
  return ({ system: '系统通知', business: '业务通告', urgent: '紧急通告' }[type] || type || '—')
}

function formatReceiverScope(row) {
  const type = row.receiverType || 'all'
  if (type === 'all') return '全部用户'
  if (type === 'collector') return '采集员'
  if (type === 'admin') return '管理员'
  if (!row.receiverIds) return '—'
  if (type === 'role') {
    return row.receiverIds.split(',').map((code) => roleNameMap[code.trim()] || code.trim()).join('、')
  }
  if (type === 'user') {
    const ids = row.receiverIds.split(',').map((s) => s.trim())
    const names = ids.map((id) => {
      const user = userOptions.value.find((u) => String(u.id) === id)
      return user ? (user.realName || user.username) : id
    })
    return names.join('、')
  }
  return row.receiverIds
}

function fillReceiverFields(form, row) {
  form.receiverType = row.receiverType || 'all'
  form.receiverRoleCodes = []
  form.receiverUserIds = []
  if (form.receiverType === 'role' && row.receiverIds) {
    form.receiverRoleCodes = row.receiverIds.split(',').map((s) => s.trim()).filter(Boolean)
  }
  if (form.receiverType === 'user' && row.receiverIds) {
    form.receiverUserIds = row.receiverIds.split(',').map((s) => Number(s.trim())).filter((n) => !Number.isNaN(n))
  }
}

/** 接口返回 ISO 时间，转为日期控件 value-format */
function normalizePickerDatetime(value) {
  if (value == null || value === '') return ''
  return String(value).replace('T', ' ').slice(0, 19)
}

function buildReceiverIds(form) {
  if (form.receiverType === 'role') {
    return (form.receiverRoleCodes || []).join(',')
  }
  if (form.receiverType === 'user') {
    return (form.receiverUserIds || []).join(',')
  }
  return null
}

function openAnnouncementDialog(row) {
  Object.assign(announcementForm, createEmptyAnnouncementForm())
  if (row) {
    announcementForm.id = row.id
    announcementForm.title = row.title
    announcementForm.content = row.content
    announcementForm.announcementType = row.announcementType || 'business'
    announcementForm.docNumber = row.docNumber || ''
    announcementForm.expireTime = normalizePickerDatetime(row.expireTime)
    announcementForm.isTop = row.isTop || 0
    fillReceiverFields(announcementForm, row)
  }
  announcementDialogVisible.value = true
}

function openDailyTipDialog(row) {
  Object.assign(dailyTipForm, createEmptyDailyTipForm())
  if (row) {
    dailyTipForm.id = row.id
    dailyTipForm.title = row.title
    dailyTipForm.content = row.content
    dailyTipForm.expireTime = normalizePickerDatetime(row.expireTime)
    fillReceiverFields(dailyTipForm, row)
  }
  dailyTipDialogVisible.value = true
}

async function submitAnnouncement(publish) {
  await announcementFormRef.value?.validate()
  if (announcementForm.receiverType === 'role' && !announcementForm.receiverRoleCodes?.length) {
    ElMessage.warning('请选择可见角色')
    return
  }
  if (announcementForm.receiverType === 'user' && !announcementForm.receiverUserIds?.length) {
    ElMessage.warning('请选择可见用户')
    return
  }
  announcementSaving.value = true
  try {
    const payload = {
      publish,
      announcement: {
        id: announcementForm.id,
        title: announcementForm.title,
        content: announcementForm.content,
        announcementType: announcementForm.announcementType,
        docNumber: announcementForm.docNumber || null,
        receiverType: announcementForm.receiverType,
        receiverIds: buildReceiverIds(announcementForm),
        expireTime: announcementForm.expireTime || null,
        isTop: announcementForm.isTop
      }
    }
    if (announcementForm.id) {
      await updateAnnouncement(announcementForm.id, payload)
    } else {
      await createAnnouncement(payload)
    }
    ElMessage.success(publish ? '通告已发布' : '草稿已保存')
    announcementDialogVisible.value = false
    await loadAnnouncementList()
  } catch (error) {
    console.error('保存通告失败:', error)
  } finally {
    announcementSaving.value = false
  }
}

async function submitDailyTip(publish) {
  await dailyTipFormRef.value?.validate()
  if (dailyTipForm.receiverType === 'role' && !dailyTipForm.receiverRoleCodes?.length) {
    ElMessage.warning('请选择可见角色')
    return
  }
  if (dailyTipForm.receiverType === 'user' && !dailyTipForm.receiverUserIds?.length) {
    ElMessage.warning('请选择可见用户')
    return
  }
  dailyTipSaving.value = true
  try {
    const payload = {
      publish,
      dailyTip: {
        id: dailyTipForm.id,
        title: dailyTipForm.title,
        content: dailyTipForm.content,
        receiverType: dailyTipForm.receiverType,
        receiverIds: buildReceiverIds(dailyTipForm),
        expireTime: dailyTipForm.expireTime || null
      }
    }
    if (dailyTipForm.id) {
      await updateDailyTip(dailyTipForm.id, payload)
    } else {
      await createDailyTip(payload)
    }
    ElMessage.success(publish ? '提示已发布' : '草稿已保存')
    dailyTipDialogVisible.value = false
    await loadDailyTipList()
  } catch (error) {
    console.error('保存提示失败:', error)
  } finally {
    dailyTipSaving.value = false
  }
}

async function handleDeleteAnnouncement(row) {
  await ElMessageBox.confirm(`确定删除通告「${row.title}」？`, '删除确认', { type: 'warning' })
  await deleteAnnouncement(row.id)
  ElMessage.success('已删除')
  await loadAnnouncementList()
}

async function handleDeleteDailyTip(row) {
  await ElMessageBox.confirm(`确定删除提示「${row.title}」？`, '删除确认', { type: 'warning' })
  await deleteDailyTip(row.id)
  ElMessage.success('已删除')
  await loadDailyTipList()
}
</script>

<style lang="scss" scoped>
.content-publish {
  .toolbar {
    margin-bottom: 16px;
  }
}
</style>
