<template>
  <div class="password-page">
    <van-nav-bar title="修改密码" left-arrow @click-left="goBack" />

    <van-cell-group inset class="form-group">
      <van-field v-model="form.oldPassword" type="password" label="原密码" placeholder="请输入原密码" />
      <van-field v-model="form.newPassword" type="password" label="新密码" placeholder="请输入新密码（至少6位）" />
      <van-field v-model="form.confirmPassword" type="password" label="确认密码" placeholder="请再次输入新密码" />
    </van-cell-group>

    <div class="btn-wrap">
      <van-button round block type="primary" :loading="submitting" @click="handleSubmit">
        确认修改
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { changePassword } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()
const submitting = ref(false)

const form = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

function goBack() {
  router.back()
}

async function handleSubmit() {
  if (!form.value.oldPassword) {
    showToast('请输入原密码')
    return
  }
  if (!form.value.newPassword || form.value.newPassword.length < 6) {
    showToast('新密码长度不能少于6位')
    return
  }
  if (form.value.newPassword !== form.value.confirmPassword) {
    showToast('两次输入的密码不一致')
    return
  }
  submitting.value = true
  try {
    await changePassword({
      oldPassword: form.value.oldPassword,
      newPassword: form.value.newPassword
    })
    showToast('密码修改成功，请重新登录')
    await userStore.logout()
    router.push('/login')
  } catch {
    // error already handled by interceptor
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.password-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.form-group {
  margin-top: 12px;
}

.btn-wrap {
  padding: 20px 16px;
}
</style>
