<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <span>今日提示</span>
      </template>
      <el-table v-loading="loading" :data="list" stripe @row-click="openDetail">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="content" label="内容摘要" min-width="260" show-overflow-tooltip />
        <el-table-column prop="publisherName" label="发布人" width="100" />
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.publishTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click.stop="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无提示" />
    </el-card>

    <ContentDetailDialog
      v-model:visible="detailVisible"
      type="dailytip"
      :item="detailItem"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDailyTipList, getDailyTipDetail } from '@/api/config'
import { formatDateTime } from '@/utils/dateFormat'
import ContentDetailDialog from '@/components/ContentDetailDialog.vue'

const loading = ref(false)
const list = ref([])
const detailVisible = ref(false)
const detailItem = ref(null)

async function loadList() {
  loading.value = true
  try {
    const res = await getDailyTipList({ limit: 100 })
    list.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function openDetail(row) {
  if (!row?.id) return
  try {
    const res = await getDailyTipDetail(row.id)
    detailItem.value = res.data || row
    detailVisible.value = true
  } catch {
    detailItem.value = row
    detailVisible.value = true
  }
}

onMounted(loadList)
</script>

<style lang="scss" scoped>
:deep(.el-table__row) {
  cursor: pointer;
}
</style>
