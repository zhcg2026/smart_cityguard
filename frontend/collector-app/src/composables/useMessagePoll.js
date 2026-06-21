import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { showNotify } from 'vant'
import { getToken } from '@/utils/auth'
import { getUnreadMessages } from '@/api/message'
import { playNotifySound } from '@/utils/notifySound'

const POLL_MS = 25000
const SEEN_KEY = 'cityguard_collector_seen_msg_ids'

function loadSeenIds() {
  try {
    const raw = sessionStorage.getItem(SEEN_KEY)
    return raw ? new Set(JSON.parse(raw)) : new Set()
  } catch {
    return new Set()
  }
}

function saveSeenIds(set) {
  sessionStorage.setItem(SEEN_KEY, JSON.stringify([...set].slice(-200)))
}

export function useMessagePoll() {
  const router = useRouter()
  const unreadCount = ref(0)
  let timer = null
  let seenIds = loadSeenIds()
  let polling = false

  async function pollOnce() {
    if (!getToken() || polling) return
    polling = true
    try {
      const listRes = await getUnreadMessages()
      const list = listRes.data || []
      unreadCount.value = list.length
      let hasNew = false
      for (const msg of list) {
        if (!msg?.id || seenIds.has(msg.id)) continue
        seenIds.add(msg.id)
        hasNew = true
        showNotify({
          type: 'primary',
          background: '#1989fa',
          color: '#fff',
          className: 'collector-push-notify',
          message: `${msg.msgTitle || '新消息'}\n${msg.msgContent || ''}`,
          duration: 5000,
          onClick: () => {
            if (msg.bizType === 'check_task' && msg.bizId) {
              router.push(`/task/check/${msg.bizId}`)
            } else if (msg.bizType === 'verify_task' && msg.bizId) {
              router.push(`/task/verify/${msg.bizId}`)
            } else if (msg.bizType === 'case') {
              router.push('/task')
            }
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
    if (!getToken()) return
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
