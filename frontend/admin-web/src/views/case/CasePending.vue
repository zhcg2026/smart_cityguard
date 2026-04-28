<template>
  <div class="case-pending">
    <!-- 快速筛选 -->
    <el-card class="filter-card">
      <el-radio-group v-model="currentTab" @change="handleTabChange">
        <el-radio-button label="pending_verify">待核查</el-radio-button>
        <el-radio-button label="pending_register">待立案</el-radio-button>
        <el-radio-button label="pending_dispatch">待派遣</el-radio-button>
        <el-radio-button label="pending_handle">待处置</el-radio-button>
        <el-radio-button label="pending_check">待核实</el-radio-button>
        <el-radio-button label="rejected">不受理</el-radio-button>
      </el-radio-group>
    </el-card>

    <!-- 案件列表 -->
    <el-card class="list-card">
      <el-table
        v-loading="loading"
        :data="caseList"
        style="width: 100%"
      >
        <el-table-column prop="caseNo" label="案件编号" width="150" />
        <el-table-column prop="categoryBigName" label="大类" width="100" />
        <el-table-column prop="categorySmallName" label="小类" width="120" />
        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="reportTime" label="上报时间" width="180" />
        <el-table-column prop="collectorName" label="上报人" width="100" />
        <el-table-column prop="timeRemaining" label="剩余时限" width="100">
          <template #default="{ row }">
            <span :class="{ 'overdue': row.isOverdue }">
              {{ row.timeRemaining || '--' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="goDetail(row.id)">
              查看
            </el-button>
            <el-button
              v-if="canProcess(row.status)"
              type="success"
              size="small"
              @click="handleProcess(row)"
            >
              处理
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPendingCaseList } from '@/api/case'

const router = useRouter()

const loading = ref(false)
const caseList = ref([])
const currentTab = ref('pending_verify')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusMap = {
  'pending_verify': 'PENDING_VERIFY',
  'pending_register': 'PENDING_REGISTER',
  'pending_dispatch': 'PENDING_DISPATCH',
  'pending_handle': 'PENDING_HANDLE',
  'pending_check': 'PENDING_CHECK',
  'rejected': 'REJECTED'
}

onMounted(async () => {
  await loadCaseList()
})

async function loadCaseList() {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: statusMap[currentTab.value]
    }
    const res = await getPendingCaseList(params)
    caseList.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取待处理案件失败:', error)
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
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

function goDetail(id) {
  router.push(`/case/detail/${id}`)
}

function canProcess(status) {
  return ['PENDING_REGISTER', 'PENDING_DISPATCH', 'PENDING_CHECK'].includes(status)
}

function handleProcess(row) {
  // 根据状态跳转到不同的处理页面
  router.push(`/case/detail/${row.id}?action=process`)
}
</script>

<style lang="scss" scoped>
.case-pending {
  .filter-card {
    margin-bottom: 20px;
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