<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <el-button type="primary" icon="el-icon-upload2" @click="openDialog">读取消费数据（CSV）</el-button>
        <span class="text-muted">仅导入所选月份、登记类型为“教职工”的记录，非本月数据不读取</span>
      </div>

      <div class="toolbar">
        <span class="filter-item">
          <span class="label">登记姓名：</span>
          <el-input v-model="query.name" placeholder="请输入姓名" clearable style="width: 160px" @keyup.enter="search" />
        </span>
        <span class="filter-item">
          <span class="label">登记类型：</span>
          <el-select v-model="query.registerType" placeholder="请选择" clearable style="width: 140px">
            <el-option label="教职工" value="教职工"></el-option>
          </el-select>
        </span>
        <span class="filter-item">
          <span class="label">消费业务类型：</span>
          <el-select v-model="query.businessType" placeholder="请选择" clearable style="width: 140px">
            <el-option label="早餐" value="早餐"></el-option>
            <el-option label="午餐" value="午餐"></el-option>
            <el-option label="晚餐" value="晚餐"></el-option>
            <el-option label="夜宵" value="夜宵"></el-option>
            <el-option label="其他" value="其他"></el-option>
          </el-select>
        </span>
        <span class="filter-item">
          <span class="label">消费日期：</span>
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
        <el-button type="primary" icon="el-icon-search" @click="search">查询</el-button>
        <el-button icon="el-icon-refresh" @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe max-height="560">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="registerType" label="登记类型" width="100" align="center" />
        <el-table-column prop="registerName" label="登记姓名" width="110" align="center" />
        <el-table-column prop="className" label="班级名称" min-width="160" />
        <el-table-column prop="amount" label="消费金额(元)" width="110" align="center" />
        <el-table-column prop="payStatus" label="支付状态" width="100" align="center" />
        <el-table-column prop="consumeDate" label="消费日期" width="120" align="center" />
        <el-table-column prop="businessType" label="消费业务类型" width="120" align="center" />
        <el-table-column prop="dish" label="消费菜品" min-width="160" />
        <el-table-column prop="orderNo" label="消费订单号" min-width="180" show-overflow-tooltip />
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

      <el-dialog title="读取消费数据" :visible.sync="dialogVisible" width="420px" :close-on-click-modal="false">
        <div class="filter-item">
          <span class="label">导入月份：</span>
          <el-date-picker v-model="importMonth" type="month" value-format="yyyy-MM" format="yyyy-MM"
                          placeholder="选择月份" style="width: 180px" />
        </div>
        <div class="filter-item" style="margin-top: 12px">
          <span class="label">数据文件：</span>
          <el-upload
            ref="upload"
            :auto-upload="false"
            :limit="1"
            :on-change="onFileChange"
            :on-remove="onFileRemove"
            :file-list="fileList"
            accept=".csv"
          >
            <el-button type="primary" plain icon="el-icon-folder-opened">选择 CSV 文件</el-button>
          </el-upload>
        </div>
        <div class="text-muted" style="margin-top: 8px">仅导入所选月份、登记类型为“教职工”的记录，非本月数据不读取。</div>
        <span slot="footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="importing" @click="doImport">开始导入</el-button>
        </span>
      </el-dialog>
    </div>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'ConsumptionView',
  data() {
    return {
      loading: false,
      importing: false,
      dialogVisible: false,
      importMonth: '',
      importFile: null,
      fileList: [],
      rows: [],
      total: 0,
      page: 1,
      size: 10,
      query: { name: '', registerType: '', businessType: '', dateRange: [] }
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
        registerType: this.query.registerType,
        businessType: this.query.businessType,
        start: start || '',
        end: end || '',
        page: this.page,
        size: this.size
      }
    },
    async load() {
      this.loading = true
      try {
        const data = await api.get('/api/pa/consumption/page', this.buildParams())
        this.rows = data.rows || []
        this.total = data.total || 0
      } finally {
        this.loading = false
      }
    },
    search() { this.page = 1; this.load() },
    reset() {
      this.query = { name: '', registerType: '', businessType: '', dateRange: [] }
      this.search()
    },
    onPageChange(p) { this.page = p; this.load() },
    onSizeChange(s) { this.size = s; this.page = 1; this.load() },
    openDialog() {
      this.importMonth = ''
      this.importFile = null
      this.fileList = []
      this.dialogVisible = true
    },
    onFileChange(file) {
      const ok = file.name.toLowerCase().endsWith('.csv')
      if (!ok) {
        this.$message.error('请选择 .csv 格式文件')
        this.importFile = null
        this.fileList = []
        return
      }
      this.importFile = file.raw
      this.fileList = [file]
    },
    onFileRemove() {
      this.importFile = null
      this.fileList = []
    },
    async doImport() {
      if (!this.importMonth) {
        this.$message.error('请先选择导入月份')
        return
      }
      if (!this.importFile) {
        this.$message.error('请先选择消费数据文件')
        return
      }
      this.importing = true
      try {
        const fd = new FormData()
        fd.append('file', this.importFile)
        fd.append('month', this.importMonth)
        const d = await api.upload('/api/pa/consumption/import', fd) || {}
        const skip = d.skipped ? `，已跳过非本月 ${d.skipped} 条` : ''
        this.$message.success(`导入成功，${d.month || this.importMonth} 共 ${d.total || 0} 条教职工消费记录${skip}`)
        this.dialogVisible = false
        this.search()
      } finally {
        this.importing = false
      }
    }
  }
}
</script>

<style scoped>
.filter-item { display: inline-flex; align-items: center; gap: 6px; margin-right: 12px; }
.filter-item .label { color: #5c789c; font-size: 13px; white-space: nowrap; }
.spacer { flex: 1; }
</style>