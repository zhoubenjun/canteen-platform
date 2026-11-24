<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="按食材名称查询" clearable style="width:220px" @keyup.enter.native="search" @clear="search" />
        <el-select v-model="source" placeholder="询价单位" clearable style="width:160px" @change="search">
          <el-option v-for="s in sources" :key="s" :label="s" :value="s" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <div class="spacer" style="flex:1"></div>
        <el-button type="primary" @click="openAdd">新增</el-button>
      </div>

      <el-table :data="rows" border stripe v-loading="loading" style="width:100%" :row-class-name="rowClassName">
        <el-table-column type="index" label="序号" width="70" align="center" :index="seqIndex" />
        <el-table-column prop="ingredientName" label="食材名称" min-width="190" show-overflow-tooltip />
        <el-table-column prop="unit" label="单位" min-width="120" align="center" />
        <el-table-column prop="price" label="单价" min-width="150" align="right">
          <template slot-scope="{row}">￥{{ fmtMoney(row.price) }}</template>
        </el-table-column>
        <el-table-column prop="source" label="询价单位" min-width="170" />
        <el-table-column label="创建时间" min-width="175" align="center">
          <template slot-scope="{row}">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="修改时间" min-width="175" align="center">
          <template slot-scope="{row}">{{ fmtTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template slot-scope="{row}">
            <el-button type="text" @click="openEdit(row)">编辑</el-button>
            <el-button type="text" style="color:#f56c6c" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="flex" style="justify-content:flex-end;margin-top:10px">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total" :page-sizes="[10,20,50,100]" :page-size="pageSize" :current-page.sync="page" @size-change="onSizeChange" @current-change="load" />
      </div>
    </div>

    <el-dialog :title="form.id ? '编辑询价' : '新增询价'" :visible.sync="dialogVisible" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="询价单位" required>
          <el-select v-model="form.source" placeholder="请选择询价单位" style="width:100%">
            <el-option v-for="s in sources" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="食材名称" required>
          <el-select v-model="form.ingredientName" filterable placeholder="请选择蔬菜类食材" style="width:100%">
            <el-option v-for="v in vegetables" :key="v" :label="v" :value="v" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="form.unit" disabled />
        </el-form-item>
        <el-form-item label="单价" required>
          <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="0.1" controls-position="right" style="width:180px" />
          <span style="margin-left:8px;color:#7a8b9e">元 / kg</span>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'MarketInquiry',
  data() {
    return {
      sources: ['楚小兔', '泰康永乐', '云仓电商'],
      keyword: '',
      source: '',
      rows: [],
      total: 0,
      page: 1,
      pageSize: 10,
      loading: false,
      saving: false,
      vegetables: [],
      dialogVisible: false,
      form: { id: null, source: '', ingredientName: '', unit: 'kg', price: undefined }
    }
  },
  created() {
    this.load()
    this.loadVegetables()
  },
  methods: {
    fmtTime(v) { return v ? String(v).replace('T', ' ').slice(0, 16) : '' },
    fmtMoney(v) { return v == null ? '' : Number(v).toFixed(2) },
    seqIndex(i) { return (this.page - 1) * this.pageSize + i + 1 },
    rowClassName({ row }) {
      if (!this.vegetables.length) return ''
      return this.vegetables.indexOf(row.ingredientName) >= 0 ? '' : 'inquiry-row-off'
    },
    onSizeChange(sz) { this.pageSize = sz; this.page = 1; this.load() },
    search() { this.page = 1; this.load() },
    async load() {
      this.loading = true
      try {
        const d = await api.get('/api/inquiry/list', { keyword: this.keyword, source: this.source, page: this.page, size: this.pageSize })
        this.rows = d.rows || []
        this.total = d.total || 0
      } finally {
        this.loading = false
      }
    },
    async loadVegetables() {
      try {
        this.vegetables = (await api.get('/api/inquiry/vegetables')) || []
      } catch (e) {
        this.vegetables = []
      }
      // 蔬菜类名单异步到达后强制重绘，刷新行标记
      this.rows = this.rows.slice()
    },
    openAdd() {
      this.form = { id: null, source: '', ingredientName: '', unit: 'kg', price: undefined }
      this.dialogVisible = true
    },
    openEdit(row) {
      this.form = {
        id: row.id,
        source: row.source,
        ingredientName: row.ingredientName,
        unit: row.unit || 'kg',
        price: row.price == null ? undefined : Number(row.price)
      }
      this.dialogVisible = true
    },
    async save() {
      if (!this.form.source) return this.$message.warning('请选择询价单位')
      if (!this.form.ingredientName) return this.$message.warning('请选择食材名称')
      if (!this.form.price || Number(this.form.price) <= 0) return this.$message.warning('请填写单价')
      this.saving = true
      try {
        const payload = { ...this.form, unit: 'kg' }
        if (payload.id) {
          await api.put('/api/inquiry', payload)
          this.$message.success('修改成功')
        } else {
          await api.post('/api/inquiry', payload)
          this.$message.success('新增成功')
        }
        this.dialogVisible = false
        this.load()
      } catch (e) {
        // 拦截器已提示
      } finally {
        this.saving = false
      }
    },
    del(row) {
      this.$confirm('确定删除「' + row.ingredientName + '」在「' + row.source + '」的询价记录？', '提示', { type: 'warning' })
        .then(() => api.del('/api/inquiry/' + row.id))
        .then(() => { this.$message.success('已删除'); this.load() })
        .catch(() => {})
    }
  }
}
</script>

<style>
/* 食材名称不属于食材管理「蔬菜类」的行：整行标红提示 */
.el-table__row.inquiry-row-off > td,
.el-table__row.inquiry-row-off.el-table__row--striped > td {
  background-color: #fdeaea !important;
  color: #d93025;
}
.el-table__row.inquiry-row-off:hover > td {
  background-color: #fbdede !important;
}
</style>
