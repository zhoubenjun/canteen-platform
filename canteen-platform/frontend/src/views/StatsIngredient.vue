<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <span class="text-muted">用餐日期：</span>
        <el-date-picker
          v-model="range"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="yyyy-MM-dd"
          style="width:260px" />
        <el-input v-model="keyword" placeholder="输入食材名搜索" prefix-icon="el-icon-search" clearable style="width:200px" />
        <el-select v-model="useFilter" placeholder="使用情况" clearable style="width:120px">
          <el-option label="有使用" value="used" />
          <el-option label="无使用" value="unused" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="refresh">统计</el-button>
        <div class="spacer" style="flex:1"></div>
        <span class="text-muted">单价取食材档案当前最新单价，总成本=总重量×单价</span>
      </div>
    </div>

    <div class="page-card">
      <div class="chart-toggle" @click="toggleChart">
        <span>📊 各类别 TOP 6 食材柱状图</span>
        <i class="el-icon-arrow-up" v-if="chartVisible"></i>
        <i class="el-icon-arrow-down" v-else></i>
      </div>
      <div v-if="chartVisible" style="margin-bottom:10px">
        <div ref="chart" style="width:100%;height:420px"></div>
      </div>

      <div class="flex" style="justify-content:space-between;margin-bottom:10px;margin-top:10px">
        <h3 style="margin:0;color:#1c3f73">食材用量统计<span class="text-muted">（{{ range[0] }} 至 {{ range[1] }}）</span></h3>
        <div>合计成本：<b style="color:#2f6fd0;font-size:20px">¥{{ totalCost }}</b></div>
      </div>

      <el-table :data="pagedRows" border size="small" stripe v-loading="loading">
        <el-table-column label="排名" width="60" align="center">
          <template slot-scope="{$index}">{{ (currentPage - 1) * pageSize + $index + 1 }}</template>
        </el-table-column>
        <el-table-column prop="ingredientName" label="食材" min-width="140" />
        <el-table-column prop="categoryName" label="类别" min-width="100">
          <template slot-scope="{row}">{{ row.categoryName || '未分类' }}</template>
        </el-table-column>
        <el-table-column label="总重量（KG）" min-width="110">
          <template slot-scope="{row}"><b>{{ row.totalWeightKg }}</b></template>
        </el-table-column>
        <el-table-column label="单价（元/KG）" min-width="110">
          <template slot-scope="{row}">¥{{ money(row.unitPrice) }}</template>
        </el-table-column>
        <el-table-column label="总成本（元）" min-width="110">
          <template slot-scope="{row}"><b style="color:#2f6fd0">¥{{ money(row.totalCost) }}</b></template>
        </el-table-column>
      </el-table>
      <div class="text-muted" style="text-align:center;padding:30px" v-if="!rows.length && !loading">
        该日期区间内暂无食谱数据
      </div>
      <el-pagination
        v-if="rows.length"
        style="margin-top:12px;text-align:right"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page.sync="currentPage"
        :page-sizes="[10, 20, 50, 100, 200]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="filteredRows.length" />
    </div>
  </div>
</template>

<script>
import api from '../api'
import * as echarts from 'echarts'

export default {
  name: 'StatsIngredient',
  data() {
    return {
      range: [],
      rows: [],
      loading: false,
      currentPage: 1,
      pageSize: 10,
      chartVisible: false,
      chart: null,
      keyword: '',
      useFilter: 'used'
    }
  },
  computed: {
    totalCost() {
      const t = this.rows.reduce((s, r) => s + Number(r.totalCost || 0), 0)
      return t.toFixed(2)
    },
    filteredRows() {
      const k = (this.keyword || '').trim()
      return this.rows.filter(r => {
        if (k && !(r.ingredientName || '').includes(k)) return false
        if (this.useFilter === 'used' && !(Number(r.used) > 0)) return false
        if (this.useFilter === 'unused' && Number(r.used) > 0) return false
        return true
      })
    },
    pagedRows() {
      const start = (this.currentPage - 1) * this.pageSize
      return this.filteredRows.slice(start, start + this.pageSize)
    }
  },
  created() {
    const end = new Date()
    const start = new Date(); start.setDate(start.getDate() - 29)
    this.range = [this.dateStr(start), this.dateStr(end)]
    this.refresh()
  },
  methods: {
    dateStr(d) {
      return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
    },
    money(v) { return Number(v || 0).toFixed(2) },
    async refresh() {
      if (!this.range || this.range.length !== 2 || !this.range[0] || !this.range[1]) {
        return this.$message.warning('请选择完整的开始和结束日期')
      }
      this.loading = true
      try {
        this.rows = await api.get('/api/stats/ingredient', { startDate: this.range[0], endDate: this.range[1] })
        this.currentPage = 1
        if (this.chartVisible) this.$nextTick(() => this.renderChart())
      } finally {
        this.loading = false
      }
    },
    toggleChart() {
      this.chartVisible = !this.chartVisible
      if (this.chartVisible) this.$nextTick(() => this.renderChart())
    },
    renderChart() {
      const el = this.$refs.chart
      if (!el || !this.rows.length) return
      if (this.chart) this.chart.dispose()
      this.chart = echarts.init(el)

      // 按类别分组，每类取总重量前 6；类别按总重量降序排列
      const byCat = {}
      this.rows.forEach(r => {
        const k = r.categoryName || '未分类'
        if (!byCat[k]) byCat[k] = []
        byCat[k].push(r)
      })
      const cats = Object.keys(byCat).map(name => ({
        name,
        items: byCat[name].slice(0, 6),
        total: byCat[name].reduce((s, r) => s + Number(r.totalWeightKg || 0), 0)
      })).sort((a, b) => b.total - a.total)

      // X 轴 = 各类别 TOP6 食材按类别顺序拼接
      const xData = []
      cats.forEach(c => c.items.forEach(r => xData.push(r.ingredientName)))

      // 每个类别一个 series，不同类别自动不同颜色；非本类别位置补 null
      const palette = ['#2f6fd0', '#1c4f9c', '#4a90d9', '#6ab0e0', '#7a8fd9', '#3aa8a0', '#8a7fd0', '#5a9ec7', '#9aa8b8']
      const series = cats.map((c, i) => {
        const data = new Array(xData.length).fill(null)
        let idx = 0
        cats.slice(0, i).forEach(prev => idx += prev.items.length)
        c.items.forEach((r, j) => data[idx + j] = Number(r.totalWeightKg))
        return {
          name: c.name,
          type: 'bar',
          data,
          itemStyle: { color: palette[i % palette.length] },
          label: { show: true, position: 'top', formatter: '{c}', fontSize: 10 }
        }
      })

      this.chart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: p => {
          const hit = p.find(x => x.value != null)
          return hit ? hit.name + '<br/>' + hit.seriesName + '：' + hit.value + ' KG' : ''
        }},
        legend: { top: 0, type: 'scroll' },
        grid: { left: 60, right: 20, top: 40, bottom: 70 },
        xAxis: { type: 'category', data: xData, axisLabel: { rotate: 40, interval: 0, fontSize: 10 } },
        yAxis: { type: 'value', name: 'KG' },
        series
      })
    },
    handleSizeChange(s) { this.pageSize = s; this.currentPage = 1 },
    handleCurrentChange(p) { this.currentPage = p }
  }
}
</script>

<style scoped>
.chart-toggle {
  cursor: pointer;
  padding: 8px 12px;
  margin-bottom: 10px;
  background: #e6f0fd;
  color: #1c3f73;
  font-weight: 700;
  border-radius: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  user-select: none;
}
.chart-toggle:hover { background: #dae8fb; }
</style>
