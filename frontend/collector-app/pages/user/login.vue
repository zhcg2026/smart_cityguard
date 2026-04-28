<template>
  <view class="login-page">
    <view class="login-header">
      <image src="/static/images/logo.png" class="logo" mode="aspectFit" />
      <view class="title">智慧城管</view>
      <view class="subtitle">采集员移动端</view>
    </view>

    <view class="login-form">
      <view class="form-item">
        <view class="form-icon">
          <text class="iconfont">&#xe612;</text>
        </view>
        <input
          v-model="formData.username"
          type="text"
          placeholder="请输入用户名"
          class="form-input"
        />
      </view>

      <view class="form-item">
        <view class="form-icon">
          <text class="iconfont">&#xe613;</text>
        </view>
        <input
          v-model="formData.password"
          type="password"
          placeholder="请输入密码"
          class="form-input"
        />
      </view>

      <view class="form-item" v-if="showCaptcha">
        <view class="form-icon">
          <text class="iconfont">&#xe614;</text>
        </view>
        <input
          v-model="formData.captcha"
          type="text"
          placeholder="请输入验证码"
          class="form-input captcha-input"
        />
        <image :src="captchaUrl" class="captcha-img" @click="refreshCaptcha" />
      </view>

      <view class="form-checkbox">
        <checkbox :checked="formData.rememberMe" @click="formData.rememberMe = !formData.rememberMe" />
        <text>记住密码</text>
      </view>

      <button class="btn-login" :loading="loading" @click="handleLogin">
        登 录
      </button>
    </view>

    <view class="login-footer">
      <text>© 2024 运城市城市综合管理服务中心</text>
    </view>
  </view>
</template>

<script>
import { login, getUserInfo } from '../../utils/api'

export default {
  data() {
    return {
      formData: {
        username: '',
        password: '',
        captcha: '',
        rememberMe: false
      },
      showCaptcha: false,
      captchaUrl: '',
      loading: false
    }
  },
  onLoad() {
    // 检查是否已登录
    const token = uni.getStorageSync('token')
    if (token) {
      uni.reLaunch({ url: '/pages/index/index' })
    }

    // 加载记住的密码
    const savedUsername = uni.getStorageSync('savedUsername')
    const savedPassword = uni.getStorageSync('savedPassword')
    if (savedUsername && savedPassword) {
      this.formData.username = savedUsername
      this.formData.password = savedPassword
      this.formData.rememberMe = true
    }
  },
  methods: {
    refreshCaptcha() {
      this.captchaUrl = `/api/auth/captcha?t=${Date.now()}`
    },
    async handleLogin() {
      if (!this.formData.username) {
        uni.showToast({ title: '请输入用户名', icon: 'none' })
        return
      }
      if (!this.formData.password) {
        uni.showToast({ title: '请输入密码', icon: 'none' })
        return
      }

      this.loading = true

      try {
        const res = await login({
          username: this.formData.username,
          password: this.formData.password,
          captcha: this.formData.captcha
        })

        // 保存token
        uni.setStorageSync('token', res.data.token)

        // 获取用户信息
        const userRes = await getUserInfo()
        uni.setStorageSync('userInfo', userRes.data.user)

        // 记住密码
        if (this.formData.rememberMe) {
          uni.setStorageSync('savedUsername', this.formData.username)
          uni.setStorageSync('savedPassword', this.formData.password)
        } else {
          uni.removeStorageSync('savedUsername')
          uni.removeStorageSync('savedPassword')
        }

        uni.showToast({ title: '登录成功', icon: 'success' })

        setTimeout(() => {
          uni.reLaunch({ url: '/pages/index/index' })
        }, 1000)
      } catch (error) {
        console.error('登录失败:', error)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea, #764ba2);
  padding: 100rpx 50rpx;

  .login-header {
    text-align: center;
    margin-bottom: 80rpx;

    .logo {
      width: 120rpx;
      height: 120rpx;
      margin-bottom: 30rpx;
    }

    .title {
      font-size: 48rpx;
      font-weight: bold;
      color: #fff;
    }

    .subtitle {
      font-size: 28rpx;
      color: rgba(255, 255, 255, 0.8);
      margin-top: 20rpx;
    }
  }

  .login-form {
    background-color: #fff;
    border-radius: 20rpx;
    padding: 60rpx 40rpx;

    .form-item {
      display: flex;
      align-items: center;
      padding: 30rpx 0;
      border-bottom: 1rpx solid #eee;

      .form-icon {
        width: 50rpx;
        color: #409eff;
        font-size: 36rpx;
      }

      .form-input {
        flex: 1;
        font-size: 30rpx;
        height: 50rpx;
      }

      .captcha-input {
        width: 300rpx;
      }

      .captcha-img {
        width: 150rpx;
        height: 50rpx;
      }
    }

    .form-checkbox {
      display: flex;
      align-items: center;
      padding: 30rpx 0;
      font-size: 26rpx;
      color: #606266;

      checkbox {
        margin-right: 10rpx;
      }
    }

    .btn-login {
      width: 100%;
      height: 90rpx;
      line-height: 90rpx;
      background-color: #409eff;
      color: #fff;
      border-radius: 12rpx;
      font-size: 34rpx;
      margin-top: 40rpx;
    }
  }

  .login-footer {
    text-align: center;
    padding-top: 60rpx;
    color: rgba(255, 255, 255, 0.6);
    font-size: 24rpx;
  }
}
</style>