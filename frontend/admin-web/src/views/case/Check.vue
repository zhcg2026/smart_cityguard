<template>
  <div class="check">
    <el-card>
      <template #header>核查栏 - 已处置待核查</template>
      <el-table :data="tableData">
        <el-table-column prop="id" label="编号" width="100" />
        <el-table-column prop="category" label="类型" width="150" />
        <el-table-column prop="description" label="问题描述" />
        <el-table-column prop="status" label="处置状态" width="100" />
        <el-table-column prop="collector" label="核查采集员" width="120" />
        <el-table-column label="操作" width="150">
          <el-button type="primary" size="small">查看</el-button>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const tableData = ref([
  { id: '001', category: '部件-路灯', description: '路灯不亮', status: '已处置', collector: '张三' }
])
</script>

<style scoped>
.check {
  padding: 20px;
}
</style>