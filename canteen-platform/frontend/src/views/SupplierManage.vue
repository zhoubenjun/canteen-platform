<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="按供应商名称查询" clearable style="width:240px" @keyup.enter.native="load" @clear="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <div class="spacer" style="flex:1"></div>
        <el-button type="primary" @click="openAdd">新增供应商</el-button>
      </div>
      <el-table :data="rows" border stripe v-loading="loading">
        <el-table-column type="index" label="序号" width="60" :index="seqIndex" />
        <el-table-column prop="name" label="供应商名称" min-width="150" />
        <el-table-column prop="legalPerson" label="企业法人" min-width="90" />
        <el-table-column prop="contact" label="联系方式" width="110" />
        <el-table-column prop="supplyStart" label="供货开始时间" width="105" />
        <el-table-column prop="supplyEnd" label="供货结束时间" width="105" />
        <el-table-column label="营业执照" width="85">
          <template slot-scope="{row}">
            <el-image v-if="row.businessLicense" :src="row.businessLicense" :preview-src-list="[row.businessLicense]" fit="cover" style="width:56px;height:56px;border-radius:4px" />
            <span v-else class="text-muted">未上传</span>
          </template>
        </el-table-column>
        <el-table-column label="食品经营许可证" width="120">
          <template slot-scope="{row}">
            <el-image v-if="row.foodLicense" :src="row.foodLicense" :preview-src-list="[row.foodLicense]" fit="cover" style="width:56px;height:56px;border-radius:4px" />
            <span v-else class="text-muted">未上传</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdBy" label="创建人" width="90" />
        <el-table-column prop="createdAt" label="创建时间" width="160" :formatter="(row, col, v) => fmtTime(v)" />
        <el-table-column label="操作" width="140" fixed="right">
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

    <el-dialog :title="form.id ? '编辑供应商' : '新增供应商'" :visible.sync="dialogVisible" width="560px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="供应商名称" required>
          <el-input v-model="form.name" placeholder="请输入供应商名称" />
        </el-form-item>
        <el-form-item label="企业法人">
          <el-input v-model="form.legalPerson" placeholder="请输入企业法人" />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="form.contact" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="供货开始时间">
          <el-date-picker v-model="form.supplyStart" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width:200px" />
        </el-form-item>
        <el-form-item label="供货结束时间">
          <el-date-picker v-model="form.supplyEnd" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width:200px" />
        </el-form-item>
        <el-form-item label="营业执照">
          <el-upload :show-file-list="false" accept="image/*" :http-request="(o) => uploadLicense(o, 'businessLicense')">
            <el-button type="primary" plain size="small">上传营业执照</el-button>
          </el-upload>
          <div style="margin-top:8px" v-if="form.businessLicense">
            <el-image :src="form.businessLicense" :preview-src-list="[form.businessLicense]" fit="cover" style="width:90px;height:90px;border-radius:4px;border:1px solid #eee" />
            <el-button type="text" size="mini" style="color:#f56c6c" @click="form.businessLicense=''">移除</el-button>
          </div>
        </el-form-item>
        <el-form-item label="食品经营许可证">
          <el-upload :show-file-list="false" accept="image/*" :http-request="(o) => uploadLicense(o, 'foodLicense')">
            <el-button type="primary" plain size="small">上传食品经营许可证</el-button>
          </el-upload>
          <div style="margin-top:8px" v-if="form.foodLicense">
            <el-image :src="form.foodLicense" :preview-src-list="[form.foodLicense]" fit="cover" style="width:90px;height:90px;border-radius:4px;border:1px solid #eee" />
            <el-button type="text" size="mini" style="color:#f56c6c" @click="form.foodLicense=''">移除</el-button>
          </div>
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
  name: 'SupplierManage',
  data() {
    return {
      keyword: '',
      rows: [],
      total: 0,
      page: 1,
      pageSize: 10,
      loading: false,
      saving: false,
      uploading: '',
      dialogVisible: false,
      form: {
        id: null, name: '', legalPerson: '', contact: '',
        supplyStart: '', supplyEnd: '', businessLicense: '', foodLicense: ''
      }
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
        const d = await api.get('/api/supplier/list', { keyword: this.keyword, page: this.page, size: this.pageSize })
        this.rows = d.rows || []
        this.total = d.total || 0
      } finally {
        this.loading = false
      }
    },
    openAdd() {
      this.form = { id: null, name: '', legalPerson: '', contact: '', supplyStart: '', supplyEnd: '', businessLicense: '', foodLicense: '' }
      this.dialogVisible = true
    },
    openEdit(row) {
      this.form = {
        id: row.id, name: row.name, legalPerson: row.legalPerson || '', contact: row.contact || '',
        supplyStart: row.supplyStart || '', supplyEnd: row.supplyEnd || '',
        businessLicense: row.businessLicense || '', foodLicense: row.foodLicense || ''
      }
      this.dialogVisible = true
    },
    async uploadLicense({ file }, field) {
      const fd = new FormData()
      fd.append('file', file)
      try {
        const r = await api.upload('/api/supplier/upload', fd)
        this.form[field] = r.url
        this.$message.success('上传成功')
      } catch (e) {
        this.$message.error('上传失败')
      }
    },
    async save() {
      if (!this.form.name || !this.form.name.trim()) return this.$message.warning('请输入供应商名称')
      this.saving = true
      try {
        if (this.form.id) {
          await api.put('/api/supplier', this.form)
          this.$message.success('修改成功')
        } else {
          await api.post('/api/supplier', this.form)
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
      this.$confirm('确定删除供应商「' + row.name + '」？', '提示', { type: 'warning' })
        .then(() => api.del('/api/supplier/' + row.id))
        .then(() => { this.$message.success('已删除'); this.load() })
        .catch(() => {})
    }
  }
}
</script>
