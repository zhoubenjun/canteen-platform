<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <el-select v-model="weekKey" placeholder="选择周" style="width:170px" @change="refresh">
          <el-option v-for="w in weekKeys" :key="w" :label="w" :value="w" />
        </el-select>
        <span class="flex" style="margin-left:6px"><label>汇总星期</label>
          <el-checkbox v-for="(n,i) in dayNames" :key="i" v-model="days[i]" :label="n" />
          <el-button type="text" @click="toggleAllDays">整周/取消</el-button>
        </span>
        <el-radio-group v-model="mealType" @change="refresh">
          <el-radio-button label="全部">合并汇总</el-radio-button>
          <el-radio-button label="早餐">早餐</el-radio-button>
          <el-radio-button label="中餐">中餐</el-radio-button>
          <el-radio-button label="晚餐">晚餐</el-radio-button>
        </el-radio-group>
        <div class="spacer" style="flex:1"></div>
        <el-button type="primary" @click="refresh">汇总</el-button>
        <el-button type="primary" @click="savePlan">保存采购计划</el-button>
        <el-button type="primary" plain @click="exportPlan" :disabled="!(currentPlan || viewPlan)">导出清单</el-button>
        <el-select v-model="viewPlan" placeholder="历史采购计划" clearable style="width:180px" @change="loadPlan">
          <el-option v-for="p in plans" :key="p" :label="p" :value="p" />
        </el-select>
      </div>
    </div>

    <div class="page-card">
      <div class="flex" style="justify-content:space-between;margin-bottom:10px">
        <h3 style="margin:0;color:#1c3f73">采购汇总<span class="text-muted" v-if="mealType!=='全部'">（{{ mealType }}）</span></h3>
        <div>采购总金额：<b style="color:#2f6fd0;font-size:20px">¥{{ totalAmount }}</b></div>
      </div>
      <!-- 按供应商分组 -->
      <div v-for="grp in groups" :key="grp.supplier" class="supplier-block">
        <div class="supplier-head">{{ grp.supplier || '未指定供应商' }}</div>
        <el-table :data="grp.items" border size="small" stripe>
          <el-table-column prop="ingredientName" label="食材" min-width="80" />
          <el-table-column prop="categoryName" label="类别" min-width="120" />
          <el-table-column prop="spec" label="规格" min-width="110" />
          <el-table-column label="单价（元/KG）" min-width="105"><template slot-scope="{row}">¥{{ money(row.price) }}</template></el-table-column>
          <el-table-column label="总重量（KG）" min-width="100">
            <template slot-scope="{row}"><b>{{ row.totalWeight }}</b></template>
          </el-table-column>
          <el-table-column label="数量" min-width="105">
            <template slot-scope="{row}"><b>{{ qtyText(row) }}</b></template>
          </el-table-column>
          <el-table-column label="金额（元）" min-width="100"><template slot-scope="{row}"><b style="color:#2f6fd0">¥{{ money(row.amount) }}</b></template></el-table-column>
        </el-table>
        <div class="supplier-total">小计：¥{{ grp.total }}</div>
      </div>
      <div v-if="!rows.length" class="text-muted" style="text-align:center;padding:30px">暂无汇总数据（请先在本周食谱中排餐）</div>
    </div>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'PurchaseSummary',
  data() {
    return {
      dayNames: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      weekKey: '', weekKeys: [],
      days: [true, true, true, true, true, true, true],
      mealType: '全部',
      rows: [], totalAmount: '0.00', groups: [],
      plans: [], viewPlan: '', currentPlan: ''
    }
  },
  computed: {
    // 选择器显示今天日期，数据按今天所在周（周一）查询
    effWeek() { return this.mondayOf(this.weekKey) }
  },
  created() {
    this.init()
    api.get('/api/purchase/plans').then(d => this.plans = d)
  },
  methods: {
    dateStr(d) { return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0') },
    todayStr() { return this.dateStr(new Date()) },
    mondayOf(dt) {
      if (!dt) return ''
      const d = new Date(dt + 'T00:00:00')
      const day = d.getDay()
      d.setDate(d.getDate() + (day === 0 ? -6 : 1 - day))
      return this.dateStr(d)
    },
    async init() {
      this.weekKey = this.todayStr()
      this.weekKeys = await api.get('/api/recipe/weekKeys')
      this.refresh()
    },
    async refresh() {
      const selectedDays = this.days.map((v, i) => v ? i : -1).filter(i => i >= 0)
      const res = await api.get('/api/purchase/summary', { weekKey: this.effWeek, days: selectedDays.length ? selectedDays.join(',') : undefined, mealType: this.mealType })
      this.rows = res.rows
      this.totalAmount = res.totalAmount
      // 按供应商分组
      const map = {}
      res.rows.forEach(r => {
        const key = r.supplierName || '未指定供应商'
        if (!map[key]) map[key] = { supplier: key, items: [], total: 0 }
        map[key].items.push(r)
        map[key].total = (Number(map[key].total) + Number(r.amount)).toFixed(2)
      })
      this.groups = Object.values(map)
    },
    toggleAllDays() {
      const all = this.days.every(Boolean)
      this.days = this.days.map(() => !all)
      this.refresh()
    },
    async savePlan() {
      const selectedDays = this.days.map((v, i) => v ? i : -1).filter(i => i >= 0)
      const r = await api.post('/api/purchase/savePlan', null, { params: { weekKey: this.effWeek, days: selectedDays.length ? selectedDays.join(',') : undefined, mealType: this.mealType } })
      this.currentPlan = r.planKey
      this.plans = await api.get('/api/purchase/plans')
      this.$message.success('采购计划已保存，批次：' + r.planKey)
    },
    async loadPlan(pk) {
      if (!pk) return
      this.currentPlan = pk
      this.rows = await api.get('/api/purchase/plan', { planKey: pk })
      const map = {}
      let t = 0
      this.rows.forEach(r => {
        const key = r.supplierName || '未指定供应商'
        if (!map[key]) map[key] = { supplier: key, items: [], total: 0 }
        map[key].items.push(r)
        map[key].total = (Number(map[key].total) + Number(r.amount)).toFixed(2)
        t += Number(r.amount)
      })
      this.groups = Object.values(map)
      this.totalAmount = t.toFixed(2)
    },
    async exportPlan() {
      const pk = this.currentPlan || this.viewPlan
      if (!pk) { this.$message.warning('请先保存采购计划'); return }
      const r = await api.blob('/api/purchase/export', { planKey: pk })
      this.$download(r.data, '采购计划-' + pk + '.xlsx')
    },
    money(v) { return Number(v || 0).toFixed(2) },
    // 根据规格计算数量：规格为空→数量=总重量(kg)；规格非空→按数值乘积换算
    calcQty(row) {
      const kg = Number(row.totalWeight) || 0
      const spec = row.spec || ''
      if (!spec) return { v: kg, unit: 'kg' }
      const slash = spec.lastIndexOf('/')
      const unit = slash >= 0 ? spec.slice(slash + 1).trim() : ''
      const left = slash >= 0 ? spec.slice(0, slash) : spec
      const parts = left.split('*').map(s => s.trim()).filter(Boolean)
      let product = 1
      let baseKg = false
      parts.forEach((p, i) => {
        const m = p.match(/(\d+(?:\.\d+)?)/)
        if (!m) return
        if (i === 0) {
          const lower = p.toLowerCase()
          // 单位换算基准：1升=1kg、1000ml=1升=1kg
          if (/(ml|毫升)/.test(lower)) baseKg = false      // ml → 数量=kg*1000/乘积
          else if (/(kg|克|斤)/.test(lower)) baseKg = true // kg → 数量=kg/乘积
          else if (/升/.test(lower)) baseKg = true         // 升 → 1升=1kg
          else if (/(^|[^a-z])l($|[^a-z])/.test(lower)) baseKg = true // l（非ml）→ 1l=1kg
          else baseKg = false                              // 默认按 g → 数量=kg*1000/乘积
        }
        product *= parseFloat(m[1])
      })
      if (!product) return { v: kg, unit: unit || 'kg' }
      const qty = baseKg ? kg / product : kg * 1000 / product
      return { v: qty, unit: unit }
    },
    qtyText(row) {
      const q = this.calcQty(row)
      const v = Math.round(q.v * 100) / 100
      return v + '（' + q.unit + '）'
    }
  }
}
</script>
<style scoped>
.supplier-block { margin-bottom: 16px; border: 1px solid #d5e3f6; border-radius: 8px; overflow: hidden; }
.supplier-head { background: #e6f0fd; color: #1c3f73; font-weight: 700; padding: 8px 12px; }
.supplier-total { text-align: right; padding: 6px 12px; background: #f7fbff; color: #5c789c; font-weight: 600; }
</style>
