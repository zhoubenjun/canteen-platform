<template>
  <div>
    <!-- ============ 大屏模式 ============ -->
    <div v-if="isBigscreen" class="bigscreen" :class="{fs: isFs}">
      <div class="big-top">
        <div class="big-name"><img src="/static/school-logo.png" class="big-logo" alt="校徽" />{{ settings.canteen_name || '食堂' }}</div>
        <div style="font-size:18px">{{ today }} {{ nowTime }}</div>
        <div class="big-period" :class="period.enabled ? (currentPeriod ? 'on' : 'off') : 'manual'">
          <i class="el-icon-time"></i>{{ periodLabel }}
        </div>
        <el-select v-model="big.day" size="small" style="width:140px" :disabled="period.enabled" @change="loadBig">
          <el-option v-for="(n,i) in dayNames" :key="i" :label="n" :value="i" />
        </el-select>
        <el-select v-model="big.meal" size="small" style="width:100px" :disabled="period.enabled" @change="loadBig">
          <el-option label="全部" value="全部" /><el-option label="早餐" value="早餐" /><el-option label="中餐" value="中餐" /><el-option label="晚餐" value="晚餐" />
        </el-select>
        <el-select v-model="big.perPage" size="small" style="width:110px" @change="resetBig">
          <el-option v-for="n in [4,6,8,10,15,20,30]" :key="n" :label="'每页 ' + n + ' 个'" :value="n" />
        </el-select>
        <el-button size="small" type="info" @click="bigSettings=true">设置</el-button>
        <el-button size="small" type="primary" icon="el-icon-full-screen" @click="toggleFullscreen">{{ isFs ? '退出全屏' : '全屏' }}</el-button>
        <el-button size="small" type="info" @click="togglePause">{{ big.paused ? '▶ 播放' : '⏸ 暂停' }}</el-button>
        <el-button size="small" type="danger" @click="closeBig">退出</el-button>
      </div>

      <!-- 全屏 + 非就餐时段：左上角常显校徽与校名 -->
      <div v-if="isFs && isNoticeMode" class="fs-brand">
        <img src="/static/school-logo.png" class="fs-brand-logo" alt="校徽" />
        <span>{{ settings.canteen_name || '食堂' }}</span>
      </div>

      <!-- 非用餐时段：居中展示可编辑提示文字 -->
      <div v-if="isNoticeMode" class="big-notice" ref="noticeBox">
        <div class="big-notice-inner" ref="noticeInner">
          <div class="notice-rule"></div>
          <div class="notice-text" :key="noticeIdx" ref="noticeText">{{ noticeLine }}</div>
          <div class="notice-rule small"></div>
        </div>
      </div>

      <!-- 当餐暂无菜品 -->
      <div v-else-if="autoEmpty" class="big-notice">
        <div class="big-notice-inner">
          <div class="notice-rule"></div>
          <div class="notice-text notice-empty">今日暂无{{ currentPeriod ? currentPeriod.meal : '' }}菜品安排</div>
          <div class="notice-rule small"></div>
        </div>
      </div>

      <div v-else class="big-grid" :style="gridStyle">
        <div v-for="(c,i) in bigPageCards" :key="'c'+i" class="big-card" :class="{'big-empty': !c}">
          <template v-if="c">
            <div class="img-wrap" v-if="bigConfig.showImage && c.image">
              <img :src="c.image" alt="" />
            </div>
            <div class="big-info" v-if="bigConfig.showImage && c.image">
              <span class="big-name">{{ c.name }}</span>
              <span class="big-price" v-if="bigConfig.showAudit">¥{{ Number(c.auditPrice||0).toFixed(2) }}</span>
            </div>
            <template v-else>
              <div class="big-full">
                <div class="big-fname">{{ c.name }}</div>
                <div class="big-fmeta" v-if="bigConfig.showCategory">{{ c.category }}</div>
                <div class="big-fmeta" v-if="bigConfig.showIngredients">{{ c.ingredientsText }}</div>
                <div class="big-fprice" v-if="bigConfig.showAudit">审核价 ¥{{ Number(c.auditPrice||0).toFixed(2) }}</div>
                <div class="big-fmeta" v-if="bigConfig.showSuggest">建议价 ¥{{ Number(c.suggestPrice||0).toFixed(2) }}</div>
              </div>
            </template>
          </template>
        </div>
      </div>

      <div class="big-nav" v-if="!isNoticeMode && !autoEmpty">
        <el-button size="small" type="info" @click="bigPrev">◀</el-button>
        <div style="display:flex">
          <span v-for="(_,i) in bigPages" :key="i" class="big-dot" :class="{active: i===big.page}" @click="big.page=i"></span>
        </div>
        <el-button size="small" type="info" @click="bigNext">▶</el-button>
      </div>

      <el-dialog title="大屏设置" :visible.sync="bigSettings" width="580px" append-to-body>
        <el-form label-width="120px" size="small">
          <el-form-item label="每页数量">
            <el-select v-model="big.perPage" style="width:180px" @change="resetBig">
              <el-option v-for="n in [4,6,8,10,15,20,30]" :key="n" :label="'每页 ' + n + ' 个'" :value="n" />
            </el-select>
          </el-form-item>
          <el-form-item label="显示图片背景"><el-switch v-model="bigConfig.showImage" /></el-form-item>
          <el-form-item label="显示分类"><el-switch v-model="bigConfig.showCategory" /></el-form-item>
          <el-form-item label="显示配料"><el-switch v-model="bigConfig.showIngredients" /></el-form-item>
          <el-form-item label="显示审核价"><el-switch v-model="bigConfig.showAudit" /></el-form-item>
          <el-form-item label="显示建议价"><el-switch v-model="bigConfig.showSuggest" /></el-form-item>
          <el-form-item label="食堂名称"><el-input v-model="settings.canteen_name" /></el-form-item>

          <el-divider content-position="left">用餐时段（自动切换）</el-divider>
          <el-form-item label="按时段切换">
            <el-switch v-model="period.enabled" />
            <span class="period-hint">开启后大屏按当前时间自动显示当天对应餐次，非用餐时段显示下方文字</span>
          </el-form-item>
          <el-form-item v-for="row in periodRows" :key="row.key" :label="row.label">
            <el-time-picker v-model="period[row.startKey]" value-format="HH:mm" format="HH:mm" placeholder="开始"
              :picker-options="{ selectableRange: '00:00:00-23:59:59' }" style="width:120px"></el-time-picker>
            <span class="period-hint" style="margin:0 6px">至</span>
            <el-time-picker v-model="period[row.endKey]" value-format="HH:mm" format="HH:mm" placeholder="结束"
              :picker-options="{ selectableRange: '00:00:00-23:59:59' }" style="width:120px"></el-time-picker>
            <span class="period-hint" style="margin-left:10px">{{ row.meal }}菜品</span>
          </el-form-item>
          <el-form-item label="非用餐时段文字">
            <el-input type="textarea" :rows="4" maxlength="300" show-word-limit v-model="period.text"
              placeholder="非用餐时段在大屏正中居中显示的提示文字，可换行"></el-input>
            <div class="period-hint">每行建议不超过 20 字，大屏会按屏幕大小自动缩放，保证整行居中显示</div>
          </el-form-item>
          <el-form-item label="当前判定">
            <span class="period-now">{{ periodLabel }}</span>
            <span class="period-hint" style="margin-left:8px">保存后大屏立即按新时段展示</span>
          </el-form-item>
        </el-form>
        <span slot="footer">
          <el-button @click="bigSettings=false">取消</el-button>
          <el-button type="primary" @click="saveBigSettings">保存</el-button>
        </span>
      </el-dialog>
    </div>

    <!-- ============ 普通公示模式 ============ -->
    <div v-else class="page">
      <div class="page-card">
        <div class="toolbar">
          <el-select v-model="weekKey" style="width:170px" @change="load">
            <el-option v-for="w in weekKeys" :key="w" :label="w" :value="w" />
          </el-select>
          <el-button @click="toggleWeek">整周公示</el-button>
          <span class="flex" style="margin-left:6px">
            <el-checkbox v-for="(n,i) in dayNames" :key="i" v-model="pubDays[i]" :label="n" />
          </span>
          <el-divider direction="vertical"></el-divider>
          <el-checkbox v-model="showIngredients">配料</el-checkbox>
          <el-checkbox v-model="showAudit">审核价</el-checkbox>
          <el-checkbox v-model="showSuggest">建议售价</el-checkbox>
          <div class="spacer" style="flex:1"></div>
          <el-button type="primary" @click="editTitle">修改标题</el-button>
          <el-button type="primary" @click="doPrint">打印公示</el-button>
        </div>
      </div>

      <!-- 公示大标题：整行居中，与下方餐次表格留出较大间隔 -->
      <div class="pub-bigtitle" style="display:block;width:100%;text-align:center;margin:0 0 32px 0;font-size:22px;font-weight:800;color:#16325a;letter-spacing:2px;">{{ pubTitle }}</div>

      <!-- 每日公示卡片（按2天一组，打印时每2天适配一页） -->
      <div v-for="(pair, gi) in dayPairs" :key="gi" class="pub-pair">
        <div v-for="di in pair" :key="di" class="pub-day" :style="pubDayStyle(di)">
          <div class="pub-body">
            <div class="pub-date">{{ pubDate(di) }} {{ dayNames[di] }}</div>
            <div class="pub-wrap">
              <div class="pub-mealheads" v-if="di === visiblePubDays[0]">
                <div class="pub-mealhead" v-for="meal in meals" :key="'h'+meal">{{ meal }}</div>
              </div>
              <div class="pub-meals" :style="pubMealsStyle(di)">
                <div class="pub-col" v-for="meal in meals" :key="meal" :style="{ background: dayColor(di) }">
                  <template v-if="mealsData[di+'-'+meal] && mealsData[di+'-'+meal].length">
                    <div class="pub-items" :style="{ columnCount: mealCols(di, meal) }">
                      <div v-for="(item,idx) in mealsData[di+'-'+meal]" :key="idx" class="pub-item">
                        <div class="flex">
                          <span class="pn">{{ item.name }}</span>
                          <span v-if="showAudit" class="pub-price-audit">¥{{ Number(item.auditPrice||0).toFixed(2) }}</span>
                          <span v-if="showSuggest" class="pub-price-suggest">建议¥{{ Number(item.suggestPrice||0).toFixed(2) }}</span>
                        </div>
                        <div v-if="showIngredients && item.ingredientsText" class="pub-ing">{{ item.ingredientsText }}</div>
                      </div>
                    </div>
                  </template>
                  <div v-else class="pub-empty">暂无</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-if="!visiblePubDays.length" class="text-muted" style="text-align:center;padding:40px">请勾选要公示的天</div>
    </div>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'Publicity',
  data() {
    return {
      dayNames: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      meals: ['早餐', '中餐', '晚餐'],
      weekKey: '', weekKeys: [],
      pubDays: [true, true, true, true, true, true, true],
      pubTitle: '郧西县职业技术学校每日食谱',
      showIngredients: false, showAudit: false, showSuggest: false,
      mealsData: {}, allDays: [],
      settings: {},
      isBigscreen: false,
      isFs: false,
      nowTime: '', today: '',
      big: { day: 0, meal: '全部', perPage: 10, page: 0, paused: false, data: [], initialized: false, loaded: false, week: '' },
      bigConfig: { showImage: true, showCategory: true, showIngredients: true, showAudit: true, showSuggest: true, showSuggestPrice: false },
      // 用餐时段设置（可在大屏“设置”中修改，保存在后端 settings）
      period: {
        enabled: true,
        breakfastStart: '07:00', breakfastEnd: '08:00',
        lunchStart: '11:00', lunchEnd: '12:30',
        dinnerStart: '16:30', dinnerEnd: '18:30',
        text: '当前为非用餐时间\n早餐 7:00-8:00　中餐 11:00-12:30\n晚餐 16:30-18:30\n请按时到食堂就餐，珍惜粮食、文明用餐'
      },
      nowMinute: new Date().getHours() * 60 + new Date().getMinutes(),
      bigSettings: false,
      noticeIdx: 0,
      timer: null,
      clockTimer: null
    }
  },
  watch: {
    'period.text'() { this.noticeIdx = 0; this.fitNotice(); this.startNoticeRotate() },
    noticeIdx() { this.fitNotice() },
    isNoticeMode(v) {
      if (v) {
        this.noticeIdx = 0
        this.startNoticeRotate()
        this.fitNotice()
      } else {
        this.stopNoticeRotate()
      }
    }
  },
  computed: {
    // 非用餐提示文字：按换行拆成多条，每隔 10 秒轮播一条
    noticeLines() {
      return String(this.period.text || '').split(/\r?\n/).map(s => s.trim()).filter(Boolean)
    },
    noticeLine() {
      const lines = this.noticeLines
      if (!lines.length) return ''
      return lines[this.noticeIdx % lines.length]
    },
    effWeek() { return this.mondayOf(this.weekKey) },
    visiblePubDays() { return this.pubDays.map((v, i) => v ? i : -1).filter(i => i >= 0) },
    dayPairs() {
      const arr = []
      for (let i = 0; i < this.visiblePubDays.length; i += 2) {
        arr.push(this.visiblePubDays.slice(i, i + 2))
      }
      return arr
    },
    bigPageCards() {
      const per = this.big.perPage
      const start = this.big.page * per
      const arr = []
      for (let i = 0; i < per; i++) arr.push(this.big.data[start + i] || null)
      return arr
    },
    bigPages() { return Math.max(1, Math.ceil(this.big.data.length / this.big.perPage)) },
    // 三餐时段行（设置面板/时段判定共用）
    periodRows() {
      return [
        { key: 'breakfast', label: '早餐时间', meal: '早餐', startKey: 'breakfastStart', endKey: 'breakfastEnd' },
        { key: 'lunch', label: '中餐时间', meal: '中餐', startKey: 'lunchStart', endKey: 'lunchEnd' },
        { key: 'dinner', label: '晚餐时间', meal: '晚餐', startKey: 'dinnerStart', endKey: 'dinnerEnd' }
      ]
    },
    // 当前所处餐次；处于非用餐时段返回 null
    currentPeriod() {
      if (!this.period.enabled) return null
      const cur = this.nowMinute
      for (const r of this.periodRows) {
        const s = this.toMin(this.period[r.startKey])
        const e = this.toMin(this.period[r.endKey])
        if (s < 0 || e < 0) continue
        if (cur >= s && cur <= e) return { meal: r.meal, start: this.period[r.startKey], end: this.period[r.endKey] }
      }
      return null
    },
    periodLabel() {
      if (!this.period.enabled) return '手动模式'
      const p = this.currentPeriod
      return p ? p.meal + '时段 ' + p.start + '—' + p.end : '非用餐时段'
    },
    // 大屏处于“文字提示”状态
    isNoticeMode() { return this.isBigscreen && this.period.enabled && !this.currentPeriod },
    // 自动模式下当前餐次暂无菜品
    autoEmpty() {
      return this.isBigscreen && this.period.enabled && !!this.currentPeriod && this.big.loaded && !this.big.data.length
    },
    gridStyle() {
      const per = this.big.perPage
      if (per === 4) return 'grid-template-columns:repeat(2,1fr)'
      if (per === 6) return 'grid-template-columns:repeat(3,1fr)'
      if (per === 8) return 'grid-template-columns:repeat(4,1fr)'
      if (per === 10) return 'grid-template-columns:repeat(5,1fr)'
      if (per === 15) return 'grid-template-columns:repeat(5,1fr)'
      if (per === 20) return 'grid-template-columns:repeat(5,1fr)'
      return 'grid-template-columns:repeat(6,1fr)'
    }
  },
  created() {
    this.isBigscreen = new URLSearchParams(location.search).get('mode') === 'bigscreen' || /[?&]mode=bigscreen/.test(location.hash)
    this._fsHandler = () => { this.isFs = !!document.fullscreenElement }
    document.addEventListener('fullscreenchange', this._fsHandler)
    this._fitHandler = () => { if (this.isNoticeMode) this.fitNotice() }
    window.addEventListener('resize', this._fitHandler)
    api.get('/api/settings/all').then(d => { this.settings = d; if (d.publicity_title) this.pubTitle = d.publicity_title })
    api.get('/api/recipe/weekKeys').then(d => { this.weekKeys = d; this.weekKey = this.todayStr(); this.load() })
    if (this.isBigscreen) this.initBig()
  },
  beforeDestroy() {
    this.stopTimer(); this.stopClock(); this.stopNoticeRotate()
    if (this._fsHandler) document.removeEventListener('fullscreenchange', this._fsHandler)
    window.removeEventListener('resize', this._fitHandler)
  },
  methods: {
    toggleFullscreen() {
      if (document.fullscreenElement) {
        document.exitFullscreen()
      } else {
        const el = document.documentElement
        const req = el.requestFullscreen || el.webkitRequestFullscreen || el.msRequestFullscreen
        if (req) req.call(el)
      }
    },
    async load() {
      if (!this.weekKey) return
      const m = {}
      const keys = await api.get('/api/recipe/weekKeys')
      this.weekKeys = keys
      if (this.isBigscreen) { this.syncAutoPeriod(true); this.loadBig(); return }
      for (let di = 0; di < 7; di++) {
        const d = await api.get('/api/publicity/day', { weekKey: this.effWeek, dayIndex: di })
        d.meals.forEach(x => { if (x.dishes.length) m[di + '-' + x.meal] = x.dishes })
      }
      this.mealsData = m
    },
    toggleWeek() { const all = this.pubDays.every(Boolean); this.pubDays = this.pubDays.map(() => !all) },
    editTitle() {
      this.$prompt('公示标题', '修改标题', { inputValue: this.pubTitle }).then(({ value }) => {
        this.pubTitle = value
        api.put('/api/settings/all', { publicity_title: value })
        this.$message.success('标题已修改')
      }).catch(() => {})
    },
    pubDate(di) {
      try {
        const base = new Date(this.effWeek + 'T00:00:00')
        base.setDate(base.getDate() + di)
        return this.dateStr(base)
      } catch (e) { return this.effWeek }
    },
    doPrint() {
      const printArea = document.querySelector('.page')
      if (!printArea) return
      document.body.classList.add('printing')
      document.body.classList.add('pdf-export')
      setTimeout(() => {
        window.print()
        document.body.classList.remove('printing')
        document.body.classList.remove('pdf-export')
      }, 150)
    },
    goBigscreen() { window.open('/publicity?mode=bigscreen', '_blank') },
    // ---------- 大屏 ----------
    initBig() {
      this.big.day = new Date().getDay() === 0 ? 6 : new Date().getDay() - 1
      this.today = this.dateStr(new Date())
      api.get('/api/settings/all').then(d => {
        this.settings = d
        if (d.bigscreen_per_page) this.big.perPage = Number(d.bigscreen_per_page)
        if (d.bigscreen_show_image !== undefined) this.bigConfig.showImage = d.bigscreen_show_image === '1'
        if (d.bigscreen_show_category !== undefined) this.bigConfig.showCategory = d.bigscreen_show_category === '1'
        if (d.bigscreen_show_ingredients !== undefined) this.bigConfig.showIngredients = d.bigscreen_show_ingredients === '1'
        if (d.bigscreen_show_audit !== undefined) this.bigConfig.showAudit = d.bigscreen_show_audit === '1'
        if (d.bigscreen_show_suggest !== undefined) this.bigConfig.showSuggest = d.bigscreen_show_suggest === '1'
        this.applyPeriodSettings(d)
        this.syncAutoPeriod()
        this.$nextTick(() => { this.noticeIdx = 0; this.startNoticeRotate(); this.fitNotice() })
      })
      this.tick()
      this.clockTimer = setInterval(this.tick, 1000)
      this.startAuto()
      window.addEventListener('keydown', this.onKey)
    },
    dateStr(d) { return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0') },
    todayStr() { return this.dateStr(new Date()) },
    mondayOf(dt) {
      if (!dt) return ''
      const d = new Date(dt + 'T00:00:00')
      const day = d.getDay()
      d.setDate(d.getDate() + (day === 0 ? -6 : 1 - day))
      return this.dateStr(d)
    },
    pubDayStyle(di) {
      const colors = ['#eef4fc', '#e9f1fb', '#f0f6fe', '#e7eff9', '#ecf3fb', '#eaf2fd', '#f1f7ff']
      return { background: colors[di % colors.length] }
    },
    // 同一天各餐使用与纸张相同的统一颜色
    dayColor(di) { return this.pubDayStyle(di).background },
    pubMealsStyle() { return { 'grid-template-columns': '1fr 1fr 1fr' } },
    // 某餐菜品数>7 时换为多列：>21 三列，>7 两列，否则单列
    mealCols(di, meal) {
      const n = (this.mealsData[di + '-' + meal] || []).length
      if (n > 6) return 2
      return 1
    },
    mealBg(di, meal) {
      const P = {
        '早餐': ['#eaf2fe', '#eef5fe', '#f2f7ff', '#e7f0fe', '#edf5ff', '#e9f2ff', '#eff6ff'],
        '中餐': ['#dbe9fb', '#d6e5fa', '#dcebfc', '#d3e3f9', '#d9e8fb', '#d8e6fa', '#d2e2f8'],
        '晚餐': ['#e8eef8', '#ebf1fa', '#e5ecf7', '#e9eff9', '#e6edf8', '#e7eef8', '#ecf2fa']
      }
      return (P[meal] || ['#f6f6f6'])[di % 7]
    },
    // 非用餐提示文字：每 10 秒轮播一条（仅大屏 + 非用餐时段 + 多条时启动）
    startNoticeRotate() {
      this.stopNoticeRotate()
      if (!this.isBigscreen || !this.isNoticeMode || this.noticeLines.length <= 1) return
      this._noticeTimer = setInterval(() => {
        if (!this.isNoticeMode) return
        this.noticeIdx = (this.noticeIdx + 1) % this.noticeLines.length
      }, 10000)
    },
    stopNoticeRotate() {
      if (this._noticeTimer) { clearInterval(this._noticeTimer); this._noticeTimer = null }
    },
    // 非用餐提示文字：整行不折行，按可用区域整体缩放，保证居中和字号协调
    fitNotice() {
      this.$nextTick(() => {
        const box = this.$refs.noticeBox
        const inner = this.$refs.noticeInner
        if (!box || !inner) return
        inner.style.transform = 'none'
        const availW = Math.max(1, box.clientWidth - 8)
        const availH = Math.max(1, box.clientHeight - 8)
        const w = Math.max(1, inner.scrollWidth)
        const h = Math.max(1, inner.scrollHeight)
        let s = Math.min(1, availW / w, availH / h)
        if (!isFinite(s) || s <= 0) s = 1
        inner.style.transform = 'scale(' + Math.max(0.3, s).toFixed(3) + ')'
      })
    },
    tick() {
      const now = new Date()
      this.nowTime = now.toLocaleTimeString('zh-CN', { hour12: false })
      this.nowMinute = now.getHours() * 60 + now.getMinutes()
      const ds = this.dateStr(now)
      if (this.isBigscreen && this.today !== ds) {
        // 跨天（含跨周）：刷新当天日期与所在周，重新按当天展示
        this.today = ds
        this.weekKey = ds
      }
      this.syncAutoPeriod()
    },
    // HH:mm -> 当日分钟数
    toMin(t) {
      if (!t) return -1
      const p = String(t).split(':')
      const h = parseInt(p[0], 10), mi = parseInt(p[1], 10)
      if (isNaN(h) || isNaN(mi)) return -1
      return h * 60 + mi
    },
    todayDayIndex() { const g = new Date().getDay(); return g === 0 ? 6 : g - 1 },
    applyPeriodSettings(d) {
      if (!d) return
      if (d.bigscreen_meal_period !== undefined) this.period.enabled = d.bigscreen_meal_period === '1'
      if (d.meal_breakfast_start) this.period.breakfastStart = d.meal_breakfast_start
      if (d.meal_breakfast_end) this.period.breakfastEnd = d.meal_breakfast_end
      if (d.meal_lunch_start) this.period.lunchStart = d.meal_lunch_start
      if (d.meal_lunch_end) this.period.lunchEnd = d.meal_lunch_end
      if (d.meal_dinner_start) this.period.dinnerStart = d.meal_dinner_start
      if (d.meal_dinner_end) this.period.dinnerEnd = d.meal_dinner_end
      if (d.bigscreen_notice_text) this.period.text = d.bigscreen_notice_text
    },
    // 按当前时间同步大屏的“天 + 餐次”；silent=true 时只同步不请求
    syncAutoPeriod(silent) {
      if (!this.isBigscreen || !this.period.enabled || !this.weekKey) return false
      const p = this.currentPeriod
      if (!p) return false
      const di = this.todayDayIndex()
      const changed = this.big.day !== di || this.big.meal !== p.meal || this.big.week !== this.effWeek
      this.big.day = di
      this.big.meal = p.meal
      if (changed && !silent) this.loadBig()
      return changed
    },
    validatePeriod() {
      for (const r of this.periodRows) {
        const s = this.period[r.startKey], e = this.period[r.endKey]
        if (!s || !e) { this.$message.warning('请填写完整的' + r.meal + '时间'); return false }
        if (this.toMin(e) <= this.toMin(s)) { this.$message.warning(r.meal + '结束时间需晚于开始时间'); return false }
      }
      const sorted = this.periodRows
        .map(r => ({ meal: r.meal, s: this.toMin(this.period[r.startKey]), e: this.toMin(this.period[r.endKey]) }))
        .sort((a, b) => a.s - b.s)
      for (let i = 1; i < sorted.length; i++) {
        if (sorted[i].s < sorted[i - 1].e) {
          this.$message.warning(sorted[i - 1].meal + '与' + sorted[i].meal + '的时间段重叠，请调整')
          return false
        }
      }
      return true
    },
    async loadBig() {
      if (!this.weekKey) return
      this.big.loaded = false
      try {
        const d = await api.get('/api/publicity/bigscreen', { weekKey: this.effWeek })
        this.allDays = d.days
        this.applyBigFilter()
        this.big.week = this.effWeek
        // 手动模式且当天无数据时，回退到本周第一个有菜的星期，避免大屏空白
        if (!this.big.initialized) {
          this.big.initialized = true
          if (!this.period.enabled && !this.big.data.length) {
            const first = this.allDays.find(x => x.meals.some(m => m.dishes.length))
            if (first) { this.big.day = first.day; this.applyBigFilter() }
          }
        }
      } finally {
        this.big.loaded = true
      }
    },
    applyBigFilter() {
      const day = this.allDays.find(x => x.day === this.big.day)
      let cards = []
      if (day) {
        day.meals.forEach(m => {
          if (this.big.meal !== '全部' && m.meal !== this.big.meal) return
          cards = cards.concat(m.dishes)
        })
      }
      this.big.data = cards
      this.big.page = 0
    },
    resetBig() { this.big.page = 0; this.startAuto() },
    togglePause() { this.big.paused = !this.big.paused; this.startAuto() },
    saveBigSettings() {
      if (!this.validatePeriod()) return
      api.put('/api/settings/all', {
        bigscreen_per_page: String(this.big.perPage),
        bigscreen_show_image: this.bigConfig.showImage ? '1' : '0',
        bigscreen_show_category: this.bigConfig.showCategory ? '1' : '0',
        bigscreen_show_ingredients: this.bigConfig.showIngredients ? '1' : '0',
        bigscreen_show_audit: this.bigConfig.showAudit ? '1' : '0',
        bigscreen_show_suggest: this.bigConfig.showSuggest ? '1' : '0',
        canteen_name: this.settings.canteen_name || '',
        bigscreen_meal_period: this.period.enabled ? '1' : '0',
        meal_breakfast_start: this.period.breakfastStart,
        meal_breakfast_end: this.period.breakfastEnd,
        meal_lunch_start: this.period.lunchStart,
        meal_lunch_end: this.period.lunchEnd,
        meal_dinner_start: this.period.dinnerStart,
        meal_dinner_end: this.period.dinnerEnd,
        bigscreen_notice_text: this.period.text || ''
      }).then(() => {
        this.bigSettings = false
        this.$message.success('设置已保存')
        this.syncAutoPeriod()
      }).catch(() => this.$message.error('保存失败'))
    },
    startAuto() {
      this.stopTimer()
      if (!this.big.paused) this.timer = setInterval(() => { this.big.page = (this.big.page + 1) % this.bigPages }, 6000)
    },
    stopTimer() { if (this.timer) { clearInterval(this.timer); this.timer = null } },
    stopClock() { if (this.clockTimer) { clearInterval(this.clockTimer); this.clockTimer = null } },
    bigNext() { this.big.page = (this.big.page + 1) % this.bigPages },
    bigPrev() { this.big.page = (this.big.page - 1 + this.bigPages) % this.bigPages },
    closeBig() { window.close(); this.isBigscreen = false },
    onKey(e) {
      if (e.key === 'Escape') this.closeBig()
      else if (e.key === ' ') { e.preventDefault(); this.big.paused = !this.big.paused; this.startAuto() }
      else if (e.key === 'ArrowRight') this.bigNext()
      else if (e.key === 'ArrowLeft') this.bigPrev()
    }
  }
}
</script>
<style scoped>
.pub-day { max-width: 1150px; margin: 0 auto 12px; padding: 14px 16px; border-radius: 10px; }
.pub-title { text-align: center; }
.pub-bigtitle { text-align: center; font-size: 22px; font-weight: 800; color: #16325a; margin: 8px 0 32px 0; letter-spacing: 2px; }
.pub-body { display: flex; gap: 12px; }
.pub-wrap { flex: 1; min-width: 0; }
.pub-mealheads { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; margin-bottom: 6px; font-weight: 700; text-align: center; }
.pub-mealhead { font-size: 16px; }
.pub-date { writing-mode: vertical-rl; text-orientation: mixed; font-weight: 700; color: #555; align-self: center; letter-spacing: 1px; padding: 4px 2px; }
.pub-meals { flex: 1; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
.pub-col { padding: 10px 12px; border-radius: 8px; min-width: 0; }
.pub-col h3 { margin: 0 0 4px 0; font-size: 16px; }
.pub-item { padding: 0; line-height: 1.4; break-inside: avoid; overflow-wrap: break-word; word-break: break-word; }
.pub-items { column-gap: 14px; }
.pub-item .flex { line-height: 1.4; flex-wrap: wrap; }
.pub-item .flex .pn { min-width: 0; overflow-wrap: anywhere; word-break: break-word; flex-shrink: 1; }
.pub-ing { font-size: 12px; line-height: 1.35; margin-top: 1px; color: #666; }
/* 打印：整周一周一张纸，自动适配，隐藏价格/配料，每天不同浅色背景 */
body.printing .top-nav, body.printing .page-card { display: none !important; }
@media print {
  @page { size: A4 portrait; margin: 3mm; }
  .top-nav, .page-card, .toolbar { display: none !important; }
  .page { width: 100%; max-width: 100%; padding: 0; margin: 0; }
  .pub-pair { page-break-inside: auto; break-inside: auto; margin: 0; }
  .pub-pair + .pub-pair { break-before: auto; page-break-before: auto; }
  .pub-day, .pub-body, .pub-meals, .pub-col, .pub-item { box-sizing: border-box; }
  .pub-day { width: 100%; max-width: 100%; margin: 0 0 1mm 0; padding: 1mm 1mm; border-radius: 2px; break-inside: avoid; }
  .pub-body { display: flex; width: 100%; gap: 1mm; }
  .pub-wrap { flex: 1 1 0%; min-width: 0; width: 0; transform: scale(0.9); transform-origin: top left; }
  .pub-mealheads { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 1mm; margin: 0 0 1px 0; }
  .pub-mealhead { font-size: 8.5px; }
  .pub-title { font-size: 14px; margin: 0 0 1px 0; text-align: center; }
  .pub-bigtitle { text-align: center; font-size: 16pt; font-weight: 800; color: #000; margin: 0 0 8mm 0; letter-spacing: 1px; }
  .pub-date { writing-mode: vertical-rl; text-orientation: mixed; font-size: 7px; margin: 0; font-weight: 700; color: #333; letter-spacing: 0.5px; max-width: 4mm; overflow: hidden; }
  .pub-meals { flex: 1 1 0%; min-width: 0; width: 100%; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 1mm; font-size: 8px; margin: 0; line-height: 1.2; }
  .pub-col { padding: 0.8mm 1.2mm; border-radius: 2px; min-width: 0; }
  .pub-col h3 { font-size: 8.5px; margin: 0 0 0.5px 0; }
  .pub-item { padding: 0; line-height: 1.2; break-inside: avoid; overflow-wrap: break-word; word-break: break-word; }
  .pub-item .flex { flex-wrap: wrap; }
  .pub-item .flex .pn { min-width: 0; overflow-wrap: anywhere; word-break: break-word; flex-shrink: 1; }
  .pub-items { column-gap: 1.5mm; }
  .pub-ing, .pub-price-audit, .pub-price-suggest { display: none !important; }
  .pub-empty { font-size: 8px; padding: 0; }
}
.big-full { padding: 14px; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; text-align: center; }
.big-fname { font-size: 26px; font-weight: 800; }
.big-fmeta { font-size: 14px; opacity: .9; margin-top: 6px; }
.big-fprice { font-size: 34px; font-weight: 800; color: #ffd166; margin-top: 8px; }
.big-card.big-empty { background: rgba(255,255,255,.08); border: 2px dashed rgba(255,255,255,.25); }
.pub-empty { color: #5c789c; font-size: 13px; padding: 8px 0; }

/* 顶部当前餐次标识 */
.big-period {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 14px; border-radius: 999px; font-size: 16px; font-weight: 700;
  color: #edf5ff; background: rgba(255,255,255,.18);
  border: 1px solid rgba(255,255,255,.45); white-space: nowrap;
}
.big-period.on { background: rgba(255,255,255,.28); box-shadow: 0 0 0 2px rgba(170,210,255,.45) inset; }
.big-period.off { background: rgba(8,32,68,.30); border-color: rgba(255,255,255,.3); }
.big-period.manual { opacity: .75; }

/* 非用餐时段居中文字 */
.big-notice { flex: 1; display: flex; align-items: center; justify-content: center; padding: 2vh 4vw; min-height: 0; overflow: hidden; }
.big-notice-inner { width: 100%; max-width: 1600px; text-align: center; transform-origin: center center; }
.notice-rule { width: 46vw; max-width: 620px; height: 3px; margin: 0 auto 4vh; border-radius: 3px;
  background: linear-gradient(90deg, rgba(255,255,255,0), rgba(200,225,255,.95), rgba(255,255,255,0)); }
.notice-rule.small { width: 22vw; max-width: 300px; margin: 4vh auto 0; height: 2px; opacity: .75; }
.notice-text {
  margin: 0; white-space: pre; word-break: keep-all;
  font-size: clamp(26px, 3vw, 72px); line-height: 1.58; font-weight: 800;
  letter-spacing: 2px; color: #f3f9ff;
  text-shadow: 0 3px 10px rgba(8,32,72,.40), 0 0 40px rgba(150,200,255,.30);
  animation: noticeIn .8s ease both;
}
@keyframes noticeIn {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}
.notice-text.notice-empty { font-weight: 700; color: #e6f0ff; opacity: .92; }

/* 设置面板 */
.period-hint { font-size: 12px; color: #5c789c; }
.period-now { font-weight: 700; color: var(--main-dark, #1c4f9c); }
</style>
<style>
/* 全局（非 scoped）：打印时隐藏顶部一级菜单导航与页面工具栏。
   scoped 样式带组件作用域标记，无法命中 App 组件的 .top-nav，故单独全局声明。 */
@media print {
  .top-nav, .page-card, .toolbar, .nav-tab { display: none !important; }
  /* 仅"导出PDF"时整体缩到0.98；普通打印保持原样 */
  body.pdf-export .page { transform: scale(0.98); transform-origin: top left; width: 102.04% !important; }
  .pub-bigtitle {
    display: block !important;
    width: 100% !important;
    text-align: center !important;
    margin: 0 0 8mm 0 !important;
    font-size: 16pt !important;
    font-weight: 800 !important;
    color: #000 !important;
    letter-spacing: 1px !important;
  }
}
</style>
