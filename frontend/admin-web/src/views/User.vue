<template>
  <div class="user">
    <el-card>
      <template #header>用户管理</template>

      <el-form :inline="true" class="search-form">
        <el-form-item label="角色">
          <el-select v-model="searchRole" placeholder="全部">
            <el-option label="采集员" value="collector" />
            <el-option label="受理员" value="receiver" />
            <el-option label="派遣员" value="dispatcher" />
            <el-option label="处置部门" value="handler" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary">查询</el-button>
          <el-button type="success">新增用户</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" border>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="role" label="角色" width="100" />
        <el-table-column prop="department" label="所属部门" />
        <el-table-column prop="phone" label="联系电话" width="150" />
        <el-table-column prop="status" label="状态" width="80" />
        <el-table-column label="操作" width="150">
          <el-button type="primary" size="small">编辑</el-button>
          <el-button type="danger" size="small">删除</el-button>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const searchRole = ref('')

const tableData = ref([
  { username: 'collector01', name: '张三', role: '采集员', department: '指挥中心', phone: '13800138001', status: '正常' },
  { username: 'receiver01', name: '李四', role: '受理员', department: '受理中心', phone: '13800138002', status: '正常' }
])
</script>

<style scoped>
.user {
  padding: 20px;
}
.search-form {
  margin-bottom: 20px;
}
</style>