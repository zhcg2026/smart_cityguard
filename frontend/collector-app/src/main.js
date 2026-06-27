import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'

// Vant 样式会通过 unplugin 自动导入
import 'vant/lib/index.css'
import { setNotifyDefaultOptions } from 'vant'

setNotifyDefaultOptions({
  type: 'primary',
  background: '#1989fa',
  color: '#fff',
  className: 'collector-push-notify'
})

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')