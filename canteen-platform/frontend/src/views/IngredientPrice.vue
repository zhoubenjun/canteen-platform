<template>
  <div class="page">
    <el-tabs v-model="tab">
      <!-- ============ 食材列表 ============ -->
      <el-tab-pane label="食材价格" name="list">
        <div class="page-card">
          <div class="toolbar">
            <el-select v-model="q.supplierId" placeholder="按供应商" clearable filterable style="width:150px" @change="load">
              <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
            <el-select v-model="q.categoryId" placeholder="按类别" clearable filterable style="width:130px" @change="load">
              <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
            <el-input v-model="q.keyword" placeholder="按名称模糊查询" clearable style="width:180px" @input="onKeywordInput" @keyup.enter.native="searchKeyword" />
            <div class="spacer" style="flex:1"></div>
            <el-button type="primary" @click="openAdd">新增食材</el-button>
            <el-button type="primary" plain @click="exportExcel">导出</el-button>
            <el-upload :show-file-list="false" :http-request="importExcel" accept=".xlsx"><el-button type="primary" plain>批量导入</el-button></el-upload>
            <el-button type="danger" plain @click="batchDelete" :disabled="!selection.length">删除选中</el-button>
            <el-button type="danger" plain @click="clearAll">清空全部</el-button>
          </div>

          <el-table :data="treeRows" row-key="rid" :tree-props="{ children: 'children' }" :default-expand-all="false" border stripe @selection-change="s => selection = s" v-loading="loading">
            <el-table-column type="selection" width="42" :selectable="row => !row.isGroup" />
            <el-table-column label="序号" width="70">
              <template slot-scope="{row}">{{ row.seq }}</template>
            </el-table-column>
            <el-table-column label="食材名称" min-width="160">
              <template slot-scope="{row}">
                <span v-if="row.isGroup">
                  <b>{{ row.name }}</b>
                  <span class="text-muted" style="margin-left:6px">（{{ row.count }} 条报价）</span>
                </span>
                <span v-else>{{ row.name }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="categoryName" label="类别" width="90" />
            <el-table-column label="供应商" min-width="130">
              <template slot-scope="{row}"><span v-if="!row.isGroup">{{ row.supplierName }}</span></template>
            </el-table-column>
            <el-table-column label="规格(参考)" min-width="150">
              <template slot-scope="{row}"><span v-if="!row.isGroup">{{ row.spec }}</span></template>
            </el-table-column>
            <el-table-column label="单价" width="100">
              <template slot-scope="{row}">
                <b v-if="!row.isGroup" style="color:#2f6fd0">¥{{ money(row.price) }}</b>
              </template>
            </el-table-column>
            <el-table-column label="单位" width="60">
              <template slot-scope="{row}"><span v-if="!row.isGroup">{{ row.unit }}</span></template>
            </el-table-column>
            <el-table-column label="计价单位" width="90">
              <template slot-scope="{row}"><span v-if="!row.isGroup">{{ row.priceUnit }}</span></template>
            </el-table-column>
            <el-table-column label="更新时间" width="150">
              <template slot-scope="{row}"><span v-if="!row.isGroup">{{ row.updatedAt }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template slot-scope="{row}">
                <template v-if="!row.isGroup">
                  <el-button type="text" @click="openEdit(row)">编辑</el-button>
                  <el-button type="text" class="price-log-btn" @click="showHistory(row)">历史</el-button>
                  <el-button type="text" style="color:#f56c6c" @click="del(row)">删除</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
          <div class="flex" style="justify-content:flex-end;margin-top:10px">
            <el-pagination background layout="total, sizes, prev, pager, next" :total="total" :page-sizes="[5, 10, 50, 100]" :page-size="q.size" :current-page.sync="q.page" @size-change="onSizeChange" @current-change="load" />
          </div>
        </div>
      </el-tab-pane>

      <!-- ============ 价格日记 ============ -->
      <el-tab-pane label="价格修改日记" name="log">
        <div class="page-card">
          <div class="toolbar">
            <el-select v-model="logQ.supplierId" placeholder="按供应商" clearable style="width:150px">
              <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
            <el-select v-model="logQ.categoryId" placeholder="按类别" clearable style="width:130px">
              <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
            <el-select v-model="logQ.changeType" placeholder="操作类型" clearable style="width:120px">
              <el-option label="新增" value="新增" /><el-option label="修改" value="修改" /><el-option label="删除" value="删除" />
            </el-select>
            <el-date-picker v-model="logQ.range" type="daterange" value-format="yyyy-MM-dd" range-separator="至" start-placeholder="开始" end-placeholder="结束" style="width:250px" />
            <el-button type="primary" @click="loadLog">查询</el-button>
            <el-button type="primary" plain @click="exportLog">导出日记</el-button>
          </div>
          <el-table :data="logs" border stripe>
            <el-table-column prop="ingredientName" label="食材名" width="130" />
            <el-table-column prop="categoryName" label="类别" width="90" />
            <el-table-column prop="supplierName" label="供应商" min-width="130" />
            <el-table-column label="原价" width="90"><template slot-scope="{row}">¥{{ money(row.oldPrice) }}</template></el-table-column>
            <el-table-column label="新价" width="90"><template slot-scope="{row}">¥{{ money(row.newPrice) }}</template></el-table-column>
            <el-table-column label="涨幅" width="90">
              <template slot-scope="{row}">
                <span :style="{color: row.changePct>0?'#e6a23c':(row.changePct<0?'#67c23a':'#666')}">{{ row.changePct }}%</span>
              </template>
            </el-table-column>
            <el-table-column prop="changeType" label="操作" width="80">
              <template slot-scope="{row}"><el-tag size="mini" :type="row.changeType==='新增'?'success':(row.changeType==='删除'?'danger':'warning')">{{ row.changeType }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" width="160" />
          </el-table>
        </div>
      </el-tab-pane>

      <!-- ============ 价格统计 ============ -->
      <el-tab-pane label="价格统计" name="stats">
        <div class="page-card" v-if="stats">
          <div class="flex" style="gap:30px;flex-wrap:wrap;margin-bottom:16px">
            <div class="stat-box"><div class="n">{{ stats.upCount||0 }}</div><div class="l">涨价次数</div></div>
            <div class="stat-box"><div class="n">{{ stats.downCount||0 }}</div><div class="l">降价次数</div></div>
            <div class="stat-box"><div class="n">{{ money(stats.avgUp||0) }}%</div><div class="l">平均涨幅</div></div>
            <div class="stat-box"><div class="n" style="color:#e6a23c">{{ stats.maxName||'-' }}</div><div class="l">涨幅最大食材({{ money(stats.maxPct||0) }}%)</div></div>
          </div>
        </div>
        <div class="page-card">
          <el-select v-model="histIngId" placeholder="选择食材查看价格趋势" filterable clearable style="width:230px" @change="showHistory">
            <el-option v-for="i in ingredients" :key="i.id" :label="i.name" :value="i.id" />
          </el-select>
          <div v-if="histSummary" class="flex" style="gap:24px;margin:10px 0">
            <span>最高 <b style="color:#2f6fd0">¥{{ money(histSummary.max) }}</b></span>
            <span>最低 <b style="color:#67c23a">¥{{ money(histSummary.min) }}</b></span>
            <span>平均 <b>¥{{ money(histSummary.avg) }}</b></span>
            <span>变动次数 <b>{{ histSummary.count }}</b> 次</span>
          </div>
          <div ref="chart" style="width:100%;height:320px"></div>
        </div>
      </el-tab-pane>

      <!-- ============ 历史版本 ============ -->
      <el-tab-pane label="价格历史版本" name="snap">
        <div class="page-card">
          <div class="toolbar">
            <el-input v-model="snapName" placeholder="版本名称(留空自动命名)" style="width:220px" />
            <el-button type="primary" @click="saveSnap">保存当前价格为版本</el-button>
            <span class="text-muted">保存后可载入往期价格数据</span>
          </div>
          <el-table :data="snapshots" border>
            <el-table-column prop="versionName" label="版本名称" min-width="180" />
            <el-table-column prop="snapshotDate" label="保存时间" width="170" />
            <el-table-column label="操作" width="200">
              <template slot-scope="{row}">
                <el-button type="text" @click="loadSnap(row)">载入此版本</el-button>
                <el-button type="text" style="color:#f56c6c" @click="delSnap(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增/编辑食材 -->
    <el-dialog :title="form.id ? '编辑食材' : '新增食材'" :visible.sync="dialogVisible" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类别">
          <el-select v-model="form.categoryId" style="width:200px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="form.supplierId" style="width:220px">
            <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="规格"><el-input v-model="form.spec" placeholder="如 70g50根4包/件(仅参考)" /></el-form-item>
        <el-form-item label="单价" required>
          <el-input-number v-model="form.price" :precision="2" :min="0" style="width:150px" />
          <el-select v-model="form.priceUnit" style="width:120px;margin-left:8px" @change="autoUnit">
            <el-option label="元/kg" value="元/kg" /><el-option label="元/斤" value="元/斤" /><el-option label="元/个" value="元/个" /><el-option label="元/瓶" value="元/瓶" /><el-option label="元/L" value="元/L" />
          </el-select>
          <el-button type="text" @click="autoUnit">按规格自动计算</el-button>
        </el-form-item>
        <el-form-item label="采购单位"><el-input v-model="form.unit" placeholder="默认KG" /></el-form-item>
        <el-form-item label="单位克重"><el-input-number v-model="form.weightPerUnit" :min="0" style="width:150px" /><span class="text-muted">1个计价单位的克重(元/kg填1000)</span></el-form-item>
      </el-form>
      <div slot="footer"><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></div>
    </el-dialog>

    <!-- 单食材价格历史对话框 -->
    <el-dialog title="食材价格历史" :visible.sync="histVisible" width="720px">
      <div class="flex" style="gap:24px;margin-bottom:10px" v-if="histSummary">
        <span>最高 <b style="color:#2f6fd0">¥{{ money(histSummary.max) }}</b></span>
        <span>最低 <b style="color:#67c23a">¥{{ money(histSummary.min) }}</b></span>
        <span>平均 <b>¥{{ money(histSummary.avg) }}</b></span>
        <span>变动次数 <b>{{ histSummary.count }}</b> 次</span>
      </div>
      <div ref="histChart" style="width:100%;height:280px"></div>
    </el-dialog>
  </div>
</template>

<script>
import api from '../api'
import * as echarts from 'echarts'

export default {
  name: 'IngredientPrice',
  data() {
    return {
      tab: 'list',
      rows: [], treeRows: [], total: 0, loading: false, selection: [],
      categories: [], suppliers: [], ingredients: [],
      q: { keyword: '', categoryId: null, supplierId: null, page: 1, size: 10 },
      form: { id: null, name: '', categoryId: null, supplierId: null, spec: '', price: 0, priceUnit: '元/kg', unit: 'KG', weightPerUnit: 1000 },
      dialogVisible: false,
      logQ: { ingredientId: null, categoryId: null, supplierId: null, changeType: '', range: null },
      logs: [],
      stats: null,
      histIngId: null, histVisible: false, histSummary: null, histChart: null,
      snapshots: [], snapName: ''
    }
  },
  created() {
    this.load(); this.loadOptions(); this.loadLog(); this.loadStats(); this.loadSnapshots()
  },
  methods: {
    onSizeChange(sz) { this.q.size = sz; this.q.page = 1; this.load() },
    seqIndex(i) { return (this.q.page - 1) * this.q.size + i + 1 },
    onKeywordInput() {
      clearTimeout(this._kwTimer)
      this._kwTimer = setTimeout(() => { this.q.page = 1; this.load() }, 350)
    },
    searchKeyword() {
      clearTimeout(this._kwTimer)
      this.q.page = 1
      this.load()
    },
    async load() {
      this.loading = true
      try {
        const d = await api.get('/api/ingredient/page', this.q)
        this.rows = d.rows; this.total = d.total
        this.treeRows = this.buildTree(d.rows)
      } finally { this.loading = false }
    },
    // 同名食材多于一条时折叠为分组行，展开后显示各条报价
    buildTree(rows) {
      const groups = []; const map = {}
      ;(rows || []).forEach(r => {
        const key = r.name
        if (!map[key]) { map[key] = { name: r.name, categoryName: r.categoryName, list: [] }; groups.push(map[key]) }
        map[key].list.push(r)
      })
      const out = []; let seq = 0
      groups.forEach(g => {
        seq++
        if (g.list.length === 1) {
          const r = g.list[0]
          out.push({ ...r, rid: 'r' + r.id, seq: (this.q.page - 1) * this.q.size + seq })
        } else {
          out.push({
            rid: 'g' + g.name, isGroup: true, name: g.name, categoryName: g.categoryName,
            count: g.list.length, seq: (this.q.page - 1) * this.q.size + seq,
            children: g.list.map(r => ({ ...r, rid: 'r' + r.id, seq: '' }))
          })
        }
      })
      return out
    },
    async loadOptions() {
      this.categories = await api.get('/api/ingredient/categories')
      this.suppliers = await api.get('/api/ingredient/suppliers')
      this.ingredients = await api.get('/api/ingredient/all')
    },
    async loadLog() {
      const p = { ...this.logQ }
      if (p.range && p.range.length === 2) { p.start = p.range[0]; p.end = p.range[1] }
      delete p.range
      this.logs = await api.get('/api/ingredient/logs', p)
    },
    async loadStats() { this.stats = await api.get('/api/ingredient/stats') },
    async loadSnapshots() { this.snapshots = await api.get('/api/ingredient/snapshots') },
    money(v) { return Number(v || 0).toFixed(2) },
    openAdd() { this.form = { id: null, name: '', categoryId: null, supplierId: null, spec: '', price: 0, priceUnit: '元/kg', unit: 'KG', weightPerUnit: 1000 }; this.dialogVisible = true },
    openEdit(r) { this.form = { ...r }; this.dialogVisible = true },
    autoUnit() {
      const u = this.form.priceUnit
      if (u === '元/kg') this.form.weightPerUnit = 1000
      else if (u === '元/斤') this.form.weightPerUnit = 500
      else if (u === '元/L') this.form.weightPerUnit = 910
      else if (u === '元/个' || u === '元/瓶') this.form.weightPerUnit = this.form.weightPerUnit || 100
    },
    async save() {
      if (!this.form.name) { this.$message.warning('请输入名称'); return }
      if (!this.form.unit) this.form.unit = 'KG'
      if (!this.form.weightPerUnit) this.form.weightPerUnit = 1000
      await api.post('/api/ingredient/save', this.form)
      this.$message.success('保存成功，已记录价格日记')
      this.dialogVisible = false; this.load(); this.loadOptions(); this.loadLog(); this.loadStats()
    },
    del(row) {
      this.$confirm('确定删除食材「' + row.name + '」？', '提示', { type: 'warning' }).then(() => {
        api.del('/api/ingredient/' + row.id).then(() => { this.$message.success('已删除'); this.load(); this.loadLog(); this.loadStats() })
      }).catch(() => {})
    },
    batchDelete() {
      this.$confirm('确定删除选中的 ' + this.selection.length + ' 个食材？', '提示', { type: 'warning' }).then(() => {
        api.post('/api/ingredient/deleteBatch', this.selection.map(x => x.id)).then(() => { this.$message.success('已删除'); this.load(); this.loadLog(); this.loadStats() })
      }).catch(() => {})
    },
    clearAll() {
      this.$confirm('将清空所有食材（含价格日记），确定？', '警告', { type: 'warning' }).then(() => {
        api.del('/api/ingredient/clearAll').then(() => { this.$message.success('已清空'); this.load(); this.loadLog(); this.loadStats() })
      }).catch(() => {})
    },
    addCat() {},
    delCat() {},
    addSup() {},
    delSup() {},
    async showHistory(ingOrId) {
      const id = typeof ingOrId === 'object' ? ingOrId.id : ingOrId
      if (!id) return
      const list = await api.get('/api/ingredient/history/' + id)
      if (!list.length) { this.$message.info('暂无价格记录'); return }
      const prices = list.map(x => Number(x.newPrice))
      this.histSummary = { max: Math.max(...prices), min: Math.min(...prices), avg: prices.reduce((a, b) => a + b, 0) / prices.length, count: list.length }
      this.histVisible = true
      this.$nextTick(() => {
        const el = this.$refs.histChart
        if (!el) return
        if (this.histChart) this.histChart.dispose()
        this.histChart = echarts.init(el)
        this.histChart.setOption({
          tooltip: { trigger: 'axis' },
          grid: { left: 50, right: 20, top: 20, bottom: 40 },
          xAxis: { type: 'category', data: list.map(x => (x.time || '').slice(0, 16)), axisLabel: { rotate: 40 } },
          yAxis: { type: 'value', name: '元' },
          series: [{ name: '价格', type: 'line', smooth: true, data: prices, lineStyle: { color: '#2f6fd0', width: 3 }, itemStyle: { color: '#1c4f9c' }, areaStyle: { color: 'rgba(47,111,208,.15)' } }]
        })
      })
    },
    async exportExcel() { const r = await api.blob('/api/ingredient/export'); this.$download(r.data, '食材价格数据.xlsx') },
    async importExcel({ file }) { const fd = new FormData(); fd.append('file', file); const r = await api.upload('/api/ingredient/import', fd); this.$message.success('导入成功 ' + r.ok + ' 条' + (r.errors.length ? '，失败 ' + r.errors.length : '')); this.load(); this.loadOptions(); this.loadLog() },
    async exportLog() { const p = { ...this.logQ }; if (p.range && p.range.length === 2) { p.start = p.range[0]; p.end = p.range[1] } delete p.range; const r = await api.blob('/api/ingredient/exportLog', p); this.$download(r.data, '价格修改日记.xlsx') },
    async saveSnap() { const r = await api.post('/api/ingredient/snapshot', { versionName: this.snapName }); this.$message.success('已保存版本 ' + r.versionName); this.snapName = ''; this.loadSnapshots() },
    loadSnap(row) { this.$confirm('载入版本「' + row.versionName + '」将覆盖当前价格，确定？', '提示', { type: 'warning' }).then(() => api.post('/api/ingredient/loadSnapshot/' + row.id).then(() => { this.$message.success('已载入往期价格'); this.load(); this.loadLog(); this.loadStats() })).catch(() => {}) },
    delSnap(row) { this.$confirm('删除版本「' + row.versionName + '」？', '提示', { type: 'warning' }).then(() => api.del('/api/ingredient/snapshot/' + row.id).then(() => { this.$message.success('已删除'); this.loadSnapshots() })).catch(() => {}) }
  }
}
</script>
<style scoped>
.stat-box { text-align: center; min-width: 150px; padding: 14px 20px; background: #fff; border: 1px solid #d5e3f6; border-radius: 10px; }
.stat-box .n { font-size: 24px; font-weight: 800; color: #2f6fd0; }
.stat-box .l { color: #5c789c; margin-top: 4px; font-size: 13px; }
</style>
