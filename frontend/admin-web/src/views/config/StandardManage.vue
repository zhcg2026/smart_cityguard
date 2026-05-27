<template>
  <div class="page-container">
    <el-card>
      <template #header><span>立结案标准</span></template>

      <el-alert
        type="warning"
        show-icon
        :closable="false"
        class="mb-4"
        title="导入将按「部件」「事件」两个 Sheet，物理删除库中对应 category_type 下的大类、小类及立案标准后重建。请仅在测试库或已确认无业务依赖时使用。"
      />

      <div v-if="isAdmin" class="import-block">
        <div class="import-title">上传模板（muban.xlsx）</div>
        <el-upload
          :show-file-list="false"
          accept=".xlsx"
          :http-request="handleImport"
          :disabled="importing"
        >
          <el-button type="primary" :loading="importing">选择并导入</el-button>
        </el-upload>
        <p class="hint">表头与 Sheet 名须与规范一致；仅管理员账号可导入。</p>
      </div>
      <el-alert v-else type="info" show-icon :closable="false" title="仅管理员可导入立结案标准模板。" />

      <el-card v-if="lastResult" class="result-card" shadow="never">
        <template #header>最近一次导入结果</template>
        <el-table :data="lastResultSheets" border size="small">
          <el-table-column prop="sheet" label="工作表" width="120" />
          <el-table-column prop="categoryType" label="类型" width="120" />
          <el-table-column prop="bigCount" label="大类数" width="90" />
          <el-table-column prop="smallCount" label="小类数" width="90" />
          <el-table-column prop="standardCount" label="标准条数" width="110" />
        </el-table>
      </el-card>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { RoleCode } from '@/utils/roleAccess'
import { importMubanStandard } from '@/api/config'

const userStore = useUserStore()
const isAdmin = computed(() => (userStore.roles || []).includes(RoleCode.ADMIN))

const importing = ref(false)
const lastResult = ref(null)

const lastResultSheets = computed(() => lastResult.value?.sheets || [])

async function handleImport({ file }) {
  if (!file) return
  importing.value = true
  try {
    const res = await importMubanStandard(file)
    lastResult.value = res.data || null
    ElMessage.success('导入成功')
  } catch (e) {
    console.error(e)
  } finally {
    importing.value = false
  }
}
</script>

<style scoped>
.page-container {
  padding: 16px;
}
.mb-4 {
  margin-bottom: 16px;
}
.import-block {
  margin-bottom: 20px;
}
.import-title {
  font-weight: 600;
  margin-bottom: 8px;
}
.hint {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.result-card {
  margin-top: 16px;
}
</style>
