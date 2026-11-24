<template>
  <div class="page">
    <!-- 顶部工具条 -->
    <div class="page-card">
      <div class="toolbar">
        <el-select v-model="weekKey" filterable placeholder="选择周(周一日期)" style="width:180px" @change="onWeekChange">
          <el-option v-for="w in weekKeys" :key="w" :label="w + (w===weekKey?'':'')" :value="w" />
        </el-select>
        <el-button @click="newWeek">本周</el-button>
        <el-button @click="goNextWeek">下一周</el-button>
        <span v-if="weekNum" class="text-muted">第 {{ weekNum.week }} 周<span v-if="weekNum.mode==='1'">（{{ weekNum.semester===1?'第一':'第二' }}学期）</span><span v-else>（公历）</span></span>
        <div class="spacer" style="flex:1"></div>
        <el-dropdown @command="onCmd" style="margin-right:8px">
          <el-button type="primary">操作菜单<i class="el-icon-arrow-down el-icon--right"></i></el-button>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="saveWeek">💾 保存本周</el-dropdown-item>
            <el-dropdown-item command="calcPortions">🔢 重新计算份数</el-dropdown-item>
            <el-dropdown-item command="autoArrange" divided>🤖 自动编排食谱</el-dropdown-item>
            <el-dropdown-item command="export" divided>⬇ 导出周食谱(Excel)</el-dropdown-item>
            <el-dropdown-item command="import">⬆ 导入周食谱(Excel)</el-dropdown-item>
            <el-dropdown-item command="saveTemplate" divided>📋 保存为模板</el-dropdown-item>
            <el-dropdown-item command="loadTemplate">📂 加载模板</el-dropdown-item>
            <el-dropdown-item command="print">🖨 打印</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
        <el-button @click="clearWeek">清空本周</el-button>
        <el-button type="primary" plain @click="openCopy">复制菜品</el-button>
      </div>
      <!-- 打印设置 -->
      <div v-if="showPrintSettings" class="flex" style="flex-wrap:wrap;gap:14px;border-top:1px dashed #d5e3f6;padding-top:10px">
        <span class="flex"><label>列宽(%)</label><el-input-number v-model="print.colWidth" :min="8" :max="40" :step="1" style="width:110px" /></span>
        <span class="flex"><label>选择天</label>
          <el-checkbox v-for="(n,i) in dayNames" :key="i" v-model="print.days[i]" :label="n" />
        </span>
        <span class="flex"><label>餐次</label>
          <el-checkbox v-model="print.breakfast" label="早餐" /><el-checkbox v-model="print.lunch" label="中餐" /><el-checkbox v-model="print.dinner" label="晚餐" />
        </span>
        <el-button type="primary" @click="doPrint">打印</el-button>
        <el-button @click="showPrintSettings=false">关闭</el-button>
      </div>
    </div>

    <!-- 周食谱主体 -->
    <div class="week-scroll" ref="weekScroll">
      <div v-for="di in visibleDays" :key="di" class="day-block">
        <div class="day-head">
          <div class="day-title">{{ dayNames[di] }}</div>
          <div class="day-actions" v-if="!printActive">
            <el-button size="mini" type="text" @click="clearDay(di)">清空当天</el-button>
          </div>
        </div>
        <div class="meal-row">
          <div v-for="meal in visibleMeals(di)" :key="meal" class="meal-col" :class="'meal-'+meal" :style="printActive ? '' : ''">
            <div class="meal-head">
              <span>{{ meal }}</span>
              <div class="flex" v-if="!printActive">
                <el-input-number v-model="peopleMap[di+'-'+meal]" :min="0" :max="99999" :controls="false" placeholder="人数" class="people-input" @change="v => onPeople(di, meal, v)" />
                <span class="text-muted">人</span>
                <el-button size="mini" type="text" @click="openPick(di, meal)">+添加菜</el-button>
              </div>
            </div>
            <div class="meal-dishes">
              <div v-for="(r,idx) in dayMeals(di, meal)" :key="r.id || idx" class="dish-line">
                <span class="dish-idx" :class="{missing: dishMissing(r)}">{{ idx+1 }}</span>
                <span class="dish-name" :class="[dupClass(r), {missing: dishMissing(r)}]">{{ r.dishName }}<span v-if="dupCount(r.dishName)>1" class="dup-tag" :class="dupClass(r)">第{{ dupCount(r.dishName) }}次</span></span>
                <el-input-number v-model="r.portions" :min="0" :controls="false" size="mini" class="portion-input" />
                <span class="text-muted">份</span>
                <el-button size="mini" type="text" style="color:#f56c6c" @click="removeDish(r)">✕</el-button>
              </div>
              <div v-if="!dayMeals(di, meal).length" class="empty-meal">—</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 选菜对话框 -->
    <el-dialog :title="'选择菜品 - ' + dayNames[pickDay] + ' ' + pickMeal" :visible.sync="pickVisible" width="620px">
      <div class="toolbar">
        <el-select v-model="pickCat" placeholder="按类别筛选" clearable style="width:140px">
          <el-option v-for="c in catOptions" :key="c" :label="c" :value="c" />
        </el-select>
        <el-input v-model="pickKeyword" placeholder="搜索菜品名称(模糊)" clearable style="width:200px" @keyup.enter.native="blurSearch" />
        <el-button type="text" @click="checked = []">清空勾选</el-button>
        <span class="text-muted">已选 {{ checked.length }} 个</span>
      </div>
      <div class="pick-list">
        <el-checkbox-group v-model="checked">
          <el-checkbox v-for="d in filteredDishes" :key="d.id" :label="d.id" style="width:45%;margin:6px 2%">
            {{ d.name }}（{{ d.category || '未分类' }}）
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <div slot="footer">
        <el-button @click="pickVisible=false">取消</el-button>
        <el-button type="primary" @click="confirmPick">添加选中菜品</el-button>
      </div>
    </el-dialog>

    <!-- 模板对话框 -->
    <el-dialog title="周食谱模板" :visible.sync="tplVisible" width="560px">
      <div class="toolbar">
        <el-input v-model="tplName" placeholder="模板名称(默认当周周一日期)" style="width:220px" />
        <el-button type="primary" @click="saveTemplate">保存当前为模板</el-button>
        <el-input v-model="tplSearch" placeholder="搜索模板" clearable style="width:140px" />
      </div>
      <el-table :data="templates" border size="mini">
        <el-table-column prop="name" label="模板名称" />
        <el-table-column prop="createdAt" label="创建时间" width="150" />
        <el-table-column label="操作" width="180">
          <template slot-scope="{row}">
            <el-button type="text" @click="loadTemplate(row)">加载</el-button>
            <el-button type="text" @click="renameTpl(row)">重命名</el-button>
            <el-button type="text" style="color:#f56c6c" @click="delTpl(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 隐藏导入 -->
    <el-dialog title="导入周食谱" :visible.sync="importVisible" width="420px">
      <el-upload drag :show-file-list="true" :http-request="importExcel" accept=".xlsx" :limit="1" style="width:100%">
        <i class="el-icon-upload"></i><div class="el-upload__text">将 Excel 拖到此处，或<em>点击上传</em></div>
      </el-upload>
    </el-dialog>

    <!-- 自动编排对话框 -->
    <el-dialog title="自动编排食谱" :visible.sync="arrVisible" width="820px">      <div class="flex" style="margin-bottom:10px;gap:20px">
        <span class="flex"><label>允许重复</label><el-switch v-model="arr.allowRepeat" /></span>
        <span class="flex" v-if="arr.allowRepeat"><label>最小重复间隔</label><el-input-number v-model="arr.minRepeatGap" :min="0" style="width:110px" /> 天</span>
      </div>
      <el-table :data="arr.meals" border size="small">
        <el-table-column label="日期" width="90"><template slot-scope="{row}">{{ dayNames[row.dayIndex] }}</template></el-table-column>
        <el-table-column label="餐次" width="80"><template slot-scope="{row}">{{ row.mealType }}</template></el-table-column>
        <el-table-column label="计划人数"><template slot-scope="{row}"><el-input-number v-model="row.peopleCount" :controls="false" :min="0" style="width:100px" /></template></el-table-column>
        <el-table-column label="每人打菜数"><template slot-scope="{row}"><el-input-number v-model="row.perPersonDishes" :min="1" style="width:100px" /></template></el-table-column>
        <el-table-column label="荤菜数"><template slot-scope="{row}"><el-input-number v-model="row.meatCount" :min="0" style="width:80px" /></template></el-table-column>
        <el-table-column label="素菜数"><template slot-scope="{row}"><el-input-number v-model="row.vegCount" :min="0" style="width:80px" /></template></el-table-column>
        <el-table-column label="主食数"><template slot-scope="{row}"><el-input-number v-model="row.stapleCount" :min="0" style="width:80px" /></template></el-table-column>
      </el-table>
      <div style="margin-top:10px">
        <el-button type="primary" @click="doArrange">生成预览</el-button>
        <el-button @click="newGroup">换一组</el-button>
        <el-button type="primary" @click="applyArrange" :disabled="!arrPreview.length">确认应用</el-button>
      </div>
      <div v-if="arrPreview.length" class="text-muted" style="margin-top:8px">预览：将生成 {{ arrPreview.length }} 条记录，确认后替换选中天</div>
    </el-dialog>

    <el-dialog title="复制菜品" :visible.sync="copyVisible" width="480px">
      <el-radio-group v-model="copyMode" style="margin-bottom:12px" @change="resetCopyDays">
        <el-radio-button label="day">本周内复制</el-radio-button>
        <el-radio-button label="lastWeek">复制上周整周</el-radio-button>
        <el-radio-button label="nextWeek">复制到下一周</el-radio-button>
      </el-radio-group>

      <div v-if="copyMode === 'day'" style="line-height:2.4">
        将
        <el-select v-model="copySource" placeholder="来源天(周几)" clearable style="width:140px">
          <el-option v-for="(n,i) in dayNames" :key="i" :label="n" :value="i" />
        </el-select>
        的菜品复制到
        <el-select v-model="copyTarget" placeholder="目标天(周几)" clearable style="width:140px">
          <el-option v-for="(n,i) in dayNames" :key="i" :label="n" :value="i" :disabled="i===copySource" />
        </el-select>
        <el-button size="mini" type="text" icon="el-icon-refresh-left" @click="resetCopyDays">清空选择</el-button>
        <div class="text-muted" style="font-size:12px">点选择框右侧的 ×，或「清空选择」，即可取消已选的天。将覆盖目标天当天现有食谱。</div>
      </div>

      <div v-else-if="copyMode === 'lastWeek'" style="line-height:2">
        <p>将把<strong>上周（{{ lastWeekKey }}）</strong>的整周食谱（共 {{ lastWeekCount }} 条记录）复制到本周（{{ effWeek }}）。</p>
        <p class="text-muted" style="font-size:12px">操作将<strong style="color:#1c4f9c">覆盖本周现有全部食谱</strong>，人数/份数一并复制。</p>
      </div>

      <div v-else style="line-height:2">
        <p>将把<strong>本周（{{ effWeek }}）</strong>的整周食谱（共 {{ thisWeekCount }} 条记录）复制到下一周（{{ nextWeekKey }}）。</p>
        <p class="text-muted" style="font-size:12px">操作将<strong style="color:#1c4f9c">覆盖下一周现有全部食谱</strong>，人数/份数一并复制。</p>
      </div>

      <span slot="footer">
        <el-button @click="copyVisible=false">取消</el-button>
        <el-button type="primary" @click="doCopy">复制</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'WeeklyRecipe',
  data() {
    return {
      dayNames: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      meals: ['早餐', '中餐', '晚餐'],
      weekKey: '', weekKeys: [], weekNum: null,
      dataMap: {},          // key "di-meal" -> [recipes]
      peopleMap: {},        // key "di-meal" -> people
      dishes: [], catOptions: ['鲜荤', '冻荤', '汤食', '素菜', '主食', '早餐'],
      pickVisible: false, pickDay: 0, pickMeal: '早餐', pickCat: '', pickKeyword: '', checked: [],
      copyVisible: false, copyTarget: null, copySource: null,
      copyMode: 'day', lastWeekCount: 0, thisWeekCount: 0,
      autoWeek: true, weekTimer: null,
      tplVisible: false, tplName: '', tplSearch: '', templates: [],
      arrVisible: false, arr: { allowRepeat: false, minRepeatGap: 0, meals: [] }, arrPreview: [],
      showPrintSettings: false,
      printActive: false,
      print: { colWidth: 30, days: [true, true, true, true, true, true, true], breakfast: true, lunch: true, dinner: true },
      importVisible: false
    }
  },
  computed: {
    effWeek() { return this.mondayOf(this.weekKey) },
    lastWeekKey() {
      if (!this.effWeek) return ''
      const d = new Date(this.effWeek + 'T00:00:00')
      d.setDate(d.getDate() - 7)
      return this.dateStr(d)
    },
    nextWeekKey() {
      if (!this.effWeek) return ''
      const d = new Date(this.effWeek + 'T00:00:00')
      d.setDate(d.getDate() + 7)
      return this.dateStr(d)
    },
    filteredDishes() {
      let list = this.dishes
      if (this.pickCat) list = list.filter(d => d.category === this.pickCat)
      const kw = (this.pickKeyword || '').trim().toLowerCase()
      if (kw) list = list.filter(d => (d.name || '').toLowerCase().indexOf(kw) !== -1)
      return list
    },
    visibleDays() {
      if (!this.printActive) return [0, 1, 2, 3, 4, 5, 6]
      return this.print.days.map((v, i) => v ? i : -1).filter(i => i >= 0)
    }
  },
  created() {
    this.newWeek()
    this.startWeekWatch()
    api.get('/api/dish/all').then(d => this.dishes = d)
    this.loadTemplates()
  },
  beforeDestroy() {
    if (this.weekTimer) { clearInterval(this.weekTimer); this.weekTimer = null }
  },
  methods: {
    /** 停留在"本周"视图时，跨周自动切换到新的一周并加载该周食谱 */
    startWeekWatch() {
      if (this.weekTimer) clearInterval(this.weekTimer)
      this.weekTimer = setInterval(() => {
        if (!this.autoWeek) return
        const monday = this.mondayOf(this.todayStr())
        if (monday && monday !== this.effWeek) {
          this.weekKey = this.todayStr()
          this.newWeek()
          this.$message.success('已进入新的一周，自动展示 ' + this.effWeek + ' 的食谱')
        }
      }, 60000)
    },
    onWeekChange() {
      this.autoWeek = false
      this.load()
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
    visibleMeals(di) {
      if (!this.printActive) return this.meals
      const arr = []
      if (this.print.breakfast) arr.push('早餐')
      if (this.print.lunch) arr.push('中餐')
      if (this.print.dinner) arr.push('晚餐')
      return arr
    },
    async newWeek() {
      this.autoWeek = true
      this.weekKey = this.todayStr()
      await this.refreshWeekKeys()
      this.load()
    },
    /** 周下拉框始终提供 上周/本周/下周 三个周一日期，即使该周还没有食谱数据也能选中并录入 */
    async refreshWeekKeys() {
      const keys = (await api.get('/api/recipe/weekKeys')) || []
      const thisMonday = this.mondayOf(this.todayStr())
      const extra = [this.addDays(thisMonday, -7), thisMonday, this.addDays(thisMonday, 7)]
      const merged = Array.from(new Set(keys.concat(extra).filter(Boolean))).sort()
      this.weekKeys = merged
    },
    addDays(dateStr, days) {
      if (!dateStr) return ''
      const d = new Date(dateStr + 'T00:00:00')
      d.setDate(d.getDate() + days)
      return this.dateStr(d)
    },
    /** 切到下一周（以当前查看的周一为基准往后一周） */
    goNextWeek() {
      this.autoWeek = false
      const key = this.addDays(this.effWeek || this.mondayOf(this.todayStr()), 7)
      if (!this.weekKeys.includes(key)) this.weekKeys = this.weekKeys.concat([key]).sort()
      this.weekKey = key
      this.load()
    },
    async load() {
      const list = await api.get('/api/recipe/list', { weekKey: this.effWeek })
      this.dataMap = {}; this.peopleMap = {}
      list.forEach(r => {
        const k = r.dayIndex + '-' + r.mealType
        if (!this.dataMap[k]) this.dataMap[k] = []
        this.dataMap[k].push(r)
        this.peopleMap[k] = r.peopleCount
      })
      this.weekNum = await api.get('/api/recipe/weekNumber', { weekKey: this.effWeek })
    },
    dayMeals(di, meal) { return this.dataMap[di + '-' + meal] || [] },
    dupCount(name) {
      let c = 0
      for (const k in this.dataMap) this.dataMap[k].forEach(r => { if (r.dishName === name) c++ })
      return c
    },
    dupClass(r) {
      const c = this.dupCount(r.dishName)
      return c >= 3 ? 'dup-3' : (c === 2 ? 'dup' : '')
    },
    dishMissing(r) {
      if (!r || !r.dishName) return true
      return !this.dishes.some(d => d.name === r.dishName)
    },
    onPeople(di, meal, v) {
      const k = di + '-' + meal
      this.dayMeals(di, meal).forEach(r => { r.peopleCount = v; this.peopleMap[k] = v })
    },
    openPick(di, meal) { this.pickDay = di; this.pickMeal = meal; this.checked = []; this.pickCat = ''; this.pickKeyword = ''; this.pickVisible = true },
    blurSearch() { document.activeElement && document.activeElement.blur() },
    confirmPick() {
      const ids = this.checked.slice()
      const k = this.pickDay + '-' + this.pickMeal
      const map = Object.assign({}, this.dataMap)
      if (!map[k]) map[k] = []
      const people = this.peopleMap[k] || 0
      ids.forEach(id => {
        const d = this.dishes.find(x => x.id === id)
        if (!d) return
        const exist = map[k].find(r => r.dishId === id)
        if (exist) { this.$message.warning('已存在该菜品'); return }
        map[k] = map[k].concat([{ id: null, dayIndex: this.pickDay, mealType: this.pickMeal, dishId: d.id, dishName: d.name, category: d.category, peopleCount: people, portions: 0, seq: map[k].length }])
      })
      this.dataMap = map
      this.pickVisible = false
    },
    removeDish(r) {
      this.$confirm('移除此菜品？', '提示', { type: 'warning' }).then(() => {
        const k = r.dayIndex + '-' + r.mealType
        const map = Object.assign({}, this.dataMap)
        map[k] = (map[k] || []).filter(x => x !== r)
        this.dataMap = map
      }).catch(() => {})
    },
    async calcPortions() {
      const all = []
      for (const k in this.dataMap) all.push(...this.dataMap[k])
      const result = await api.post('/api/recipe/calcPortions', all)
      this.dataMap = {}
      result.forEach(r => {
        const key = r.dayIndex + '-' + r.mealType
        if (!this.dataMap[key]) this.dataMap[key] = []
        this.dataMap[key].push(r)
        this.peopleMap[key] = r.peopleCount
      })
      this.$message.success('份数已计算')
    },
    saveWeek() {
      const all = []
      for (const k in this.dataMap) all.push(...this.dataMap[k])
      api.post('/api/recipe/save?weekKey=' + this.effWeek, all).then(() => {
        this.$message.success('已保存 ' + this.effWeek + ' 起这一周的食谱')
        this.refreshWeekKeys()
        this.load()
      })
    },
    clearWeek() { this.$confirm('清空本周全部食谱？', '提示', { type: 'warning' }).then(() => { this.dataMap = {}; this.peopleMap = {}; this.saveWeek() }).catch(() => {}) },
    clearDay(di) { this.$confirm('清空' + this.dayNames[di] + '？', '提示', { type: 'warning' }).then(() => { for (const m of this.meals) this.dataMap[di + '-' + m] = []; this.saveWeek() }).catch(() => {}) },
    openCopy() {
      this.copyVisible = true
      this.copyMode = 'day'
      this.copySource = null
      this.copyTarget = null
      // 查询上周记录数
      api.get('/api/recipe/list', { weekKey: this.lastWeekKey }).then(list => {
        this.lastWeekCount = (list || []).length
      }).catch(() => { this.lastWeekCount = 0 })
      // 查询本周记录数（供"复制到下一周"使用）
      api.get('/api/recipe/list', { weekKey: this.effWeek }).then(list => {
        this.thisWeekCount = (list || []).length
      }).catch(() => { this.thisWeekCount = 0 })
    },
    resetCopyDays() { this.copySource = null; this.copyTarget = null },
    doCopy() {
      if (this.copyMode === 'nextWeek') {
        if (!this.thisWeekCount) { this.$message.warning('本周（' + this.effWeek + '）尚无食谱数据'); return }
        this.$confirm('将本周（' + this.effWeek + '）整周食谱复制到下一周（' + this.nextWeekKey + '）？将覆盖下一周全部现有食谱。', '提示', { type: 'warning' }).then(() => {
          api.post('/api/recipe/copyToNextWeek', null, { params: { weekKey: this.effWeek } }).then(() => {
            this.$message.success('已复制 ' + this.thisWeekCount + ' 条记录到下一周（' + this.nextWeekKey + '）')
            this.copyVisible = false
            api.get('/api/recipe/weekKeys').then(keys => {
              this.weekKeys = keys || []
              if (!this.weekKeys.includes(this.effWeek)) this.weekKeys.unshift(this.effWeek)
            })
            this.load()
          })
        }).catch(() => {})
        return
      }
      if (this.copyMode === 'lastWeek') {
        if (!this.lastWeekCount) { this.$message.warning('上周（' + this.lastWeekKey + '）尚无食谱数据'); return }
        this.$confirm('将上周（' + this.lastWeekKey + '）整周食谱复制到本周（' + this.effWeek + '）？将覆盖本周全部现有食谱。', '提示', { type: 'warning' }).then(() => {
          api.post('/api/recipe/copyLastWeek', null, { params: { weekKey: this.effWeek } }).then(() => {
            this.$message.success('已从上周复制 ' + this.lastWeekCount + ' 条记录'); this.copyVisible = false; this.load()
          })
        }).catch(() => {})
        return
      }
      if (this.copySource === null || this.copySource === undefined || this.copySource === '' ||
          this.copyTarget === null || this.copyTarget === undefined || this.copyTarget === '') { this.$message.warning('请选择来源天和目标天'); return }
      if (this.copySource === this.copyTarget) { this.$message.warning('来源天与目标天不能相同'); return }
      const cur = this.copyTarget, src = this.copySource
      this.$confirm('将' + this.dayNames[src] + '的菜品复制到' + this.dayNames[cur] + '？将覆盖' + this.dayNames[cur] + '当天现有食谱。', '提示', { type: 'warning' }).then(() => {
        api.post('/api/recipe/copyDay', null, { params: { weekKey: this.effWeek, currentDay: cur, sourceDay: src } }).then(() => {
          this.$message.success('复制完成'); this.copyVisible = false; this.load()
        })
      }).catch(() => {})
    },
    onCmd(cmd) {
      if (cmd === 'saveWeek') this.saveWeek()
      else if (cmd === 'calcPortions') this.calcPortions()
      else if (cmd === 'autoArrange') this.openArrange()
      else if (cmd === 'export') this.exportExcel()
      else if (cmd === 'import') this.importVisible = true
      else if (cmd === 'saveTemplate') this.openSaveTemplate()
      else if (cmd === 'loadTemplate') this.openTemplates()
      else if (cmd === 'print') this.showPrintSettings = !this.showPrintSettings
    },
    async exportExcel() { const r = await api.blob('/api/recipe/export', { weekKey: this.effWeek }); this.$download(r.data, '周食谱-' + this.effWeek + '.xlsx') },
    async importExcel({ file }) { const fd = new FormData(); fd.append('file', file); fd.append('weekKey', this.effWeek); const r = await api.upload('/api/recipe/import', fd); this.$message.success('导入成功 ' + r.ok + ' 条'); this.importVisible = false; this.load() },
    // 模板
    openSaveTemplate() { this.$prompt('模板名称（默认当周周一日期）', '保存为模板', { inputValue: this.effWeek }).then(({ value }) => { api.post('/api/recipe/template?weekKey=' + this.effWeek + '&name=' + encodeURIComponent(value || this.effWeek)).then(() => { this.$message.success('模板已保存'); this.loadTemplates() }) }).catch(() => {}) },
    openTemplates() { this.tplVisible = true; this.loadTemplates() },
    async loadTemplates() { this.templates = await api.get('/api/recipe/templates', { keyword: this.tplSearch || undefined }) },
    loadTemplate(row) { this.$confirm('加载模板「' + row.name + '」到本周？将替换本周内容。', '提示', { type: 'warning' }).then(() => api.post('/api/recipe/loadTemplate?id=' + row.id + '&weekKey=' + this.effWeek).then(() => { this.$message.success('已加载'); this.tplVisible = false; this.load() })).catch(() => {}) },
    renameTpl(row) { this.$prompt('新名称', '重命名模板', { inputValue: row.name }).then(({ value }) => api.put('/api/recipe/template/rename?id=' + row.id + '&name=' + encodeURIComponent(value)).then(() => { this.$message.success('已重命名'); this.loadTemplates() })).catch(() => {}) },
    delTpl(row) { this.$confirm('删除模板「' + row.name + '」？', '提示', { type: 'warning' }).then(() => api.del('/api/recipe/template/' + row.id).then(() => { this.$message.success('已删除'); this.loadTemplates() })).catch(() => {}) },
    // 自动编排
    openArrange() {
      const meals = []
      for (let di = 0; di < 7; di++) for (const m of this.meals) {
        meals.push({ dayIndex: di, mealType: m, peopleCount: 0, perPersonDishes: 2, meatCount: 1, vegCount: 1, stapleCount: 1 })
      }
      this.arr = { allowRepeat: false, minRepeatGap: 0, meals }
      this.arrPreview = []
      this.arrVisible = true
    },
    async doArrange() { this.arrPreview = await api.post('/api/recipe/autoArrange', this.arr) },
    newGroup() { this.doArrange() },
    applyArrange() {
      this.$confirm('应用自动编排结果？将覆盖对应天/餐的食谱。', '提示', { type: 'warning' }).then(() => {
        const all = []
        for (const k in this.dataMap) all.push(...this.dataMap[k])
        // 移除自动编排涉及的天/餐的旧数据
        const covered = new Set(this.arrPreview.map(r => r.dayIndex + '-' + r.mealType))
        const keep = all.filter(r => !covered.has(r.dayIndex + '-' + r.mealType))
        const merged = keep.concat(this.arrPreview)
        api.post('/api/recipe/save?weekKey=' + this.effWeek, merged).then(() => { this.$message.success('已应用'); this.arrVisible = false; this.load() })
      }).catch(() => {})
    },
    doPrint() {
      this.printActive = true
      this.$nextTick(() => {
        setTimeout(() => { window.print(); this.printActive = false }, 200)
      })
    }
  }
}
</script>
<style scoped>
.week-scroll { max-height: calc(100vh - 200px); overflow-y: auto; }
.pick-list { max-height: 400px; overflow-y: auto; margin-top: 10px; padding-right: 6px; }
.day-block { margin-bottom: 12px; }
.day-head {
  position: sticky; top: 0; z-index: 10;
  background: linear-gradient(90deg, #d7e5f8, #e6f0fd);
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 12px; border-radius: 8px 8px 0 0;
  border: 1px solid #d5e3f6;
}
.day-title { font-weight: 800; color: #1c3f73; font-size: 15px; }
.meal-row { display: grid; grid-template-columns: 5fr 7fr 5fr; gap: 8px; }
.meal-col { border: 1px solid #d5e3f6; border-top: none; padding: 8px; background: #fff; }
.meal-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.meal-head > span:first-child { font-weight: 700; color: #1e56ab; }
.people-input { width: 90px; }
.dish-line { display: flex; align-items: center; gap: 6px; padding: 3px 0; border-bottom: 1px dashed #e0ebf9; }
.dish-idx {
  display: inline-flex; align-items: center; justify-content: center;
  width: 20px; height: 20px; border-radius: 50%;
  background: #1c4f9c; color: #fff; font-size: 12px; font-weight: 700;
  flex-shrink: 0;
}
.dish-idx.missing { background: #000; }
.dish-name.missing { color: #000; font-weight: 700; }
.dish-name { flex: 1; font-size: 13px; }
.portion-input { width: 64px; }
.empty-meal { color: #a8bfd8; text-align: center; padding: 8px; }
.dup { color: #e6a23c; }
.dup-3 { color: #f56c6c; }
.dup-tag { font-size: 10px; margin-left: 4px; border: 1px solid currentColor; border-radius: 8px; padding: 0 4px; }
@media print {
  .top-nav, .page-card { display: none !important; }
  .week-scroll { max-height: none; overflow: visible; }
  .day-head { position: static; }
  .meal-row { display: grid; grid-template-columns: repeat(3, 1fr) !important; }
  .people-input, .portion-input, .day-actions { display: none !important; }
}
</style>
