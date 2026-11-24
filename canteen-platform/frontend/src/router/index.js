import Vue from 'vue'
import Router from 'vue-router'
import axios from 'axios'

Vue.use(Router)

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { title: '登录' } },
  { path: '/legal', name: 'legal', component: () => import('../views/LegalNotice.vue'), meta: { title: '法律声明', anon: true } },
  { path: '/sitemap', name: 'sitemap', component: () => import('../views/Sitemap.vue'), meta: { title: '网站地图', anon: true } },
  { path: '/', redirect: '/dish' },
  { path: '/dish', name: 'dish', component: () => import('../views/DishLibrary.vue'), meta: { title: '菜品管理' } },
  { path: '/ingredient', name: 'ingredient', component: () => import('../views/IngredientPrice.vue'), meta: { title: '食材管理' } },
  { path: '/ingredient-category', name: 'ingredient-category', component: () => import('../views/IngredientCategoryManage.vue'), meta: { title: '食材类别管理' } },
  { path: '/supplier', name: 'supplier', component: () => import('../views/SupplierManage.vue'), meta: { title: '供应商管理' } },
  { path: '/price-inquiry', name: 'price-inquiry', component: () => import('../views/MarketInquiry.vue'), meta: { title: '市场询价' } },
  { path: '/price-compare', name: 'price-compare', component: () => import('../views/PriceCompare.vue'), meta: { title: '价格比对' } },
  { path: '/recipe', name: 'recipe', component: () => import('../views/WeeklyRecipe.vue'), meta: { title: '周食谱管理' } },
  { path: '/purchase', name: 'purchase', component: () => import('../views/PurchaseSummary.vue'), meta: { title: '食材汇总' } },
  { path: '/publicity', name: 'publicity', component: () => import('../views/Publicity.vue'), meta: { title: '菜品公示' } },
  { path: '/stats/ingredient', name: 'stats-ingredient', component: () => import('../views/StatsIngredient.vue'), meta: { title: '食材统计' } },
  { path: '/stats/dish', name: 'stats-dish', component: () => import('../views/StatsDish.vue'), meta: { title: '菜品统计' } },
  { path: '/pa/consumption', name: 'pa-consumption', component: () => import('../views/PaConsumption.vue'), meta: { title: '消费数据分析' } },
  { path: '/pa/companion', name: 'pa-companion', component: () => import('../views/PaCompanion.vue'), meta: { title: '陪餐数据分析' } },
  { path: '/pa/analysis', name: 'pa-analysis', component: () => import('../views/PaAnalysis.vue'), meta: { title: '陪餐支付分析' } },
  { path: '/user', name: 'user', component: () => import('../views/UserManage.vue'), meta: { title: '用户管理' } },
  { path: '/role', name: 'role', component: () => import('../views/RoleManage.vue'), meta: { title: '角色管理' } },
  { path: '/page-content', name: 'page-content', component: () => import('../views/PageContent.vue'), meta: { title: '页面内容管理' } }
]

const router = new Router({ routes })

/** 校验当前会话：已登录返回 {username,roles}，否则返回 null */
async function checkAuth() {
  try {
    const resp = await axios.get('/api/auth/me', { timeout: 4000 })
    const d = resp.data
    if (d && d.code === 0 && d.data && d.data.username) {
      return d.data
    }
    return null
  } catch (e) {
    return null
  }
}

// 需要 admin 角色才能访问的页面
const ADMIN_PAGES = ['/user', '/role', '/page-content']

// 路由守卫：未登录时任何页面（含直接输入模块地址）都跳转登录页；用户管理/角色管理仅 admin 可进
router.beforeEach(async (to, from, next) => {
  // 免登录的静态页（法律声明、网站地图）
  if (to.meta && to.meta.anon) return next()
  const me = await checkAuth()
  if (to.path === '/login') {
    if (me) return next('/dish')
    return next()
  }
  if (!me) {
    localStorage.removeItem('canteen_user')
    return next('/login')
  }
  localStorage.setItem('canteen_user', JSON.stringify(me))
  if (ADMIN_PAGES.indexOf(to.path) >= 0) {
    const isAdmin = (me.username || '').toLowerCase() === 'admin' || (me.roles || []).indexOf('admin') >= 0
    if (!isAdmin) return next('/dish')
  }
  next()
})

router.afterEach((to) => {
  document.title = (to.meta && to.meta.title ? to.meta.title + ' - ' : '') + '食堂管理数据平台'
})

export default router
