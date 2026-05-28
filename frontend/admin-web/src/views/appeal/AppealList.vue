<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="header-row">
          <span>处置超时申诉</span>
          <el-form :inline="true" class="filter-form" @submit.prevent="loadList">
            <el-form-item v-if="showTabFilter" label="状态">
              <el-radio-group v-model="tab" @change="onTabChange">
                <el-radio-button label="pending">待办</el-radio-button>
                <el-radio-button label="done">已办</el-radio-button>
                <el-radio-button label="all">全部</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="案件编号">
              <el-input v-model="caseCode" clearable placeholder="模糊查询" style="width: 160px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadList">查询</el-button>
            </el-form-item>
          </el-form>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="appealCode" label="申诉编号" width="170" />
        <el-table-column prop="caseCode" label="案件编号" width="150">
          <template #default="{ row }">
            <el-link type="primary" @click="goCase(row.caseId)">{{ row.caseCode }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="applyDeptName" label="申请部门" min-width="120" show-overflow-tooltip />
        <el-table-column prop="applyUserName" label="申请人" width="100" />
        <el-table-column prop="appealStatus" label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="getAppealStatusTagType(row.appealStatus)" size="small">
              {{ formatAppealStatusLabel(row.appealStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applyTime" label="申请时间" width="170" />
        <el-table-column prop="appealDesc" label="申诉说明" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="goDetail(row.id)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTimeoutAppealList } from '@/api/appeal'
import { useUserStore } from '@/stores/user'
import { RoleCode } from '@/utils/roleAccess'
import { formatAppealStatusLabel, getAppealStatusTagType } from '@/utils/appealStatus'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const caseCode = ref('')
const tab = ref('pending')

const showTabFilter = computed(() => {
  const roles = userStore.roles || []
  return (
    roles.includes(RoleCode.DISPATCHER) ||
    roles.includes(RoleCode.ACCEPTOR) ||
    roles.includes(RoleCode.ADMIN) ||
    roles.includes(RoleCode.SUPERVISOR)
  )
})

async function loadList() {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      caseCode: caseCode.value || undefined
    }
    if (showTabFilter.value) {
      params.tab = tab.value
    }
    const res = await getTimeoutAppealList(params)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  pageNum.value = 1
  loadList()
}

function goDetail(id) {
  router.push({ name: 'AppealDetail', params: { id } })
}

function goCase(caseId) {
  router.push({ path: `/case/detail/${caseId}` })
}

onMounted(loadList)
</script>

<style scoped>
.header-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.filter-form {
  margin: 0;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
