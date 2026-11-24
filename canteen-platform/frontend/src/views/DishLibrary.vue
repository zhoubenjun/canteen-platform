<template>
  <div class="page">
    <!-- 成本核算设置 -->
    <div class="page-card">
      <div class="flex" style="flex-wrap:wrap; gap:14px;">
        <span class="section-title">成本核算参数</span>
        <span class="flex"><label>食材入口率</label><el-input-number v-model="cost.entranceRate" :precision="2" :step="0.05" :min="0.01" :max="1" style="width:130px" /></span>
        <span class="flex"><label>劳务费比例</label><el-input-number v-model="cost.laborRate" :precision="2" :step="0.01" :min="0" :max="1" style="width:130px" /></span>
        <span class="flex"><label>其他费用比例</label><el-input-number v-model="cost.otherRate" :precision="2" :step="0.01" :min="0" :max="1" style="width:130px" /></span>
        <span class="flex"><label>调料费/份</label><el-input-number v-model="cost.seasoningCost" :precision="2" :step="0.1" :min="0" style="width:130px" /></span>
        <el-button type="primary" @click="saveCost">保存核算参数</el-button>
        <el-button @click="recalcAll">重新核算全部</el-button>
      </div>
    </div>

    <!-- 工具条 -->
    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索菜名" clearable style="width:160px" @keyup.enter.native="load" />
        <el-input v-model="query.ingredientKeyword" placeholder="按食材名称搜索" clearable style="width:160px" @keyup.enter.native="load" />
        <el-select v-model="query.category" placeholder="按类别筛选" clearable style="width:130px" @change="load">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-checkbox v-model="query.unPriced" @change="load">有配料未定价</el-checkbox>
        <el-select v-model="query.recipeDay" placeholder="按食谱天筛选" clearable style="width:150px" @change="loadRecipe">
          <el-option v-for="d in dayOptions" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
        <el-select v-model="query.recipeMeal" placeholder="餐次" clearable style="width:110px" @change="loadRecipe" :disabled="!query.recipeDay">
          <el-option label="早餐" value="早餐" /><el-option label="中餐" value="中餐" /><el-option label="晚餐" value="晚餐" />
        </el-select>
        <div class="spacer" style="flex:1"></div>
        <el-button type="primary" @click="openAdd">新增菜品</el-button>
        <el-button type="primary" plain @click="exportExcel">导出菜品</el-button>
        <el-upload :show-file-list="false" :http-request="importExcel" accept=".xlsx">
          <el-button type="primary" plain>导入菜品</el-button>
        </el-upload>
      </div>
    </div>

    <!-- 菜品卡片 -->
    <div class="dish-grid" v-loading="loading">
      <div v-for="d in list" :key="d.id" class="dish-card">
        <div class="dish-info">
          <div class="flex" style="justify-content:space-between">
            <span class="d-name">{{ d.code }}. {{ d.name }}</span>
            <el-tag size="mini" :type="catType(d.category)">{{ d.category || '未分类' }}</el-tag>
          </div>
          <div class="d-meta">配料：{{ d.ingredientsText || '—' }}</div>
          <div class="d-meta">
            建议价 <b style="color:#1e56ab">¥{{ money(d.suggestPrice) }}</b>
            &nbsp;审核价 <b style="color:#2f6fd0">¥{{ money(d.auditPrice) }}</b>
            <span v-if="diffMark(d)" v-html="diffMark(d)"></span>
          </div>
          <div class="d-meta" v-if="d.excludeSeasoning === 1">[不含调料]</div>
        </div>
        <div class="dish-img-wrap">
          <img v-if="d.image" :src="d.image" class="dish-img" alt="" />
          <img v-else :src="defaultImg(d.category)" class="dish-img" alt="" />
        </div>
        <div class="dish-actions" style="display:flex;justify-content:center;gap:8px;margin-top:8px">
          <el-button type="primary" size="mini" @click.stop="openEdit(d)">编辑</el-button>
          <el-button type="danger" size="mini" @click.stop="delDish(d)">删除</el-button>
        </div>
      </div>
    </div>
    <div v-if="!list.length && !loading" class="text-muted" style="text-align:center;padding:30px">暂无菜品</div>

    <div class="flex" style="justify-content:flex-end;margin-top:14px">
      <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="total" :page-sizes="[12, 24, 60, 120]" :page-size="query.size" :current-page.sync="query.page" @size-change="onSizeChange" @current-change="load" />
    </div>

    <!-- 新增/编辑菜品 -->
    <el-dialog :title="form.id ? '编辑菜品' : '新增菜品'" :visible.sync="dialogVisible" width="640px" @open="onDialogOpen">
      <el-form :model="form" label-width="90px">
        <el-form-item label="菜名" required>
          <el-input v-model="form.name" placeholder="请输入菜名" />
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="form.category" placeholder="早餐可不设" clearable style="width:200px">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="排除调料">
          <el-switch v-model="form.excludeSeasoning" :active-text="'无需调料(默认调料费用' + seasoningYuan + '元)'" />
        </el-form-item>
        <el-form-item label="审核价格">
          <el-input-number v-model="form.auditPrice" :precision="2" :min="0" style="width:160px" />
          <span class="text-muted" style="margin-left:8px">建议售价 ¥{{ money(form.suggestPrice) }}</span>
        </el-form-item>
        <el-form-item label="配料">
          <div v-for="(ing, idx) in form.ingredients" :key="idx" class="flex" style="margin-bottom:6px">
            <el-select v-model="ing.ingredientId" filterable placeholder="选择食材(下拉防止名称不一致)" style="width:230px" @change="v => onIngChange(ing, v)">
              <el-option v-for="i in ingredients" :key="i.id" :label="i.name" :value="i.id" />
            </el-select>
            <el-input-number v-model="ing.weightG" :min="0" :precision="0" style="width:110px" placeholder="克重" />
            <span class="text-muted">克</span>
            <el-button type="text" @click="form.ingredients.splice(idx,1)">删除</el-button>
          </div>
          <el-button size="mini" @click="addIng">+ 添加配料</el-button>
        </el-form-item>
        <el-form-item label="成本估算">
          <div class="cost-estimate">
            当前食材成本估算：<b style="color:#2f6fd0">¥{{ money(currentCost.total) }} 元/份</b>&nbsp;&nbsp;(含调料 ¥{{ money(currentCost.seasoning) }} 元)
          </div>
          <div class="cost-formula">
            <div class="cf-line">成本 = {{ costFormula.rule }}</div>
            <div class="cf-line cf-calc">成本 = {{ costFormula.calc }}</div>
          </div>
        </el-form-item>
        <el-form-item label="菜品图片">
          <div class="flex">
            <el-upload :show-file-list="false" :http-request="uploadImg" accept="image/*">
              <img v-if="form.image" :src="form.image" style="width:100px;height:75px;object-fit:cover;border-radius:6px" alt="" />
              <el-button v-else size="small">上传图片</el-button>
            </el-upload>
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'DishLibrary',
  data() {
    return {
      list: [], total: 0, loading: false,
      categories: ['鲜荤', '冻荤', '汤食', '素菜', '主食', '早餐'],
      ingredients: [], ingredientByName: {},
      weekKey: '',
      dayOptions: [{ label: '周一', value: 0 }, { label: '周二', value: 1 }, { label: '周三', value: 2 }, { label: '周四', value: 3 }, { label: '周五', value: 4 }, { label: '周六', value: 5 }, { label: '周日', value: 6 }],
      query: { keyword: '', ingredientKeyword: '', category: '', unPriced: false, recipeDay: null, recipeMeal: '', page: 1, size: 12 },
      cost: { entranceRate: 0.9, laborRate: 0.1, otherRate: 0.05, seasoningCost: 0.5 },
      dialogVisible: false,
      form: { id: null, name: '', category: '', excludeSeasoning: false, auditPrice: 0, ingredients: [], image: '' }
    }
  },
  created() {
    this.loadCost()
    this.loadIngredients()
    api.get('/api/recipe/currentMonday').then(d => { this.weekKey = d.weekKey })
    this.load()
  },
  computed: {
    // 默认调料费（取成本设置里的“调料费/份”，默认 0.5 元，去掉多余小数位）
    seasoningYuan() {
      const n = Number(this.cost.seasoningCost === undefined || this.cost.seasoningCost === null ? 0.5 : this.cost.seasoningCost)
      return String(Math.round(n * 100) / 100)
    },
    currentCost() {
      let mat = 0
      for (const ing of this.form.ingredients || []) {
        const it = (this.ingredients || []).find(x => x.id === ing.ingredientId)
        if (it && ing.weightG) {
          const wpu = Number(it.weightPerUnit) || 1000
          mat += Number(ing.weightG) / wpu * Number(it.price || 0)
        }
      }
      const seasoning = this.form.excludeSeasoning ? 0 : Number(this.cost.seasoningCost || 0)
      const total = mat + seasoning
      return { mat, seasoning, total }
    },
    costFormula() {
      // 第一行：计算规则；第二行：代入实际数据的列式计算（含劳务费/其他费/入口率）
      const terms = []
      let mat = 0
      for (const ing of this.form.ingredients || []) {
        const it = (this.ingredients || []).find(x => x.id === ing.ingredientId)
        if (it && ing.weightG) {
          const wpu = Number(it.weightPerUnit) || 1000
          const price = Number(it.price || 0)
          const w = Number(ing.weightG)
          mat += w / wpu * price
          terms.push(`(${it.name} ${w}g ÷ ${wpu}g × ¥${this.money(price)})`)
        }
      }
      const seasoning = this.form.excludeSeasoning ? 0 : Number(this.cost.seasoningCost || 0)
      const laborRate = Number(this.cost.laborRate || 0)
      const otherRate = Number(this.cost.otherRate || 0)
      const entranceRate = Number(this.cost.entranceRate) || 0
      const ingredientCost = mat + seasoning
      const totalCost = ingredientCost * (1 + laborRate + otherRate)
      const suggest = entranceRate > 0 ? totalCost / entranceRate : 0

      const rule = '[Σ(用量 ÷ 单位克重 × 单价) + 调料费] × (1 + 劳务费比例 + 其他费比例) ÷ 入口率'
      let inner = terms.length ? terms.join(' + ') : ''
      if (seasoning > 0) inner = (inner ? inner + ' + ' : '') + `调料费 ¥${this.money(seasoning)}`
      if (!inner) return { rule, calc: '—' }
      const calc = `[${inner}] × (1 + ${this.money(laborRate)} + ${this.money(otherRate)}) ÷ ${this.money(entranceRate)} = ¥${this.money(suggest)}`
      return { rule, calc }
    }
  },
  methods: {
    onSizeChange(sz) { this.query.size = sz; this.query.page = 1; this.load() },
    async load() {
      this.loading = true
      try {
        const d = await api.get('/api/dish/page', this.query)
        this.list = d.rows; this.total = d.total
      } finally { this.loading = false }
    },
    async loadCost() {
      const c = await api.get('/api/dish/costSettings')
      this.cost = c
    },
    async loadIngredients() {
      const all = await api.get('/api/ingredient/all')
      // 同名食材有多条时只保留第一条（id 最小），配料下拉只能选到它
      const seen = {}; const list = []
      ;(all || []).slice().sort((a, b) => (a.id || 0) - (b.id || 0)).forEach(i => {
        if (!seen[i.name]) { seen[i.name] = i; list.push(i) }
      })
      this.ingredients = list
      this.ingredientByName = seen
    },
    async loadRecipe() {
      if (this.query.recipeDay === null || this.query.recipeDay === undefined || this.query.recipeDay === '') { this.load(); return }
      const recs = await api.get('/api/recipe/list', { weekKey: this.weekKey })
      const ids = new Set()
      recs.filter(r => r.dayIndex === this.query.recipeDay && (!this.query.recipeMeal || r.mealType === this.query.recipeMeal))
          .forEach(r => ids.add(r.dishId))
      const all = await api.get('/api/dish/byRecipe', { weekKey: this.weekKey, mealType: this.query.recipeMeal || undefined })
      this.list = all.filter(d => ids.has(d.id))
      this.total = this.list.length
    },
    async saveCost() {
      await api.post('/api/dish/costSettings', this.cost)
      this.$message.success('核算参数已保存，已重新核算')
      this.load()
    },
    async recalcAll() {
      await api.post('/api/dish/recalcAll')
      this.$message.success('已重新核算全部菜品成本')
      this.load()
    },
    catType(c) {
      return { '鲜荤': 'danger', '冻荤': 'primary', '汤食': 'warning', '素菜': 'success', '主食': 'info' }[c] || 'info'
    },
    defaultImg(cat) {
      return '/static/default-' + ({ '鲜荤': 'meat', '冻荤': 'frozen', '汤食': 'soup', '主食': 'staple' }[cat] || 'veg') + '.png'
    },
    money(v) { return (v || 0) === 0 ? '0.00' : Number(v).toFixed(2) },
    diffMark(d) {
      if (!d.auditPrice || !d.suggestPrice || d.suggestPrice <= 0) return ''
      const ratio = (d.auditPrice - d.suggestPrice) / d.suggestPrice
      const abs = Math.abs(ratio)
      if (abs > 0.5) return '<span class="price-abnormal">⚠异常</span>'
      if (ratio > 0.1) return '<span class="price-up">↑卖贵了</span>'
      if (ratio < -0.1) return '<span class="price-down">↓卖低了</span>'
      return ''
    },
    openAdd() {
      this.form = { id: null, name: '', category: '', excludeSeasoning: false, auditPrice: 0, ingredients: [], image: '' }
      this.dialogVisible = true
    },
    openEdit(d) {
      api.get('/api/dish/' + d.id).then(dish => {
        this.form = {
          id: dish.id, name: dish.name, category: dish.category || '',
          excludeSeasoning: dish.excludeSeasoning === 1, auditPrice: Number(dish.auditPrice || 0),
          ingredients: (dish.ingredients || []).map(i => ({
            ingredientId: this.firstIngredientId(i.ingredientName, i.ingredientId),
            ingredientName: i.ingredientName,
            weightG: Number(i.weightG)
          })),
          image: dish.image || '', suggestPrice: Number(dish.suggestPrice || 0)
        }
        if (!this.form.ingredients.length) this.addIng()
        this.dialogVisible = true
      })
    },
    onDialogOpen() { if (!this.form.ingredients.length) this.addIng() },
    addIng() { this.form.ingredients.push({ ingredientId: null, ingredientName: '', weightG: 100 }) },
    onIngChange(ing, id) {
      const it = this.ingredients.find(x => x.id === id)
      if (it) ing.ingredientName = it.name
    },
    // 同名食材有多条时统一落到第一条的 id
    firstIngredientId(name, fallbackId) {
      const first = name ? (this.ingredientByName || {})[name] : null
      return first ? first.id : fallbackId
    },
    async uploadImg({ file }) {
      const fd = new FormData()
      fd.append('file', file)
      const r = await api.upload('/api/dish/uploadImage', fd)
      this.form.image = r.url
    },
    async save() {
      const name = (this.form.name || '').trim()
      if (!name) { this.$message.warning('请输入菜名'); return }
      this.form.name = name
      const payload = { ...this.form, name, excludeSeasoning: this.form.excludeSeasoning ? 1 : 0 }
      try {
        await api.post('/api/dish/save', payload)
        this.$message.success('保存成功')
        this.dialogVisible = false
        this.load()
      } catch (e) {
        this.$alert((e && e.message) || '保存失败', '提示', { type: 'warning' })
      }
    },
    async delDish(d) {
      try {
        await this.$confirm(`确定删除菜品「${d.name}」吗？其关联配料将一并删除，删除后不可恢复。`, '删除确认',
          { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
      } catch (e) { return }
      await api.post('/api/dish/deleteBatch', [d.id])
      this.$message.success('删除成功')
      this.load()
    },
    async exportExcel() {
      const res = await api.blob('/api/dish/export')
      this.$download(res.data, '菜品库数据.xlsx')
    },
    async importExcel({ file }) {
      const fd = new FormData(); fd.append('file', file)
      const r = await api.upload('/api/dish/import', fd)
      this.$message.success('导入成功 ' + r.ok + ' 条' + (r.errors.length ? '，失败 ' + r.errors.length : ''))
      this.load()
    }
  }
}
</script>
