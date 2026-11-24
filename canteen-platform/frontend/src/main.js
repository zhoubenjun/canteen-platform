import Vue from 'vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import App from './App.vue'
import router from './router'
import './assets/theme.css'

Vue.use(ElementUI, { size: 'small' })
Vue.config.productionTip = false

// 全局下载文件辅助
Vue.prototype.$download = function (blob, filename) {
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
