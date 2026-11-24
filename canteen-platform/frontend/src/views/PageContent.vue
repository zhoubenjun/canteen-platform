<template>
  <div class="page-wrap">
    <el-card class="box" shadow="never">
      <div slot="header" class="box-head">
        <span class="title">页面内容管理</span>
        <span class="tip">维护登录页“法律声明”等静态页面的标题与正文，保存后立即生效，无需重新打包</span>
      </div>

      <el-row :gutter="18">
        <el-col :span="6">
          <div class="list-title">页面列表</div>
          <div
            v-for="p in pages"
            :key="p.pageKey"
            class="page-item"
            :class="{ active: p.pageKey === currentKey }"
            @click="select(p)"
          >
            <div class="pi-name">{{ p.pageTitle }}</div>
            <div class="pi-key">#/{{ p.pageKey }}</div>
            <div class="pi-time">{{ p.updatedAt || '未编辑' }}</div>
          </div>
          <div v-if="!pages.length" class="pi-empty">暂无页面内容</div>
        </el-col>

        <el-col :span="18">
          <el-form label-width="72px" size="small">
            <el-form-item label="页面标题">
              <el-input v-model="form.title" maxlength="60" show-word-limit placeholder="如：法律声明" @input="dirty = true" />
            </el-form-item>
            <el-form-item label="正文">
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="18"
                resize="vertical"
                placeholder="每行一个段落；以“- ”开头的行会按二级缩进显示"
                @input="dirty = true"
              />
              <div class="hint">
                排版说明：每行一段，自动首行缩进 2 字符；行首加 “- ”（减号加空格）表示二级缩进子项文；空行会被忽略。
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="save">保存</el-button>
              <el-button @click="preview">预览页面</el-button>
              <span v-if="dirty" class="dirty">有未保存的修改</span>
            </el-form-item>
          </el-form>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'PageContent',
  data() {
    return {
      pages: [],
      currentKey: 'legal',
      form: { title: '', content: '' },
      saving: false,
      dirty: false
    }
  },
  mounted() {
    this.loadList()
  },
  methods: {
    async loadList() {
      try {
        const list = await api.get('/api/page/list')
        this.pages = list || []
        if (!this.pages.length) return
        const target = this.pages.find(p => p.pageKey === this.currentKey) || this.pages[0]
        this.apply(target)
      } catch (e) {
        // 提示已由拦截器统一处理
      }
    },
    apply(p) {
      this.currentKey = p.pageKey
      this.form.title = p.pageTitle || ''
      this.form.content = p.content || ''
      this.dirty = false
    },
    select(p) {
      if (this.dirty) {
        this.$confirm('当前修改尚未保存，确定切换页面吗？', '提示', { type: 'warning' })
          .then(() => this.apply(p))
          .catch(() => {})
      } else {
        this.apply(p)
      }
    },
    async save() {
      if (!this.form.title.trim()) return this.$message.warning('请填写页面标题')
      if (!this.form.content.trim()) return this.$message.warning('请填写正文')
      this.saving = true
      try {
        await api.put('/api/page/' + this.currentKey, {
          title: this.form.title.trim(),
          content: this.form.content
        })
        this.$message.success('保存成功')
        this.dirty = false
        this.loadList()
      } catch (e) {
        // 提示已由拦截器统一处理
      } finally {
        this.saving = false
      }
    },
    preview() {
      window.open('/index.html?bust=' + Date.now() + '#/' + this.currentKey, '_blank')
    }
  }
}
</script>

<style scoped>
.page-wrap { padding: 14px 18px 24px; }
.box-head { display: flex; align-items: baseline; gap: 12px; }
.box-head .title { font-size: 16px; font-weight: 700; color: #1c3f73; }
.box-head .tip { font-size: 12px; color: #8a97a8; }
.list-title { font-size: 13px; font-weight: 700; color: #37517a; margin-bottom: 8px; }
.page-item { padding: 10px 12px; border: 1px solid #e3e9f2; border-radius: 6px; margin-bottom: 8px; cursor: pointer; background: #fbfcfe; }
.page-item.active { border-color: #3f7ad1; background: #f2f7ff; box-shadow: 0 1px 6px rgba(63, 122, 209, .18); }
.pi-name { font-size: 14px; font-weight: 600; color: #22334d; }
.pi-key { font-size: 12px; color: #6f839c; margin-top: 2px; }
.pi-time { font-size: 12px; color: #98a6b8; margin-top: 2px; }
.pi-empty { font-size: 12px; color: #98a6b8; }
.hint { font-size: 12px; color: #8a97a8; line-height: 1.7; margin-top: 6px; }
.dirty { color: #d38a1e; font-size: 12px; margin-left: 8px; }
</style>
