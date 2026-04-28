<template>
  <div class="check-task">
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="任务状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="待执行" value="PENDING" />
            <el-option label="已完成" value="COMPLETED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="list-card">
      <el-table v-loading="loading" :data="taskList" style="width: 100%">
        <el-table-column prop="taskNo" label="任务编号" width="150" />
        <el-table-column prop="caseNo" label="关联案件" width="150" />
        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="collectorName" label="核实人" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column prop="deadline" label="截止时间" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PENDING' ? 'warning' : 'success'">
              {{ row.status === 'PENDING' ? '待执行' : '已完成' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="goDetail(row.id)">查看</el-button>
            <el-button v-if="row.status === 'PENDING'" type="success" size="small" link @click="handleExecute(row)">
              执行
            </el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCheckTaskList } from '@/api/task'

const router = useRouter()
const loading = ref(false)
const taskList = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({ status: '' })

onMounted(async () => {
  await loadTaskList()
})

async function loadTaskList() {
  loading.value = true
  try {
    const res = await getCheckTaskList({ pageNum: pageNum.value, pageSize: pageSize.value, ...searchForm })
    taskList.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadTaskList()
}

function handleReset() {
  searchForm.status = ''
  pageNum.value = 1
  loadTaskList()
}

function handleSizeChange(size) {
  pageSize.value = size
  loadTaskList()
}

function handleCurrentChange(page) {
  pageNum.value = page
  loadTaskList()
}

function goDetail(id) {
  router.push(`/task/check/${id}`)
}

function handleExecute(row) {
  router.push(`/task/check/${id}?action=execute`)
}
</script>

<style lang="scss" scoped>
.check-task {
  .search-card { margin-bottom: 20px; }
  .list-card .el-pagination { margin-top: 20px; justify-content: flex-end; }
}
</style>