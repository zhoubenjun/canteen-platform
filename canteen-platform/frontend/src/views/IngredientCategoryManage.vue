<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="按类别名称查询" clearable style="width:220px" @keyup.enter.native="load" @clear="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <div class="spacer" style="flex:1"></div>
        <el-button type="primary" @click="openAdd">新增类别</el-button>
      </div>
      <el-table :data="rows" border stripe v-loading="loading">
        <el-table-column type="index" label="序号" width="60" :index="seqIndex" />
        <el-table-column prop="name" label="类别名称" min-width="110" />
        <el-table-column prop="code" label="编码" min-width="90" />
        <el-table-column label="状态" min-width="100">
          <template slot-scope="{row}">
            <el-switch :value="row.status" :active-value="1" :inactive-value="0" @change="v => toggleStatus(row, v)" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="185" :formatter="(row, col, v) => fmtTime(v)" />
        <el-table-column prop="updatedAt" label="修改时间" min-width="185" :formatter="(row, col, v) => fmtTime(v)" />
        <el-table-column prop="createdBy" label="创建人" min-width="115" />
        <el-table-column prop="updatedBy" label="修改人" min-width="115" />
        <el-table-column label="操作" min-width="150">
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

    <el-dialog :title="form.id ? '编辑类别' : '新增类别'" :visible.sync="dialogVisible" width="440px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="类别名称" required>
          <el-input v-model="form.name" placeholder="请输入类别名称" />
        </el-form-item>
        <el-form-item label="编码">
          <el-input v-model="form.code" placeholder="请输入类别编码" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="开启" inactive-text="关闭" />
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
  name: 'IngredientCategoryManage',
  data() {
    return {
      keyword: '',
      rows: [],
      total: 0,
      page: 1,
      pageSize: 10,
      loading: false,
      saving: false,
      dialogVisible: false,
      form: { id: null, name: '', code: '', status: 1 }
    }
  },
  created() { this.load() },
  methods: {
    fmtTime(v) { return v ? String(v).replace('T', ' ').slice(0, 19) : '' },
    seqIndex(i) { return (this.page - 1) * this.pageSize + i + 1 },
    onSizeChange(sz) { this.pageSize = sz; this.page = 1; this.load() },
    async load() {
      this.loading = true
      try {
        const d = await api.get('/api/ingredient-category/list', { keyword: this.keyword, page: this.page, size: this.pageSize })
        this.rows = d.rows || []
        this.total = d.total || 0
      } finally {
        this.loading = false
      }
    },
    openAdd() {
      this.form = { id: null, name: '', code: '', status: 1 }
      this.dialogVisible = true
    },
    openEdit(row) {
      this.form = { id: row.id, name: row.name, code: row.code || '', status: row.status === undefined ? 1 : row.status }
      this.dialogVisible = true
    },
    async toggleStatus(row, v) {
      try {
        await api.post('/api/ingredient-category/status', null, { params: { id: row.id, status: v } })
        row.status = v
        this.$message.success(v ? '已开启' : '已关闭')
      } catch (e) {
        // 拦截器已提示
      }
    },
    async save() {
      if (!this.form.name || !this.form.name.trim()) return this.$message.warning('请输入类别名称')
      this.saving = true
      try {
        if (this.form.id) {
          await api.put('/api/ingredient-category', this.form)
          this.$message.success('修改成功')
        } else {
          await api.post('/api/ingredient-category', this.form)
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
      this.$confirm('确定删除食材类别「' + row.name + '」？', '提示', { type: 'warning' })
        .then(() => api.del('/api/ingredient-category/' + row.id))
        .then(() => { this.$message.success('已删除'); this.load() })
        .catch(() => {})
    }
  }
}
</script>
