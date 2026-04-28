<template>
  <div class="case-list">
    <!-- 搜索表单 -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="案件编号">
          <el-input v-model="searchForm.caseNo" placeholder="请输入案件编号" clearable />
        </el-form-item>
        <el-form-item label="案件状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="待核查" value="PENDING_VERIFY" />
            <el-option label="待立案" value="PENDING_REGISTER" />
            <el-option label="待派遣" value="PENDING_DISPATCH" />
            <el-option label="待处置" value="PENDING_HANDLE" />
            <el-option label="待核实" value="PENDING_CHECK" />
            <el-option label="已结案" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="案件大类">
          <el-select v-model="searchForm.categoryBig" placeholder="请选择大类" clearable>
            <el-option v-for="item in categoryBigList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="上报时间">
          <el-date-picker
            v-model="searchForm.reportTime"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 案件列表 -->
    <el-card class="list-card">
      <el-table
        v-loading="loading"
        :data="caseList"
        style="width: 100%"
        @row-click="handleRowClick"
      >
        <el-table-column prop="caseNo" label="案件编号" width="150" />
        <el-table-column prop="categoryBigName" label="大类" width="100" />
        <el-table-column prop="categorySmallName" label="小类" width="120" />
        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reportTime" label="上报时间" width="180" />
        <el-table-column prop="collectorName" label="上报人" width="100" />
        <el-table-column prop="timeRemaining" label="剩余时限" width="100">
          <template #default="{ row }">
            <span :class="{ 'overdue': row.isOverdue }">
              {{ row.timeRemaining || '--' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="goDetail(row.id)">
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCaseList, getCategoryBigList } from '@/api/case'
import { getCategoryBigList as getCategoryList } from '@/api/config'

const router = useRouter()

const loading = ref(false)
const caseList = ref([])
const categoryBigList = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  caseNo: '',
  status: '',
  categoryBig: '',
  reportTime: []
})

onMounted(async () => {
  await loadCategoryBigList()
  await loadCaseList()
})

async function loadCategoryBigList() {
  try {
    const res = await getCategoryList()
    categoryBigList.value = res.data || []
  } catch (error) {
    console.error('获取大类列表失败:', error)
  }
}

async function loadCaseList() {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      ...searchForm,
      startTime: searchForm.reportTime?.[0],
      endTime: searchForm.reportTime?.[1]
    }
    const res = await getCaseList(params)
    caseList.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取案件列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadCaseList()
}

function handleReset() {
  searchForm.caseNo = ''
  searchForm.status = ''
  searchForm.categoryBig = ''
  searchForm.reportTime = []
  pageNum.value = 1
  loadCaseList()
}

function handleSizeChange(size) {
  pageSize.value = size
  loadCaseList()
}

function handleCurrentChange(page) {
  pageNum.value = page
  loadCaseList()
}

function handleRowClick(row) {
  goDetail(row.id)
}

function goDetail(id) {
  router.push(`/case/detail/${id}`)
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
.case-list {
  .search-card {
    margin-bottom: 20px;

    .el-form-item {
      margin-bottom: 0;
    }
  }

  .list-card {
    .el-pagination {
      margin-top: 20px;
      justify-content: flex-end;
    }

    .overdue {
      color: #f56c6c;
    }
  }
}
</style>