<template>
  <div class="message-page">
    <van-nav-bar title="消息通知" left-arrow @click-left="goBack">
      <template #right>
        <van-button v-if="unreadCount > 0" size="small" type="primary" plain @click="readAll">全部已读</van-button>
      </template>
    </van-nav-bar>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list v-model:loading="loading" :finished="finished" :finished-text="messageList.length > 0 ? '没有更多了' : ''" @load="loadMessages">
        <van-cell v-for="item in messageList" :key="item.id" :title="item.msgTitle" :label="item.msgContent" :value="formatTime(item.msgTime || item.createTime)" is-link @click="viewMessage(item)">
          <template v-if="item.msgStatus !== 'read'" #icon>
            <div class="unread-dot" />
          </template>
        </van-cell>
      </van-list>
    </van-pull-refresh>

    <van-empty v-if="!loading && messageList.length === 0" description="暂无消息" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserMessages, markMessageRead, markAllMessagesRead, getUnreadMessageCount } from '@/api/message'
import { showSuccessToast } from 'vant'

const router = useRouter()
const messageList = ref([])
const loading = ref(false)
const finished = ref(true)
const refreshing = ref(false)
const unreadCount = ref(0)

function goBack() {
  router.back()
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return `${date.getMonth() + 1}-${date.getDate()}`
}

function onRefresh() {
  loadMessages()
}

async function loadMessages() {
  loading.value = true
  try {
    const res = await getUserMessages()
    messageList.value = res.data || []
    finished.value = true
    await updateUnreadCount()
  } catch (e) {
    console.error('加载消息失败', e)
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

async function updateUnreadCount() {
  try {
    const res = await getUnreadMessageCount()
    unreadCount.value = res.data || 0
  } catch (e) {
    console.error('获取未读数失败', e)
  }
}

async function viewMessage(item) {
  if (item.msgStatus !== 'read' && item.id) {
    try {
      await markMessageRead(item.id)
      item.msgStatus = 'read'
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (e) {
      console.error('标记已读失败', e)
    }
  }
  if (item.bizId) {
    const bizType = item.bizType || ''
    if (bizType === 'check_task') {
      router.push(`/task/check/${item.bizId}`)
    } else if (bizType === 'verify_task') {
      router.push(`/task/verify/${item.bizId}`)
    }
  }
}

async function readAll() {
  try {
    await markAllMessagesRead()
    messageList.value.forEach(item => { item.msgStatus = 'read' })
    unreadCount.value = 0
    showSuccessToast('已全部标记已读')
  } catch (e) {
    console.error('全部已读失败', e)
  }
}

onMounted(() => {
  loadMessages()
})
</script>

<style scoped>
.message-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #1989fa;
  margin-right: 8px;
  flex-shrink: 0;
}
</style>
