<template>
  <div class="doc-page">
    <header class="doc-header">
      <img src="/static/school-logo.png" class="doc-logo" alt="郧西县职业技术学校校徽" />
      <div class="doc-school">
        <div class="doc-school-name">郧西县职业技术学校</div>
        <div class="doc-school-sub">数字食堂管理平台</div>
      </div>
    </header>

    <main class="doc-body">
      <h1 class="doc-title">网站地图</h1>

      <div class="map-group" v-for="g in groups" :key="g.title">
        <div class="map-group-title">{{ g.title }}</div>
        <div class="map-links">
          <a v-for="it in g.items" :key="it.path" :href="'#' + it.path">{{ it.title }}</a>
        </div>
      </div>

      <div class="map-note">未登录时点击上述链接会先跳转到登录页；登录后直接进入对应模块。</div>
    </main>

    <footer class="doc-footer">
      <div>郧西县职业技术学校主办 · 版权所有：郧西县职业技术学校和周本君老师联合开发</div>
      <div>联系邮箱：<a href="mailto:tchr_zhou@163.com">tchr_zhou@163.com</a> · 地址：十堰市郧西县城关镇江岸路1号</div>
    </footer>
  </div>
</template>

<script>
export default {
  name: 'Sitemap',
  mounted() { document.body.style.minWidth = '0px' },
  beforeDestroy() { document.body.style.minWidth = '' },
  data() {
    return {
      groups: [
        { title: '基础数据', items: [
          { path: '/dish', title: '菜品管理' },
          { path: '/ingredient', title: '食材管理' },
          { path: '/ingredient-category', title: '食材类别管理' },
          { path: '/supplier', title: '供应商管理' }
        ] },
        { title: '价格管理', items: [
          { path: '/price-inquiry', title: '市场询价' },
          { path: '/price-compare', title: '价格比对' }
        ] },
        { title: '食谱与采购', items: [
          { path: '/recipe', title: '周食谱管理' },
          { path: '/purchase', title: '食材汇总' },
          { path: '/publicity', title: '菜品公示' }
        ] },
        { title: '数据统计', items: [
          { path: '/stats/ingredient', title: '食材统计' },
          { path: '/stats/dish', title: '菜品统计' }
        ] },
        { title: '陪餐分析', items: [
          { path: '/pa/consumption', title: '消费数据分析' },
          { path: '/pa/companion', title: '陪餐数据分析' },
          { path: '/pa/analysis', title: '陪餐支付分析' }
        ] },
        { title: '系统管理', items: [
          { path: '/user', title: '用户管理' },
          { path: '/role', title: '角色管理' },
          { path: '/page-content', title: '页面内容管理' }
        ] },
        { title: '其他', items: [
          { path: '/login', title: '登录' },
          { path: '/legal', title: '法律声明' }
        ] }
      ]
    }
  }
}
</script>

<style scoped>
.doc-page { min-height: 100vh; background: #f5f7fa; display: flex; flex-direction: column; }
.doc-header {
  display: flex; align-items: center; justify-content: center; gap: 14px;
  padding: 22px 16px; background: linear-gradient(135deg, #1e56ab, #2f6fd0 55%, #4a8ad8); color: #fff;
  box-shadow: 0 2px 10px rgba(30, 86, 171, .25);
}
.doc-logo { width: 52px; height: 52px; border-radius: 50%; background: #fff; object-fit: cover; flex: none; box-shadow: 0 1px 4px rgba(0,0,0,.2); }
.doc-school-name { font-size: 22px; font-weight: 800; letter-spacing: 1px; }
.doc-school-sub { font-size: 13px; opacity: .88; margin-top: 2px; letter-spacing: .5px; }
.doc-body {
  flex: 1; width: 100%; max-width: 880px; margin: 26px auto; background: #fff;
  padding: 36px 52px 40px; box-sizing: border-box; box-shadow: 0 4px 18px rgba(30, 60, 110, .08); border-radius: 6px;
}
.doc-title { text-align: center; font-size: 22px; font-weight: 800; color: #1c3f73; letter-spacing: 3px; margin: 0 0 26px; }
.map-group { margin-bottom: 18px; }
.map-group-title { font-size: 15px; font-weight: 700; color: #1c4f9c; padding-bottom: 6px; border-bottom: 1px solid #e6ecf5; margin-bottom: 8px; }
.map-links { display: flex; flex-wrap: wrap; gap: 8px 24px; }
.map-links a { font-size: 14px; color: #2b3542; text-decoration: none; }
.map-links a:hover { color: #1c4f9c; text-decoration: underline; }
.map-note { margin-top: 18px; font-size: 12px; color: #8a96a6; }
.doc-footer { padding: 0 16px 26px; text-align: center; font-size: 12px; line-height: 1.9; color: #7a8798; }
.doc-footer a { color: #1c4f9c; text-decoration: none; }
.doc-footer a:hover { text-decoration: underline; }
</style>
