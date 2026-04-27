<template>
  <div class="handled">
    <el-card>
      <template #header>经办案件 - 已处理历史</template>
      <el-table :data="tableData">
        <el-table-column prop="id" label="编号" width="100" />
        <el-table-column prop="category" label="类型" width="150" />
        <el-table-column prop="description" label="问题描述" />
        <el-table-column prop="result" label="处理结果" width="100" />
        <el-table-column prop="time" label="完成时间" width="180" />
        <el-table-column label="操作" width="100">
          <el-button type="primary" size="small">详情</el-button>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const tableData = ref([
  { id: '001', category: '部件-井盖', description: '井盖缺失', result: '已恢复', time: '2024-04-26 15:00' }
])
</script>

<style scoped>
.handled {
  padding: 20px;
}
</style>