<template>
  <div class="filing">
    <el-card>
      <template #header>立案栏 - 信息完整待批转</template>
      <el-table :data="tableData">
        <el-table-column prop="id" label="编号" width="100" />
        <el-table-column prop="category" label="类型" width="150" />
        <el-table-column prop="description" label="问题描述" />
        <el-table-column prop="location" label="位置" width="200" />
        <el-table-column prop="deadline" label="处置时限" width="120" />
        <el-table-column label="操作" width="200">
          <el-button type="success" size="small">批转</el-button>
          <el-button type="danger" size="small">作废</el-button>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const tableData = ref([
  { id: '001', category: '部件-公用设施-井盖', description: '井盖缺失', location: 'XX路XX号', deadline: '2紧急工作时' }
])
</script>

<style scoped>
.filing {
  padding: 20px;
}
</style>