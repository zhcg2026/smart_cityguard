<template>
  <div class="user-manage">
    <el-row :gutter="16">
      <!-- 左侧：组织架构（部门树） -->
      <el-col :span="6">
        <el-card class="dept-card">
          <template #header>
            <div class="header-with-action">
              <span>组织架构</span>
              <el-button type="primary" link @click="goDeptManage">维护部门</el-button>
            </div>
          </template>
          <el-input
            v-model="deptFilter"
            placeholder="筛选部门"
            clearable
            class="dept-filter"
          />
          <el-tree
            ref="deptTreeRef"
            v-loading="deptLoading"
            :data="deptTreeDisplay"
            :props="{ label: 'deptName', children: 'children' }"
            node-key="id"
            highlight-current
            default-expand-all
            :filter-node-method="filterDeptNode"
            @node-click="onDeptNodeClick"
          />
        </el-card>
      </el-col>

      <!-- 右侧：部门下人员 -->
      <el-col :span="18">
        <el-card class="search-card">
          <el-form :model="searchForm" inline>
            <el-form-item label="用户名">
              <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
            </el-form-item>
            <el-form-item label="姓名">
              <el-input v-model="searchForm.realName" placeholder="请输入姓名" clearable />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 100px">
                <el-option label="正常" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">搜索</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="list-card">
          <template #header>
            <div class="header-with-action">
              <span>{{ selectedDeptTitle }}</span>
              <el-button type="primary" :disabled="!selectedDeptId || unassignedOnly" @click="handleAdd">
                在本部门添加人员
              </el-button>
            </div>
          </template>

          <el-alert
            v-if="unassignedOnly"
            type="warning"
            :closable="false"
            show-icon
            title="以下人员未归属任何部门（常见于测试账号）。网格分配等下拉会列出全部采集员角色用户，与左侧部门筛选无关；可在此删除或编辑后指定部门。"
            class="dept-tip"
          />
          <el-alert
            v-else-if="!selectedDeptId"
            type="info"
            :closable="false"
            show-icon
            title="当前为全部部门人员。若某用户在具体部门下找不到，请点左侧「未分配部门」。添加人员请先选择具体部门。"
            class="dept-tip"
          />

          <el-table v-loading="loading" :data="userList" style="width: 100%">
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column prop="realName" label="姓名" width="100" />
            <el-table-column prop="roleNames" label="角色" min-width="140" show-overflow-tooltip />
            <el-table-column prop="phone" label="手机号" width="130" />
            <el-table-column label="所属部门" width="150" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.systemProtected ? '—（系统管理员）' : (row.departmentName || '—') }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                  {{ row.status === 1 ? '正常' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <template v-if="row.systemProtected">
                  <span class="protected-hint">系统管理员</span>
                </template>
                <template v-else-if="row.deptLoginAccount">
                  <span class="protected-hint">部门登录账号</span>
                </template>
                <template v-else>
                  <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
                  <el-button type="warning" size="small" link @click="handleResetPwd(row)">重置密码</el-button>
                  <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            layout="total, sizes, prev, pager, next"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="userForm" :rules="userRules" label-width="90px">
        <el-form-item v-if="!isFormAdminRole" label="所属部门" prop="departmentId">
          <el-tree-select
            v-model="userForm.departmentId"
            :data="deptTreeForSelect"
            :props="{ label: 'deptName', value: 'id', children: 'children' }"
            placeholder="请选择部门"
            check-strictly
            filterable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-else label="所属部门">
          <span class="protected-hint">系统管理员不归属任何部门</span>
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="isEdit" placeholder="登录账号" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="userForm.password" type="password" placeholder="默认 123456" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="userForm.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="userForm.roleIds" multiple placeholder="请选择岗位角色" style="width: 100%">
            <el-option v-for="role in roleList" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
          <div class="form-hint">处置部门人员请选择「处置人员」；派遣员、受理员等选对应角色</div>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="可选" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="可选" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetPwdVisible" title="重置密码" width="300px">
      <el-form :model="resetPwdForm" label-width="80px">
        <el-form-item label="新密码">
          <el-input v-model="resetPwdForm.password" type="password" placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResetPwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getUserList,
  getUserDetail,
  getUserRoles,
  createUser,
  updateUser,
  deleteUser,
  resetPassword,
  getDeptTree,
  getRoleList
} from '@/api/system'
import { RoleCode } from '@/utils/roleAccess'

const router = useRouter()

const deptLoading = ref(false)
const deptTreeRaw = ref([])
const deptTreeForSelect = ref([])
const deptTreeRef = ref(null)
const deptFilter = ref('')
const selectedDeptId = ref(null)
const selectedDeptName = ref('')
/** 左侧选中「未分配部门」时为 true */
const unassignedOnly = ref(false)

const loading = ref(false)
const userList = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  username: '',
  realName: '',
  status: null
})

const roleList = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref()

const userForm = reactive({
  id: null,
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  departmentId: null,
  roleIds: [],
  status: 1
})

const adminRoleId = computed(() => roleList.value.find((r) => r.roleCode === RoleCode.ADMIN)?.id)

const isFormAdminRole = computed(() => {
  const id = adminRoleId.value
  return id != null && userForm.roleIds?.includes(id)
})

const userRules = computed(() => ({
  departmentId: isFormAdminRole.value
    ? []
    : [{ required: true, message: '请选择所属部门', trigger: 'change' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleIds: [{ required: true, message: '请选择角色', trigger: 'change' }]
}))

watch(
  () => userForm.roleIds?.slice(),
  () => {
    if (isFormAdminRole.value) {
      userForm.departmentId = null
    }
  }
)

const resetPwdVisible = ref(false)
const resetPwdForm = reactive({ userId: null, password: '' })

const selectedDeptTitle = computed(() => {
  if (unassignedOnly.value) {
    return '未分配部门 · 人员列表'
  }
  if (!selectedDeptId.value) {
    return '全部部门 · 人员列表'
  }
  return `${selectedDeptName.value || '部门'} · 人员列表`
})

const deptTreeDisplay = computed(() => {
  const allNode = { id: '__all__', deptName: '全部部门', children: deptTreeForSelect.value }
  const unassignedNode = { id: '__unassigned__', deptName: '未分配部门' }
  return [allNode, unassignedNode]
})

watch(deptFilter, (val) => {
  deptTreeRef.value?.filter(val)
})

function filterDeptNode(value, data) {
  if (!value) return true
  return data.deptName?.includes(value)
}

function buildTree(list, parentId) {
  const tree = []
  const pid = parentId == null ? 0 : parentId
  for (const item of list) {
    const itemParent = item.parentId == null ? 0 : item.parentId
    if (itemParent === pid) {
      const children = buildTree(list, item.id)
      const node = { ...item }
      if (children.length > 0) {
        node.children = children
      }
      tree.push(node)
    }
  }
  return tree
}

onMounted(async () => {
  await loadDeptTree()
  await loadRoleList()
  await loadUserList()
})

async function loadDeptTree() {
  deptLoading.value = true
  try {
    const res = await getDeptTree()
    const list = res.data || []
    deptTreeRaw.value = list
    deptTreeForSelect.value = buildTree(list, 0)
    if (!selectedDeptId.value && deptTreeForSelect.value.length > 0) {
      const first = findFirstDept(deptTreeForSelect.value)
      if (first) {
        selectedDeptId.value = first.id
        selectedDeptName.value = first.deptName
        await loadUserList()
      }
    }
  } catch (error) {
    console.error('获取部门树失败:', error)
  } finally {
    deptLoading.value = false
  }
}

function findFirstDept(nodes) {
  if (!nodes?.length) return null
  return nodes[0]
}

function onDeptNodeClick(data) {
  if (data.id === '__unassigned__') {
    unassignedOnly.value = true
    selectedDeptId.value = null
    selectedDeptName.value = '未分配部门'
  } else if (data.id === '__all__') {
    unassignedOnly.value = false
    selectedDeptId.value = null
    selectedDeptName.value = ''
  } else {
    unassignedOnly.value = false
    selectedDeptId.value = data.id
    selectedDeptName.value = data.deptName
  }
  pageNum.value = 1
  loadUserList()
}

function goDeptManage() {
  router.push('/system/dept')
}

async function loadRoleList() {
  try {
    const res = await getRoleList()
    roleList.value = (res.data || []).filter((r) => r.roleCode !== RoleCode.DEPT)
  } catch (error) {
    console.error('获取角色列表失败:', error)
  }
}

async function loadUserList() {
  loading.value = true
  try {
    const params = {
      pageNum: unassignedOnly.value ? 1 : pageNum.value,
      pageSize: unassignedOnly.value ? 200 : pageSize.value,
      ...searchForm
    }
    if (unassignedOnly.value) {
      params.unassignedOnly = 'true'
    } else if (selectedDeptId.value) {
      params.departmentId = selectedDeptId.value
    }
    const res = await getUserList(params)
    let records = res.data?.records || []
    if (unassignedOnly.value) {
      records = records.filter((u) => u.departmentId == null || u.departmentId === 0)
      total.value = records.length
    } else {
      total.value = res.data?.total || 0
    }
    userList.value = records
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadUserList()
}

function handleReset() {
  searchForm.username = ''
  searchForm.realName = ''
  searchForm.status = null
  pageNum.value = 1
  loadUserList()
}

function handleSizeChange(size) {
  pageSize.value = size
  loadUserList()
}

function handleCurrentChange(page) {
  pageNum.value = page
  loadUserList()
}

function handleAdd() {
  if (!selectedDeptId.value) {
    ElMessage.warning('请先在左侧选择部门')
    return
  }
  isEdit.value = false
  dialogTitle.value = `添加人员 · ${selectedDeptName.value}`
  userForm.id = null
  userForm.username = ''
  userForm.password = ''
  userForm.realName = ''
  userForm.phone = ''
  userForm.email = ''
  userForm.departmentId = selectedDeptId.value
  userForm.roleIds = []
  userForm.status = 1
  dialogVisible.value = true
}

async function handleEdit(row) {
  if (row.systemProtected) {
    ElMessage.warning('系统管理员账号不可编辑')
    return
  }
  if (row.deptLoginAccount) {
    ElMessage.warning('部门登录账号请在「部门管理」中维护')
    return
  }
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  try {
    const userRes = await getUserDetail(row.id)
    const roleRes = await getUserRoles(row.id)
    Object.assign(userForm, userRes.data)
    userForm.roleIds = roleRes.data?.map((r) => r.id) || []
    dialogVisible.value = true
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

async function submitForm() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    const userData = {
      username: userForm.username,
      password: userForm.password,
      realName: userForm.realName,
      phone: userForm.phone,
      email: userForm.email,
      departmentId: isFormAdminRole.value ? null : userForm.departmentId,
      status: userForm.status
    }
    if (isEdit.value) {
      userData.id = userForm.id
    }
    const data = { user: userData, roleIds: userForm.roleIds }
    if (isEdit.value) {
      await updateUser(data)
      ElMessage.success('更新成功')
    } else {
      await createUser(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadUserList()
  } catch (error) {
    console.error('保存用户失败:', error)
  }
}

function handleResetPwd(row) {
  if (row.systemProtected) {
    ElMessage.warning('系统管理员账号不可重置密码')
    return
  }
  if (row.deptLoginAccount) {
    ElMessage.warning('部门登录账号请在「部门管理」中重置密码')
    return
  }
  resetPwdForm.userId = row.id
  resetPwdForm.password = ''
  resetPwdVisible.value = true
}

async function submitResetPwd() {
  try {
    await resetPassword(resetPwdForm.userId, resetPwdForm.password)
    ElMessage.success('密码重置成功')
    resetPwdVisible.value = false
  } catch (error) {
    console.error('重置密码失败:', error)
  }
}

async function handleDelete(row) {
  if (row.systemProtected) {
    ElMessage.warning('系统管理员账号不可删除')
    return
  }
  if (row.deptLoginAccount) {
    ElMessage.warning('部门登录账号随部门删除，不可单独删除')
    return
  }
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？', '提示', { type: 'warning' })
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    await loadUserList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除用户失败:', error)
    }
  }
}
</script>

<style lang="scss" scoped>
.user-manage {
  .dept-card {
    min-height: 520px;
    .dept-filter {
      margin-bottom: 12px;
    }
  }
  .search-card {
    margin-bottom: 16px;
  }
  .list-card {
    .header-with-action {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .dept-tip {
      margin-bottom: 16px;
    }
    .el-pagination {
      margin-top: 20px;
      justify-content: flex-end;
    }
  }
  .form-hint {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    line-height: 1.4;
    margin-top: 4px;
  }
  .protected-hint {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }
}
</style>
