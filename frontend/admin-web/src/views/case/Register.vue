<template>
  <div class="register">
    <el-card>
      <template #header>案件登记</template>

      <el-form :model="form" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="问题描述">
              <el-input v-model="form.description" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="位置">
              <el-input v-model="form.location" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="问题来源">
              <el-select v-model="form.source" placeholder="请选择">
                <el-option label="12345热线" value="12345" />
                <el-option label="市民投诉" value="citizen" />
                <el-option label="领导交办" value="leader" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="问题等级">
              <el-select v-model="form.level" placeholder="请选择">
                <el-option label="日常" value="daily" />
                <el-option label="一般" value="normal" />
                <el-option label="严重" value="serious" />
                <el-option label="重大" value="major" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="问题类型">
              <el-select v-model="form.type" placeholder="请选择">
                <el-option label="部件" value="component" />
                <el-option label="事件" value="event" />
                <el-option label="服务事项" value="service" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="大类">
              <el-select v-model="form.category" placeholder="请选择">
                <el-option label="公用设施" value="01" />
                <el-option label="道路交通设施" value="02" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="小类">
              <el-select v-model="form.subcategory" placeholder="请选择">
                <el-option label="上水井盖" value="01" />
                <el-option label="路灯" value="37" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="附件">
          <el-upload action="#" list-type="picture-card" :auto-upload="false">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <el-button type="primary">保存草稿</el-button>
          <el-button type="success">批转</el-button>
          <el-button>发核实</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="list-card">
      <template #header>待登记列表</template>
      <el-table :data="tableData">
        <el-table-column prop="id" label="编号" width="100" />
        <el-table-column prop="source" label="来源" width="100" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="time" label="时间" width="180" />
        <el-table-column label="操作" width="150">
          <el-button type="primary" size="small">登记</el-button>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const form = ref({
  description: '',
  location: '',
  source: '',
  level: '',
  type: '',
  category: '',
  subcategory: ''
})

const tableData = ref([
  { id: '001', source: '12345热线', description: 'XX路井盖缺失', time: '2024-04-27 10:30' }
])
</script>

<style scoped>
.register {
  padding: 20px;
}
.list-card {
  margin-top: 20px;
}
</style>