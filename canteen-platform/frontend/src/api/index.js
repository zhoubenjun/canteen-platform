import axios from 'axios'
import { Message } from 'element-ui'

const http = axios.create({ baseURL: '', timeout: 60000 })

/** 匿名静态页（法律声明、网站地图）：不做登录跳转、不弹登录提示 */
function isAnonPage() {
  return /^#\/(legal|sitemap)/.test(window.location.hash || '')
}

http.interceptors.response.use(
  res => {
    if (res.config.responseType === 'blob') return res
    const d = res.data
    if (d && d.code !== undefined) {
      if (d.code === 0) return d.data
      if (!isAnonPage()) Message.error(d.message || '操作失败')
      return Promise.reject(new Error(d.message))
    }
    return d
  },
  err => {
    // 未登录：清理会话并跳转登录页
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('canteen_user')
      if (!isAnonPage() && window.location.hash !== '#/login') window.location.hash = '#/login'
      return Promise.reject(err)
    }
    if (!isAnonPage()) Message.error(err.response ? (err.response.data && err.response.data.message) || '请求失败' : '网络错误')
    return Promise.reject(err)
  }
)

export default {
  get: (url, params) => http.get(url, { params }),
  post: (url, data, config) => http.post(url, data, config),
  put: (url, data) => http.put(url, data),
  del: (url) => http.delete(url),
  blob: (url, params) => http.get(url, { params, responseType: 'blob' }),
  upload: (url, formData) => http.post(url, formData)
}
