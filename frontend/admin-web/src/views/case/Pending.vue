<template>
  <div class="pending">
    <el-card>
      <template #header>待查栏 - 无照片/视频的案件</template>
      <el-table :data="tableData">
        <el-table-column prop="id" label="编号" width="100" />
        <el-table-column prop="source" label="来源" width="100" />
        <el-table-column prop="description" label="问题描述" />
        <el-table-column prop="location" label="位置" width="200" />
        <el-table-column prop="time" label="登记时间" width="180" />
        <el-table-column label="操作" width="200">
          <el-button type="primary" size="small">发核实</el-button>
          <el-button type="danger" size="small">作废</el-button>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const tableData = ref([
  { id: '001', source: '12345热线', description: 'XX路路灯不亮', location: 'XX路XX号', time: '2024-04-27 09:00' }
])
</script>

<style scoped>
.pending {
  padding: 20px;
}
</style>