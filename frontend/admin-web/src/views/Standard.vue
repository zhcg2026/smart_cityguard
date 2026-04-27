<template>
  <div class="standard">
    <el-card>
      <template #header>立结案标准管理</template>

      <el-form :inline="true" class="search-form">
        <el-form-item label="类型">
          <el-select v-model="searchType" placeholder="全部">
            <el-option label="部件类" value="component" />
            <el-option label="事件类" value="event" />
            <el-option label="服务事项" value="service" />
          </el-select>
        </el-form-item>
        <el-form-item label="大类">
          <el-select v-model="searchCategory" placeholder="全部" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" border>
        <el-table-column prop="categoryCode" label="大类代码" width="80" />
        <el-table-column prop="categoryName" label="大类名称" width="120" />
        <el-table-column prop="subCode" label="小类代码" width="80" />
        <el-table-column prop="subName" label="小类名称" width="120" />
        <el-table-column prop="condition" label="立案条件" />
        <el-table-column prop="deadline" label="处置时限" width="120" />
        <el-table-column prop="closeCondition" label="结案条件" width="100" />
      </el-table>

      <el-pagination layout="total, prev, pager, next" :total="202" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const searchType = ref('')
const searchCategory = ref('')

const tableData = ref([
  { categoryCode: '01', categoryName: '公用设施', subCode: '01', subName: '上水井盖', condition: '缺失、移位、开裂，存在安全隐患', deadline: '2紧急工作时', closeCondition: '恢复' },
  { categoryCode: '01', categoryName: '公用设施', subCode: '37', subName: '路灯', condition: '倾斜、倒地、折断', deadline: '1工作日', closeCondition: '更换、修复或拆除' }
])
</script>

<style scoped>
.standard {
  padding: 20px;
}
.search-form {
  margin-bottom: 20px;
}
.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>