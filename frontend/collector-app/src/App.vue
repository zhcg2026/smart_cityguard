<template>
  <router-view />
</template>

<script setup>
import { watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getToken } from '@/utils/auth'
import { useMessagePoll } from '@/composables/useMessagePoll'

const userStore = useUserStore()
userStore.initUser()

const route = useRoute()
const { startPoll, stopPoll } = useMessagePoll()

watch(
  () => [route.path, getToken()],
  () => {
    if (getToken() && route.path !== '/login') {
      startPoll()
    } else {
      stopPoll()
    }
  },
  { immediate: true }
)
</script>

<style>
body {
  margin: 0;
  padding: 0;
  background-color: #f7f8fa;
}
#app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* 上传失败 Toast：避免默认白底看不清 */
.upload-fail-toast,
.upload-success-toast {
  max-width: 92vw !important;
  padding: 12px 16px !important;
  line-height: 1.45 !important;
  word-break: break-word !important;
}
.upload-fail-toast {
  background: rgba(50, 50, 50, 0.94) !important;
  color: #fff !important;
}
.upload-success-toast {
  background: rgba(7, 193, 96, 0.94) !important;
  color: #fff !important;
}
</style>

<style>
.upload-error-dialog .van-dialog__message {
  text-align: left;
  white-space: pre-wrap;
  line-height: 1.5;
  max-height: 60vh;
  overflow-y: auto;
}
</style>