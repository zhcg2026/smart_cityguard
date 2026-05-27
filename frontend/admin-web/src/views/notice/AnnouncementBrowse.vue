<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <span>公文通告</span>
      </template>
      <el-table v-loading="loading" :data="list" stripe @row-click="openDetail">
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="tagType(row.announcementType)" size="small">
              {{ typeLabel(row.announcementType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="docNumber" label="文号" width="140" show-overflow-tooltip />
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
      <el-empty v-if="!loading && list.length === 0" description="暂无通告" />
    </el-card>

    <ContentDetailDialog
      v-model:visible="detailVisible"
      type="announcement"
      :item="detailItem"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAnnouncementList, getAnnouncementDetail } from '@/api/config'
import { formatDateTime } from '@/utils/dateFormat'
import ContentDetailDialog from '@/components/ContentDetailDialog.vue'

const loading = ref(false)
const list = ref([])
const detailVisible = ref(false)
const detailItem = ref(null)

function typeLabel(t) {
  return ({ system: '系统', business: '普通', urgent: '紧急' }[t] || '通告')
}

function tagType(t) {
  if (t === 'urgent') return 'danger'
  if (t === 'system') return 'warning'
  return 'info'
}

async function loadList() {
  loading.value = true
  try {
    const res = await getAnnouncementList({ limit: 100 })
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
    const res = await getAnnouncementDetail(row.id)
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
