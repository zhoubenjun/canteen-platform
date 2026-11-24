<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <span class="text-muted">以陪餐人员 + 陪餐日期 + 餐次为基准，与消费数据的登记姓名 + 消费日期 + 消费业务类型逐一比对，判断是否已支付。</span>
        <span class="spacer"></span>
        <el-button type="primary" plain icon="el-icon-download" :loading="exporting" @click="exportExcel">导出分析结果</el-button>
        <el-button type="primary" icon="el-icon-data-analysis" :loading="analyzing" @click="openDialog">支付分析</el-button>
      </div>

      <div v-if="stats" class="toolbar">
        <el-tag type="info">总记录：{{ stats.total }}</el-tag>
        <el-tag type="success">已支付：{{ stats.paid }}</el-tag>
        <el-tag type="danger">未支付：{{ stats.unpaid }}</el-tag>
      </div>

      <div class="toolbar">
        <span class="filter-item">
          <span class="label">姓名：</span>
          <el-input v-model="query.name" placeholder="请输入姓名" clearable style="width: 160px" @keyup.enter="search" />
        </span>
        <span class="filter-item">
          <span class="label">陪餐日期：</span>
          <el-date-picker
            v-model="query.dateRange"
            type="daterange"
            value-format="yyyy-MM-dd"
            format="yyyy-MM-dd"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
          />
        </span>
        <span class="filter-item">
          <span class="label">餐次：</span>
          <el-select v-model="query.mealType" placeholder="请选择" clearable style="width: 140px">
            <el-option label="早餐" value="早餐"></el-option>
            <el-option label="午餐" value="午餐"></el-option>
            <el-option label="晚餐" value="晚餐"></el-option>
          </el-select>
        </span>
        <span class="filter-item">
          <span class="label">是否支付：</span>
          <el-select v-model="query.paid" placeholder="请选择" clearable style="width: 140px">
            <el-option label="已支付" value="已支付"></el-option>
            <el-option label="未支付" value="未支付"></el-option>
          </el-select>
        </span>
        <el-button type="primary" icon="el-icon-search" @click="search">查询</el-button>
        <el-button icon="el-icon-refresh" @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe max-height="560">
        <el-table-column type="index" label="序号" width="80" align="center" />
        <el-table-column prop="companionName" label="陪餐人员" width="160" align="center" />
        <el-table-column prop="companionDate" label="陪餐日期" width="160" align="center" />
        <el-table-column prop="mealType" label="餐次" width="120" align="center" />
        <el-table-column label="是否支付" width="120" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.paid === '已支付' ? 'success' : 'danger'">{{ scope.row.paid }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="toolbar" style="justify-content: flex-end; margin-top: 12px;">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :current-page="page"
          :page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>

      <el-dialog title="支付分析" :visible.sync="dialogVisible" width="380px" :close-on-click-modal="false">
        <div class="filter-item">
          <span class="label">分析月份：</span>
          <el-date-picker v-model="analysisMonth" type="month" value-format="yyyy-MM" format="yyyy-MM"
                          placeholder="选择月份" style="width: 180px" />
        </div>
        <div class="text-muted" style="margin-top: 8px">将先清除该月已有分析结果，再按该月陪餐数据重新比对统计。</div>
        <span slot="footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="analyzing" @click="confirmRun">开始分析</el-button>
        </span>
      </el-dialog>
    </div>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'AnalysisView',
  data() {
    return {
      loading: false,
      analyzing: false,
      exporting: false,
      dialogVisible: false,
      analysisMonth: '',
      rows: [],
      total: 0,
      page: 1,
      size: 10,
      stats: null,
      query: { name: '', mealType: '', paid: '', dateRange: [] }
    }
  },
  created() {
    this.load()
  },
  methods: {
    buildParams() {
      const [start, end] = this.query.dateRange || []
      return {
        name: this.query.name,
        mealType: this.query.mealType,
        paid: this.query.paid,
        start: start || '',
        end: end || '',
        page: this.page,
        size: this.size
      }
    },
    async load() {
      this.loading = true
      try {
        const data = await api.get('/api/pa/analysis/page', this.buildParams())
        this.rows = data.rows || []
        this.total = data.total || 0
      } finally {
        this.loading = false
      }
    },
    openDialog() {
      this.analysisMonth = ''
      this.dialogVisible = true
    },
    async confirmRun() {
      if (!this.analysisMonth) {
        this.$message.error('请先选择分析月份')
        return
      }
      this.analyzing = true
      try {
        this.stats = await api.post('/api/pa/analysis/run', {}, { params: { month: this.analysisMonth } })
        this.dialogVisible = false
        this.$message.success('支付分析完成')
        this.search()
      } finally {
        this.analyzing = false
      }
    },
    async exportExcel() {
      this.exporting = true
      try {
        const params = this.buildParams()
        delete params.page
        delete params.size
        const res = await api.blob('/api/pa/analysis/export', params)
        this.$download(res.data, '陪餐支付分析.xlsx')
      } finally {
        this.exporting = false
      }
    },
    search() { this.page = 1; this.load() },
    reset() {
      this.query = { name: '', mealType: '', paid: '', dateRange: [] }
      this.search()
    },
    onPageChange(p) { this.page = p; this.load() },
    onSizeChange(s) { this.size = s; this.page = 1; this.load() }
  }
}
</script>

<style scoped>
.filter-item { display: inline-flex; align-items: center; gap: 6px; margin-right: 12px; }
.filter-item .label { color: #5c789c; font-size: 13px; white-space: nowrap; }
.spacer { flex: 1; }
</style>