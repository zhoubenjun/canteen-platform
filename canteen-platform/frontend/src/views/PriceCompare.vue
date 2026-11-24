<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="按食材名称查询" clearable style="width:240px" @keyup.enter.native="load" @clear="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <div class="spacer" style="flex:1"></div>
        <a class="source-link" href="http://fgw.shiyan.gov.cn/xxgk/fdzdgknr/tjsj/jgjcsj/" target="_blank" rel="noopener noreferrer">十堰市发改委食材价格参考</a>
      </div>

      <el-table :data="pagedRows" border stripe v-loading="loading" style="width:100%">
        <el-table-column type="index" label="序号" width="60" align="center" :index="seqIndex" />
        <el-table-column prop="ingredientName" label="食材名称" min-width="120" show-overflow-tooltip />
        <el-table-column label="供应商" min-width="200" show-overflow-tooltip>
          <template slot-scope="{row}">{{ row.supplierName || '—' }}</template>
        </el-table-column>
        <el-table-column label="单价（元/kg）" min-width="120" align="right">
          <template slot-scope="{row}">{{ money(priceKg(row)) }}</template>
        </el-table-column>
        <el-table-column label="单位" width="60" align="center">kg</el-table-column>
        <el-table-column label="市场平均价（元/kg）" min-width="140" align="right">
          <template slot-scope="{row}">{{ money(row.avgPrice) }}</template>
        </el-table-column>
        <el-table-column label="市场最高价（元/kg）" min-width="140" align="right">
          <template slot-scope="{row}">{{ money(row.maxPrice) }}</template>
        </el-table-column>
        <el-table-column label="市场最低价（元/kg）" min-width="140" align="right">
          <template slot-scope="{row}">{{ money(row.minPrice) }}</template>
        </el-table-column>
        <el-table-column label="较市场均价" min-width="110" align="center">
          <template slot-scope="{row}">
            <span v-if="gapPct(row) === null" class="text-muted">—</span>
            <span v-else :class="Number(gapPct(row)) > 0 ? 'gap-up' : (Number(gapPct(row)) < 0 ? 'gap-down' : 'gap-same')">
              {{ gapPct(row) }}%
            </span>
          </template>
        </el-table-column>
      </el-table>

      <div class="flex" style="justify-content:flex-end;margin-top:10px">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="rows.length"
                       :page-sizes="[10, 20, 50, 100]" :page-size="pageSize" :current-page.sync="page"
                       @size-change="onSizeChange" />
      </div>
    </div>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'PriceCompare',
  data() {
    return { keyword: '', rows: [], loading: false, page: 1, pageSize: 10 }
  },
  created() { this.load() },
  computed: {
    pagedRows() {
      const start = (this.page - 1) * this.pageSize
      return this.rows.slice(start, start + this.pageSize)
    }
  },
  methods: {
    money(v) { return v == null || v === '' ? '—' : '￥' + Number(v).toFixed(2) },
    seqIndex(i) { return (this.page - 1) * this.pageSize + i + 1 },
    onSizeChange(size) { this.pageSize = size; this.page = 1 },
    // 统一折算成元/kg：计价重量为空按 1000g 计
    priceKg(row) {
      if (row.price == null) return null
      const wpu = Number(row.weightPerUnit) > 0 ? Number(row.weightPerUnit) : 1000
      return Number(row.price) * 1000 / wpu
    },
    gapPct(row) {
      const p = this.priceKg(row)
      const avg = row.avgPrice == null ? null : Number(row.avgPrice)
      if (p == null || avg == null || avg <= 0) return null
      return ((p - avg) / avg * 100).toFixed(2)
    },
    async load() {
      this.loading = true
      this.page = 1
      try {
        const list = (await api.get('/api/inquiry/veg-board', { keyword: this.keyword })) || []
        // 按较市场均价从高到低排列；无市场询价的食材排在最后（组内按名称、id）
        list.sort((a, b) => {
          const ga = this.gapPct(a); const gb = this.gapPct(b)
          if (ga === null && gb === null) {
            const c = String(a.ingredientName).localeCompare(String(b.ingredientName), 'zh')
            return c !== 0 ? c : (a.ingredientId || 0) - (b.ingredientId || 0)
          }
          if (ga === null) return 1
          if (gb === null) return -1
          const d = Number(gb) - Number(ga)
          return d !== 0 ? d : String(a.ingredientName).localeCompare(String(b.ingredientName), 'zh')
        })
        this.rows = list
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.gap-up { color: #c0392b; font-weight: 700; }
.gap-down { color: #1e8e3e; font-weight: 700; }
.gap-same { color: #666; }
.source-link {
  font-size: 14px;
  color: #1a73e8;
  text-decoration: none;
  white-space: nowrap;
}
.source-link:hover { text-decoration: underline; }
</style>
