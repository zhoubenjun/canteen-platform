<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <div class="text-muted">系统角色列表</div>
        <div class="toolbar-right" v-if="isAdmin">
          <el-button type="primary" size="small" icon="el-icon-plus" @click="openAdd">新增角色</el-button>
        </div>
      </div>
      <el-table :data="list" border size="small" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleName" label="角色名称" />
        <el-table-column prop="createdAt" label="创建时间" />
        <el-table-column label="操作" width="100" align="center" v-if="isAdmin">
          <template slot-scope="{ row }">
            <el-button type="danger" size="mini" :disabled="isBuiltin(row.roleName)" @click="delRole(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog title="新增角色" :visible.sync="addVisible" width="400px" append-to-body>
      <el-form label-width="80px" @submit.native.prevent>
        <el-form-item label="角色名称">
          <el-input v-model="addName" maxlength="20"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="doAdd">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import api from '../api'

const BUILTIN = ['admin']

export default {
  name: 'RoleManage',
  data() {
    return { list: [], loading: false, addVisible: false, addLoading: false, addName: '' }
  },
  computed: {
    isAdmin() {
      try {
        const u = JSON.parse(localStorage.getItem('canteen_user') || 'null')
        if (!u) return false
        if ((u.username || '').toLowerCase() === 'admin') return true
        return (u.roles || []).indexOf('admin') >= 0
      } catch (e) { return false }
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      this.loading = true
      api.get('/api/role/list')
        .then(d => { this.list = d || [] })
        .catch(() => {})
        .finally(() => { this.loading = false })
    },
    isBuiltin(name) {
      return BUILTIN.indexOf(name) >= 0
    },
    openAdd() {
      this.addName = ''
      this.addVisible = true
    },
    doAdd() {
      if (!this.addName || !this.addName.trim()) return this.$message.warning('请输入角色名称')
      this.addLoading = true
      api.post('/api/role/add', { roleName: this.addName.trim() })
        .then(() => {
          this.$message.success('新增成功')
          this.addVisible = false
          this.load()
        })
        .catch(() => {})
        .finally(() => { this.addLoading = false })
    },
    delRole(row) {
      this.$confirm(`确定删除角色「${row.roleName}」？`, '提示', { type: 'warning' })
        .then(() => api.delete(`/api/role/${row.id}`))
        .then(() => { this.$message.success('删除成功'); this.load() })
        .catch(() => {})
    }
  }
}
</script>
