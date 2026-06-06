<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="header-row">
          <span>我的消息</span>
          <el-button type="primary" link @click="markAllRead">全部已读</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="messages" stripe>
        <el-table-column label="状态" width="72">
          <template #default="{ row }">
            <el-tag v-if="row.msgStatus === 'unread'" type="danger" size="small">未读</el-tag>
            <el-tag v-else type="info" size="small">已读</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="msgTitle" label="标题" min-width="140" />
        <el-table-column prop="msgContent" label="内容" min-width="220" show-overflow-tooltip />
        <el-table-column prop="msgTime" label="时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.msgTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openMessage(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && messages.length === 0" description="暂无消息" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMessageList, markMessageRead, markAllMessagesRead } from '@/api/message'
import { formatDateTime } from '@/utils/dateFormat'

const router = useRouter()
const loading = ref(false)
const messages = ref([])

async function loadMessages() {
  loading.value = true
  try {
    const res = await getMessageList()
    messages.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function resolveRoute(msg) {
  if (!msg?.bizId) return null
  if (msg.bizType === 'case') {
    return `/case/detail/${msg.bizId}`
  }
  return null
}

async function openMessage(row) {
  if (row.msgStatus === 'unread' && row.id) {
    try {
      await markMessageRead(row.id)
      row.msgStatus = 'read'
    } catch {
      /* ignore */
    }
  }
  const path = resolveRoute(row)
  if (path) {
    router.push(path)
  } else {
    ElMessage.info(row.msgContent || row.msgTitle)
  }
}

async function markAllRead() {
  try {
    await markAllMessagesRead()
    ElMessage.success('已全部标记为已读')
    await loadMessages()
  } catch (e) {
    console.error(e)
  }
}

onMounted(loadMessages)
</script>

<style scoped lang="scss">
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
