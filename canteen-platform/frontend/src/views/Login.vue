<template>
  <div class="login-wrap">
    <div class="login-box">
      <div class="login-card">
        <div class="login-brand"><img src="/static/school-logo.png" class="login-logo" alt="校徽" />郧西县职业技术学校数字食堂</div>
        <div class="login-sub"></div>
        <el-form @submit.native.prevent="doLogin">
          <el-input v-model="username" placeholder="请输入用户名" prefix-icon="el-icon-user" size="medium" clearable />
          <el-input v-model="password" type="password" placeholder="请输入密码" prefix-icon="el-icon-lock" size="medium" show-password @keyup.enter.native="doLogin" />
          <el-button type="primary" size="medium" :loading="loading" class="login-btn" @click="doLogin">登 录</el-button>
        </el-form>
      </div>

      <div class="login-footer">
        <div class="foot-line foot-links">
          <a href="http://www.yxzjedu.cn/" target="_blank" rel="noopener">关于我们</a>
          <span class="dash">-</span>
          <a href="#/legal" target="_blank" rel="noopener">法律声明</a>
          <span class="dash">-</span>
          <a href="#/sitemap" target="_blank" rel="noopener">网站地图</a>
          <span class="org">郧西县职业技术学校主办</span>
        </div>
        <div class="foot-line">版权所有：郧西县职业技术学校和周本君老师联合开发</div>
        <div class="foot-line">联系邮箱：tchr_zhou@163.com</div>
        <div class="foot-line">地址：十堰市郧西县城关镇江岸路1号</div>
      </div>
    </div>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'Login',
  data() {
    return { username: '', password: '', loading: false }
  },
  methods: {
    doLogin() {
      if (!this.username.trim()) return this.$message.warning('请输入用户名')
      if (!this.password) return this.$message.warning('请输入密码')
      this.loading = true
      api.post('/api/auth/login', { username: this.username.trim(), password: this.password })
        .then(user => {
          localStorage.setItem('canteen_user', JSON.stringify({ username: user.username, roles: user.roles || [] }))
          this.$message.success('登录成功')
          this.$router.push('/dish')
        })
        .catch(() => {})
        .finally(() => { this.loading = false })
    }
  }
}
</script>

<style scoped>
.login-wrap {
  position: fixed; inset: 0; z-index: 2000; overflow-y: auto;
  background: linear-gradient(135deg, #1e56ab, #2f6fd0 55%, #4a8ad8);
  display: flex; align-items: center; justify-content: center; padding: 24px 16px;
}
.login-box { display: flex; flex-direction: column; align-items: center; }
.login-card {
  width: 470px; background: #fff; border-radius: 12px; padding: 32px 42px 40px;
  box-shadow: 0 12px 36px rgba(120, 60, 20, .35); text-align: center;
}
.login-brand { font-size: 24px; font-weight: 800; color: #1c3f73; display: flex; align-items: center; justify-content: center; }
.login-brand .login-logo { width: 56px; height: 56px; border-radius: 50%; object-fit: cover; background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,.18); margin-right: 12px; flex: none; }
.login-sub { height: 0; margin: 0 0 30px; }
.login-card .el-input { margin-bottom: 16px; }
.login-btn { width: 100%; margin-top: 6px; }

.login-footer {
  margin-top: 22px; width: auto; max-width: 100%; text-align: left;
  color: rgba(255, 255, 255, .92); font-size: 13px; line-height: 1.9;
  text-shadow: 0 1px 2px rgba(20, 45, 90, .35);
}
.login-footer .foot-links { display: flex; align-items: baseline; justify-content: flex-start; flex-wrap: wrap; }
.login-footer .foot-links .dash { margin: 0 8px; opacity: .8; }
.login-footer .foot-links .org { margin-left: 14px; }
.login-footer a { color: #fff; text-decoration: none; border-bottom: 1px solid rgba(255, 255, 255, .55); }
.login-footer a:hover { color: #ffe9b8; border-bottom-color: #ffe9b8; }
</style>
