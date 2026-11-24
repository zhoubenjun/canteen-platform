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
      <h1 class="doc-title">{{ title }}</h1>
      <p v-for="(p, i) in paras" :key="i" :class="p.cls">{{ p.text }}</p>
      <div v-if="loadFailed" class="doc-empty">正文暂时无法加载，请稍后刷新重试。</div>
    </main>

    <footer class="doc-footer">
      <div>郧西县职业技术学校主办 · 版权所有：郧西县职业技术学校和周本君老师联合开发</div>
      <div>联系邮箱：tchr_zhou@163.com · 地址：十堰市郧西县城关镇江岸路1号</div>
    </footer>
  </div>
</template>

<script>
import api from '../api'

/** 接口不可用时的兜底正文（与数据库初始内容一致），保证页面不空白 */
const FALLBACK_TITLE = '法律声明'
const FALLBACK_TEXT = [
  '郧西县职业技术学校数字食堂及其注册用户及本系统的资料提供者拥有本系统所有资料的版权。未经郧西县职业技术学校的明确书面许可，任何人不得复制或在非“郧西县职业技术学校”所属的服务器上做镜像。',
  '郧西县职业技术学校数字食堂（以下简称“本系统”）是郧西县职业技术学校主办，郧西县职业技术学校对其实施管理、指导、协调和监督的学校系统。',
  '特此说明本系统对系统资料所采取的政策，请您务必仔细阅读：',
  '1、资料保护',
  '使用本系统的用户，享有本系统的使用权以及查询数据的权利，但未经郧西县职业技术学校管理中心的书面许可，任何人不得复制或在非“郧西县职业技术学校数字”所属的服务器上做镜像。',
  '2、免 责',
  '下列情况时本系统毋需承担任何责任：',
  '- 1) 任何由于计算机2000年问题、黑客攻击、计算机病毒侵入或发作、因学校管制而造成的暂时性关闭等影响网络正常经营之不可抗力而造成的资料泄露、丢失、被盗用或被篡改等。',
  '- 2) 由于与本系统链接的其他网站所造成之个人资料泄露及由此而导致的任何法律争议和后果。'
].join('\n')

export default {
  name: 'LegalNotice',
  data() {
    return {
      title: FALLBACK_TITLE,
      paras: this.parse(FALLBACK_TEXT),
      loadFailed: false
    }
  },
  mounted() {
    document.body.style.minWidth = '0px'
    this.load()
  },
  beforeDestroy() {
    document.body.style.minWidth = ''
  },
  methods: {
    /** 正文按行解析：每行一段，"- " 开头为二级缩进子项 */
    parse(content) {
      return (content || '')
        .split(/\r?\n/)
        .map(s => s.trim())
        .filter(Boolean)
        .map(s => (s.indexOf('- ') === 0
          ? { cls: 'ind2', text: s.slice(2) }
          : { cls: 'ind', text: s }))
        .filter(p => p.text)
    },
    async load() {
      try {
        const data = await api.get('/api/page/legal')
        if (data && data.content) {
          this.title = data.pageTitle || FALLBACK_TITLE
          this.paras = this.parse(data.content)
          this.loadFailed = false
        }
      } catch (e) {
        this.loadFailed = true
      }
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
  flex: 1; width: 100%; max-width: 880px; margin: 0 auto; background: #fff;
  padding: 40px 52px 46px; box-sizing: border-box; box-shadow: 0 4px 18px rgba(30, 60, 110, .08);
  margin-top: 26px; margin-bottom: 26px; border-radius: 6px;
}
.doc-title { text-align: center; font-size: 22px; font-weight: 800; color: #1c3f73; letter-spacing: 3px; margin: 0 0 28px; }
.doc-body p { font-size: 15px; line-height: 2; color: #2b3542; margin: 0 0 8px; text-align: justify; }
.ind { text-indent: 2em; }
.ind2 { text-indent: 4em; }
.doc-empty { color: #b06a2c; font-size: 13px; margin-top: 16px; text-align: center; }
.doc-footer { padding: 0 16px 26px; text-align: center; font-size: 12px; line-height: 1.9; color: #7a8798; }
</style>
