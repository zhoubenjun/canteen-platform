<template>
  <div id="app">
    <div class="top-nav" v-if="!isBarePage">
      <div class="brand"><img src="/static/school-logo.png" class="brand-logo" alt="校徽" /><span class="brand-text">郧西县职业技术学校数字食堂</span></div>
      <el-dropdown class="nav-drop" v-if="isAdmin" @command="onUserCmd">
        <span class="nav-tab" :class="{ active: isUserActive }">用户管理<i class="el-icon-arrow-down el-icon--right"></i></span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="user">用户管理</el-dropdown-item>
          <el-dropdown-item command="role">角色管理</el-dropdown-item>
          <el-dropdown-item command="page-content">页面内容管理</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
      <el-dropdown class="nav-drop" @command="onIngredientCmd">
        <span class="nav-tab" :class="{ active: isIngredientActive }">食材管理<i class="el-icon-arrow-down el-icon--right"></i></span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="ingredient-category">类别管理</el-dropdown-item>
          <el-dropdown-item command="supplier">供应商管理</el-dropdown-item>
          <el-dropdown-item command="ingredient">食材管理</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
      <el-dropdown class="nav-drop" @command="onPriceCmd">
        <span class="nav-tab" :class="{ active: isPriceActive }">价格比对<i class="el-icon-arrow-down el-icon--right"></i></span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="price-inquiry">市场询价</el-dropdown-item>
          <el-dropdown-item command="price-compare">价格比对</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
      <div
        v-for="tab in tabs"
        :key="tab.path"
        class="nav-tab"
        :class="{ active: $route.path === tab.path }"
        @click="$router.push(tab.path)"
      >{{ tab.title }}</div>
      <el-dropdown class="nav-drop" @command="onStatsCmd">
        <span class="nav-tab" :class="{ active: isStatsActive }">数据统计<i class="el-icon-arrow-down el-icon--right"></i></span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="stats-ingredient">食材统计</el-dropdown-item>
          <el-dropdown-item command="stats-dish">菜品统计</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
      <el-dropdown class="nav-drop" @command="onPaCmd">
        <span class="nav-tab" :class="{ active: isPaActive }">陪餐分析<i class="el-icon-arrow-down el-icon--right"></i></span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="pa-consumption">消费数据分析</el-dropdown-item>
          <el-dropdown-item command="pa-companion">陪餐数据分析</el-dropdown-item>
          <el-dropdown-item command="pa-analysis">陪餐支付分析</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
      <div class="spacer"></div>
      <div class="nav-tab" @click="openPublicity()">📺 大屏展示</div>
      <span class="nav-user" v-if="currentUser">
        <el-dropdown @command="onUserRightCmd">
          <span class="nav-tab">{{ currentUser.username }}<i class="el-icon-arrow-down el-icon--right"></i></span>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="changePwd">修改密码</el-dropdown-item>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </span>
    </div>
    <router-view />

    <el-dialog title="修改密码" :visible.sync="pwdVisible" width="420px" append-to-body>
      <el-form :model="pwdForm" label-width="90px">
        <el-form-item label="原密码">
          <el-input type="password" v-model="pwdForm.oldPassword" show-password></el-input>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input type="password" v-model="pwdForm.newPassword" show-password></el-input>
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input type="password" v-model="pwdForm.confirm" show-password></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="doChangePwd">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import api from './api'

export default {
  name: 'App',
  data() {
    return {
      user: this.readUser(),
      pwdVisible: false,
      pwdLoading: false,
      pwdForm: { oldPassword: '', newPassword: '', confirm: '' },
      heartbeatTimer: null,
      tabs: [
        { path: '/dish', title: '菜品管理' },
        { path: '/recipe', title: '周食谱管理' },
        { path: '/purchase', title: '食材汇总' },
        { path: '/publicity', title: '菜品公示' }
      ]
    }
  },
  watch: {
    '$route'() {
      this.user = this.readUser()
      if (this.isBarePage || this.isLoginPage) this.stopHeartbeat()
      else if (this.user) this.startHeartbeat()
      else this.stopHeartbeat()
    }
  },
  mounted() {
    if (!this.isBarePage && !this.isLoginPage) this.startHeartbeat()
  },
  beforeDestroy() {
    this.stopHeartbeat()
  },
  computed: {
    currentUser() {
      return this.user
    },
    isUserActive() {
      const p = this.$route.path
      return p === '/user' || p === '/role' || p === '/page-content'
    },
    isIngredientActive() {
      const p = this.$route.path
      return p === '/ingredient' || p === '/ingredient-category' || p === '/supplier'
    },
    isStatsActive() {
      return this.$route.path === '/stats/ingredient' || this.$route.path === '/stats/dish'
    },
    isPriceActive() {
      const p = this.$route.path
      return p === '/price-inquiry' || p === '/price-compare'
    },
    isPaActive() {
      const p = this.$route.path
      return p === '/pa/consumption' || p === '/pa/companion' || p === '/pa/analysis'
    },
    isAdmin() {
      if (!this.currentUser) return false
      const u = this.currentUser.username || ''
      if (u.toLowerCase() === 'admin') return true
      return (this.currentUser.roles || []).indexOf('admin') >= 0
    },
    /** 独立静态页（法律声明、网站地图）不显示顶部导航 */
    isBarePage() {
      return ['/legal', '/sitemap'].indexOf(this.$route.path) >= 0
    },
    /** 登录页/首页不需要在线心跳，避免残留失效会话弹出“未登录”提示 */
    isLoginPage() {
      return this.$route.path === '/login' || this.$route.path === '/' || this.$route.path === ''
    }
  },
  methods: {
    readUser() {
      try {
        const u = localStorage.getItem('canteen_user')
        return u ? JSON.parse(u) : null
      } catch (e) {
        return null
      }
    },
    /** 在线状态心跳：每 60 秒刷新一次，关闭页面后服务端约 2.5 分钟内判定离线 */
    startHeartbeat() {
      this.stopHeartbeat()
      if (!this.user) return
      const beat = () => {
        if (!localStorage.getItem('canteen_user')) return
        api.post('/api/auth/heartbeat').catch(() => {})
      }
      beat()
      this.heartbeatTimer = setInterval(beat, 60000)
    },
    stopHeartbeat() {
      if (this.heartbeatTimer) {
        clearInterval(this.heartbeatTimer)
        this.heartbeatTimer = null
      }
    },
    openPublicity() {
      window.open('/#/publicity?mode=bigscreen', '_blank')
    },
    onUserCmd(cmd) {
      if (cmd === 'user') this.$router.push('/user')
      else if (cmd === 'role') this.$router.push('/role')
      else if (cmd === 'page-content') this.$router.push('/page-content')
    },
    onIngredientCmd(cmd) {
      if (cmd === 'ingredient-category') this.$router.push('/ingredient-category')
      else if (cmd === 'supplier') this.$router.push('/supplier')
      else if (cmd === 'ingredient') this.$router.push('/ingredient')
    },
    onStatsCmd(cmd) {
      if (cmd === 'stats-ingredient') this.$router.push('/stats/ingredient')
      else if (cmd === 'stats-dish') this.$router.push('/stats/dish')
    },
    onPriceCmd(cmd) {
      if (cmd === 'price-inquiry') this.$router.push('/price-inquiry')
      else if (cmd === 'price-compare') this.$router.push('/price-compare')
    },
    onPaCmd(cmd) {
      if (cmd === 'pa-consumption') this.$router.push('/pa/consumption')
      else if (cmd === 'pa-companion') this.$router.push('/pa/companion')
      else if (cmd === 'pa-analysis') this.$router.push('/pa/analysis')
    },
    onUserRightCmd(cmd) {
      if (cmd === 'logout') this.logout()
      else if (cmd === 'changePwd') {
        this.pwdForm = { oldPassword: '', newPassword: '', confirm: '' }
        this.pwdVisible = true
      }
    },
    async doChangePwd() {
      const { oldPassword, newPassword, confirm } = this.pwdForm
      if (!oldPassword) return this.$message.warning('请输入原密码')
      if (!newPassword) return this.$message.warning('请输入新密码')
      if (newPassword.length < 6) return this.$message.warning('新密码必须大于等于6个字符')
      if (newPassword !== confirm) return this.$message.warning('两次输入的新密码不一致')
      this.pwdLoading = true
      try {
        await api.post('/api/auth/changePwd', { oldPassword, newPassword })
        this.pwdVisible = false
        this.$message.success('密码修改成功，请重新登录')
        this.logout()
      } catch (e) {
        // 错误提示已由拦截器统一处理
      } finally {
        this.pwdLoading = false
      }
    },
    async logout() {
      localStorage.removeItem('canteen_user')
      try { await api.post('/api/auth/logout') } catch (e) {}
      this.$router.push('/login')
    }
  }
}
</script>
