import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElNotification } from 'element-plus'
import { getUnreadMessageCount, getUnreadMessages } from '@/api/message'
import { playNotifySound } from '@/utils/notifySound'

const POLL_MS = 25000
const SEEN_KEY = 'cityguard_seen_msg_ids'

function loadSeenIds() {
  try {
    const raw = sessionStorage.getItem(SEEN_KEY)
    return raw ? new Set(JSON.parse(raw)) : new Set()
  } catch {
    return new Set()
  }
}

function saveSeenIds(set) {
  const arr = [...set].slice(-200)
  sessionStorage.setItem(SEEN_KEY, JSON.stringify(arr))
}

function resolveMessageRoute(msg) {
  if (!msg?.bizId) return null
  const t = msg.bizType
  if (t === 'case' || t === 'check_task' || t === 'verify_task') {
    return { path: `/case/detail/${msg.bizId}` }
  }
  return null
}

export function useMessagePoll() {
  const router = useRouter()
  const unreadCount = ref(0)
  let timer = null
  let seenIds = loadSeenIds()
  let polling = false

  async function pollOnce() {
    if (polling) return
    polling = true
    try {
      const countRes = await getUnreadMessageCount()
      unreadCount.value = countRes.data ?? 0

      const listRes = await getUnreadMessages()
      const list = listRes.data || []
      let hasNew = false
      for (const msg of list) {
        if (!msg?.id || seenIds.has(msg.id)) continue
        seenIds.add(msg.id)
        hasNew = true
        const routeTo = resolveMessageRoute(msg)
        ElNotification({
          title: msg.msgTitle || '新消息',
          message: msg.msgContent || '',
          type: 'info',
          duration: 8000,
          onClick: () => {
            if (routeTo) router.push(routeTo)
          }
        })
      }
      if (hasNew) {
        playNotifySound()
        saveSeenIds(seenIds)
        window.dispatchEvent(new CustomEvent('cityguard:refresh-lists'))
      }
    } catch (e) {
      console.warn('消息轮询失败', e)
    } finally {
      polling = false
    }
  }

  function startPoll() {
    stopPoll()
    pollOnce()
    timer = setInterval(pollOnce, POLL_MS)
  }

  function stopPoll() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  onMounted(startPoll)
  onBeforeUnmount(stopPoll)

  return { unreadCount, pollOnce, startPoll, stopPoll }
}
