<template>
  <div class="dept-manage">
    <!-- 部门树 -->
    <el-card class="tree-card">
      <template #header>
        <div class="header-with-action">
          <span>部门管理</span>
          <el-button type="primary" @click="handleAdd(null)">新增部门</el-button>
        </div>
      </template>

      <el-tree
        :data="deptTree"
        :props="{ label: 'deptName', children: 'children' }"
        node-key="id"
        default-expand-all
        :expand-on-click-node="false"
      >
        <template #default="{ node, data }">
          <div class="tree-node">
            <span>
              {{ data.deptName }}
              <span v-if="data.deptLevel >= 1" class="login-tag">
                （{{ data.loginUsername ? `登录: ${data.loginUsername}` : '未配置登录账号' }}）
              </span>
            </span>
            <span class="tree-actions">
              <el-button type="primary" size="small" link @click="handleAdd(data)">新增</el-button>
              <el-button type="primary" size="small" link @click="handleEdit(data)">编辑</el-button>
              <el-button
                v-if="data.deptLevel >= 1 && !data.loginUsername"
                type="success"
                size="small"
                link
                @click="handleEnsureLogin(data)"
              >
                创建登录账号
              </el-button>
              <el-button
                v-if="data.loginUsername"
                type="warning"
                size="small"
                link
                @click="handleResetDeptPwd(data)"
              >
                重置密码
              </el-button>
              <el-button type="danger" size="small" link @click="handleDelete(data)">删除</el-button>
            </span>
          </div>
        </template>
      </el-tree>
    </el-card>

    <!-- 部门编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="400px">
      <el-form ref="formRef" :model="deptForm" :rules="deptRules" label-width="80px">
        <el-form-item label="上级部门" prop="parentId">
          <el-tree-select
            v-model="deptForm.parentId"
            :data="deptTree"
            :props="{ label: 'deptName', value: 'id' }"
            placeholder="请选择上级部门"
            check-strictly
            clearable
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="deptForm.deptName" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="部门层级" prop="deptLevel">
          <el-select v-model="deptForm.deptLevel" placeholder="请选择部门层级">
            <el-option label="一级部门" :value="1" />
            <el-option label="二级部门" :value="2" />
            <el-option label="三级部门" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="deptForm.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="deptForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="deptForm.deptLevel >= 1 && deptForm.loginUsername" label="登录账号">
          <el-input :model-value="deptForm.loginUsername" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDeptTree,
  getDeptDetail,
  createDept,
  updateDept,
  deleteDept,
  ensureDeptLogin,
  resetDeptLoginPassword
} from '@/api/system'

const deptTree = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref()

const deptForm = reactive({
  id: null,
  parentId: 0,
  deptName: '',
  deptLevel: 1,
  sortOrder: 0,
  status: 1,
  loginUsername: ''
})

const deptRules = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

onMounted(async () => {
  await loadDeptTree()
})

async function loadDeptTree() {
  try {
    const res = await getDeptTree()
    // 构建树形结构
    deptTree.value = buildTree(res.data || [], 0)
  } catch (error) {
    console.error('获取部门树失败:', error)
  }
}

function buildTree(list, parentId) {
  const tree = []
  for (const item of list) {
    if (item.parentId === parentId) {
      const children = buildTree(list, item.id)
      if (children.length > 0) {
        item.children = children
      }
      tree.push(item)
    }
  }
  return tree
}

function handleAdd(parent) {
  isEdit.value = false
  dialogTitle.value = '新增部门'
  deptForm.id = null
  deptForm.parentId = parent ? parent.id : 0
  deptForm.deptName = ''
  deptForm.deptLevel = parent ? (parent.deptLevel || 1) + 1 : 1
  deptForm.sortOrder = 0
  deptForm.status = 1
  dialogVisible.value = true
}

async function handleEdit(data) {
  isEdit.value = true
  dialogTitle.value = '编辑部门'
  try {
    const res = await getDeptDetail(data.id)
    Object.assign(deptForm, res.data)
    dialogVisible.value = true
  } catch (error) {
    console.error('获取部门详情失败:', error)
  }
}

async function submitForm() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (isEdit.value) {
      await updateDept(deptForm)
      ElMessage.success('更新成功')
    } else {
      const res = await createDept(deptForm)
      const created = res.data
      if (created?.deptLevel >= 1 && created?.loginUsername) {
        ElMessage.success(`创建成功，部门登录账号：${created.loginUsername}，初始密码 admin123`)
      } else {
        ElMessage.success('创建成功')
      }
    }
    dialogVisible.value = false
    await loadDeptTree()
  } catch (error) {
    console.error('保存部门失败:', error)
  }
}

async function handleEnsureLogin(data) {
  try {
    const res = await ensureDeptLogin(data.id)
    const loginUsername = res.data?.loginUsername
    ElMessage.success(
      loginUsername
        ? `已创建部门登录账号：${loginUsername}，初始密码 admin123`
        : '部门登录账号已就绪'
    )
    await loadDeptTree()
  } catch (error) {
    const msg = error?.message || error.response?.data?.message || '创建失败'
    ElMessage.error(msg)
  }
}

async function handleResetDeptPwd(data) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新密码（留空则重置为 admin123）', '重置部门登录密码', {
      inputType: 'password',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    await resetDeptLoginPassword(data.id, value || undefined)
    ElMessage.success('密码已重置')
  } catch (error) {
    if (error !== 'cancel') {
      const msg = error?.message || error.response?.data?.message || '重置失败'
      ElMessage.error(msg)
    }
  }
}

async function handleDelete(data) {
  try {
    await ElMessageBox.confirm('确定要删除该部门吗？', '提示', { type: 'warning' })
    await deleteDept(data.id)
    ElMessage.success('删除成功')
    await loadDeptTree()
  } catch (error) {
    if (error !== 'cancel') {
      const msg = error?.message || error.response?.data?.message || '删除失败'
      ElMessage.error(msg)
    }
  }
}
</script>

<style lang="scss" scoped>
.dept-manage {
  .tree-card {
    .header-with-action { display: flex; justify-content: space-between; align-items: center; }
    .tree-node {
      flex: 1;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 14px;
      padding-right: 8px;
      .tree-actions { margin-left: 10px; }
      .login-tag { color: #909399; font-size: 12px; margin-left: 4px; }
    }
  }
}
</style>