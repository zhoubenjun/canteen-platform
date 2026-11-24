<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="按用户名查询" clearable style="width:220px" @keyup.enter.native="loadList" />
        <el-button type="primary" @click="loadList">查询</el-button>
        <div class="spacer" style="flex:1"></div>
        <el-button v-if="isAdmin" type="primary" @click="openAdd">+ 新增用户</el-button>
      </div>

      <el-table :data="list" border size="small" v-loading="loading">
        <el-table-column prop="username" label="用户名" width="160" />
        <el-table-column label="密码" width="200">
          <template slot-scope="{row}">
            <span>{{ row.password }}</span>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="180">
          <template slot-scope="{row}">
            <el-tag v-for="r in (row.roles || [])" :key="r" type="primary" size="mini" style="margin-right:4px">{{ r }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="是否在线" width="90">
          <template slot-scope="{row}">
            <el-tag :type="row.online === 1 ? 'success' : 'info'" size="mini">{{ row.online === 1 ? '在线' : '离线' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="登录时间" width="170">
          <template slot-scope="{row}">
            <span v-if="row.lastLoginTime">{{ row.lastLoginTime }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column prop="createdBy" label="创建人" width="120" />
        <el-table-column v-if="isAdmin" label="操作" width="230">
          <template slot-scope="{row}">
            <el-button type="text" @click="openReset(row)">重置密码</el-button>
            <el-button type="text" @click="openEdit(row)">编辑</el-button>
            <el-button type="text" style="color:#f56c6c" :disabled="row.username==='admin'" @click="delUser(row)">注销</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增用户 -->
    <el-dialog title="新增用户" :visible.sync="addVisible" width="420px">
      <el-form label-width="70px" @submit.native.prevent="doAdd">
        <el-form-item label="用户名"><el-input v-model="addForm.username" placeholder="登录用户名" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="addForm.password" placeholder="≥6位，字母/数字/特殊符号" show-password /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="addForm.roleIds" multiple placeholder="请选择角色" style="width:100%">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <div class="text-muted">密码必须大于等于6个字符，只能由数字、字母或特殊符号组成。</div>
      <span slot="footer">
        <el-button @click="addVisible=false">取消</el-button>
        <el-button type="primary" @click="doAdd">确定</el-button>
      </span>
    </el-dialog>

    <!-- 编辑角色 -->
    <el-dialog title="编辑用户角色" :visible.sync="editVisible" width="420px">
      <el-form label-width="70px" @submit.native.prevent="doEdit">
        <el-form-item label="用户名"><el-input :value="editTarget.username" disabled /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editRoleIds" multiple placeholder="请选择角色" style="width:100%">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="editVisible=false">取消</el-button>
        <el-button type="primary" @click="doEdit">确定</el-button>
      </span>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog title="重置密码" :visible.sync="resetVisible" width="420px">
      <el-form label-width="70px" @submit.native.prevent="doReset">
        <el-form-item label="用户名"><el-input :value="resetTarget.username" disabled /></el-form-item>
        <el-form-item label="新密码"><el-input value="123456" disabled /></el-form-item>
      </el-form>
      <div class="text-muted">确定将该用户的密码重置为 <b>123456</b> 吗？</div>
      <span slot="footer">
        <el-button @click="resetVisible=false">取消</el-button>
        <el-button type="primary" @click="doReset">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'UserManage',
  data() {
    return {
      keyword: '', list: [], loading: false, roleOptions: [],
      addVisible: false, addForm: { username: '', password: '', roleIds: [] },
      editVisible: false, editTarget: {}, editRoleIds: [],
      resetVisible: false, resetTarget: {}
    }
  },
  computed: {
    isAdmin() {
      try {
        const u = JSON.parse(localStorage.getItem('canteen_user') || 'null')
        if (!u) return false
        if (u.username === 'admin') return true
        return (u.roles || []).indexOf('admin') !== -1
      } catch (e) { return false }
    }
  },
  created() { this.loadList(); this.loadRoles() },
  methods: {
    loadList() {
      this.loading = true
      api.get('/api/user/list', { username: this.keyword || undefined })
        .then(d => { this.list = d || [] })
        .catch(() => {})
        .finally(() => { this.loading = false })
    },
    loadRoles() {
      api.get('/api/role/list')
        .then(d => { this.roleOptions = d || [] })
        .catch(() => {})
    },
    openAdd() {
      this.addVisible = true
      this.addForm = { username: '', password: '', roleIds: [] }
    },
    doAdd() {
      if (!this.addForm.username.trim()) return this.$message.warning('请输入用户名')
      if (!this.addForm.password) return this.$message.warning('请输入密码')
      api.post('/api/user/add', {
        username: this.addForm.username.trim(),
        password: this.addForm.password,
        roles: this.addForm.roleIds
      })
        .then(() => { this.$message.success('新增成功'); this.addVisible = false; this.loadList() })
        .catch(() => {})
    },
    openEdit(row) {
      this.editTarget = row
      this.editRoleIds = []
      // 通过角色名映射到角色id
      const names = row.roles || []
      this.editRoleIds = this.roleOptions.filter(r => names.indexOf(r.roleName) >= 0).map(r => r.id)
      this.editVisible = true
    },
    doEdit() {
      api.post('/api/user/updateRole', { id: this.editTarget.id, roles: this.editRoleIds })
        .then(() => { this.$message.success('已保存'); this.editVisible = false; this.loadList() })
        .catch(() => {})
    },
    openReset(row) { this.resetTarget = row; this.resetVisible = true },
    doReset() {
      api.post('/api/user/resetPwd', { id: this.resetTarget.id, password: '123456' })
        .then(() => { this.$message.success('密码已重置为 123456'); this.resetVisible = false; this.loadList() })
        .catch(() => {})
    },
    delUser(row) {
      this.$confirm('确定注销用户「' + row.username + '」？注销后该账号将被物理删除。', '注销确认', { type: 'warning' })
        .then(() => api.del('/api/user/' + row.id))
        .then(() => { this.$message.success('已注销'); this.loadList() })
        .catch(() => {})
    }
  }
}
</script>
