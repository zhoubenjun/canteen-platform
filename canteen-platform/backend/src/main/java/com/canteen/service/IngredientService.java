package com.canteen.service;

import com.canteen.dto.PageResult;
import com.canteen.entity.*;
import com.canteen.mapper.*;
import com.canteen.util.ExcelUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class IngredientService {

    @Resource private IngredientMapper ingredientMapper;
    @Resource private IngredientCategoryMapper categoryMapper;
    @Resource private SupplierMapper supplierMapper;
    @Resource private PriceLogMapper priceLogMapper;
    @Resource private PriceHistoryMapper priceHistoryMapper;

    // ---------------- 食材 CRUD ----------------
    public PageResult<Ingredient> page(String keyword, Integer categoryId, Integer supplierId, int page, int size) {
        int offset = (page - 1) * size;
        long total = ingredientMapper.count(keyword, categoryId, supplierId);
        List<Ingredient> rows = ingredientMapper.page(keyword, categoryId, supplierId, offset, size);
        return new PageResult<>(total, rows);
    }

    public List<Ingredient> listAll() {
        return ingredientMapper.listAll();
    }

    public Ingredient findById(Integer id) {
        return ingredientMapper.findById(id);
    }

    /** 新增：记录价格日记(新增) */
    @Transactional
    public Ingredient add(Ingredient i) {
        if (i.getUnit() == null || i.getUnit().isEmpty()) i.setUnit("KG");
        if (i.getWeightPerUnit() == null) i.setWeightPerUnit(BigDecimal.ONE);
        ingredientMapper.insert(i);
        writeLog(i, BigDecimal.ZERO, i.getPrice(), "新增");
        return i;
    }

    /** 修改：记录价格日记(修改) */
    @Transactional
    public Ingredient update(Ingredient i) {
        Ingredient old = ingredientMapper.findById(i.getId());
        BigDecimal oldPrice = old == null || old.getPrice() == null ? BigDecimal.ZERO : old.getPrice();
        ingredientMapper.update(i);
        if (oldPrice.compareTo(i.getPrice()) != 0) {
            writeLog(i, oldPrice, i.getPrice(), "修改");
        }
        return i;
    }

    /** 删除：记录价格日记(删除) */
    @Transactional
    public void delete(Integer id) {
        Ingredient old = ingredientMapper.findById(id);
        if (old != null) {
            writeLog(old, old.getPrice(), BigDecimal.ZERO, "删除");
            ingredientMapper.delete(id);
        }
    }

    @Transactional
    public void deleteByIds(List<Integer> ids) {
        for (Integer id : ids) {
            Ingredient old = ingredientMapper.findById(id);
            if (old != null) {
                writeLog(old, old.getPrice(), BigDecimal.ZERO, "删除");
            }
        }
        ingredientMapper.deleteByIds(ids);
    }

    @Transactional
    public void deleteAll() {
        for (Ingredient old : ingredientMapper.listAll()) {
            writeLog(old, old.getPrice(), BigDecimal.ZERO, "删除");
        }
        ingredientMapper.deleteAll();
    }

    private void writeLog(Ingredient i, BigDecimal oldP, BigDecimal newP, String type) {
        PriceLog log = new PriceLog();
        log.setIngredientId(i.getId());
        log.setIngredientName(i.getName());
        IngredientCategory c = i.getCategoryId() == null ? null : categoryMapper.findById(i.getCategoryId());
        log.setCategoryName(c == null ? "" : c.getName());
        Supplier s = i.getSupplierId() == null ? null : supplierMapper.findById(i.getSupplierId());
        log.setSupplierName(s == null ? "" : s.getName());
        log.setOldPrice(oldP);
        log.setNewPrice(newP);
        BigDecimal pct = BigDecimal.ZERO;
        if (oldP.compareTo(BigDecimal.ZERO) != 0) {
            pct = newP.subtract(oldP).divide(oldP, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        }
        log.setChangePct(pct);
        log.setChangeType(type);
        priceLogMapper.insert(log);
    }

    // ---------------- 价格日记 ----------------
    public List<PriceLog> queryLog(Integer ingredientId, Integer categoryId, Integer supplierId,
                                   String changeType, String start, String end) {
        java.util.Date sd = parseDate(start);
        java.util.Date ed = parseDate(end);
        return priceLogMapper.query(ingredientId, categoryId, supplierId, changeType, sd, ed);
    }

    public List<Map<String, Object>> history(Integer ingredientId) {
        return priceLogMapper.history(ingredientId);
    }

    public Map<String, Object> stats() {
        Map<String, Object> s = priceLogMapper.stats();
        if (s == null) s = new HashMap<>();
        // 涨幅最大食材
        List<PriceLog> logs = priceLogMapper.query(null, null, null, "修改", null, null);
        String maxName = "-"; BigDecimal maxPct = BigDecimal.ZERO;
        for (PriceLog l : logs) {
            if (l.getChangePct() != null && l.getChangePct().compareTo(maxPct) > 0) {
                maxPct = l.getChangePct(); maxName = l.getIngredientName();
            }
        }
        s.put("maxName", maxName);
        s.put("maxPct", maxPct);
        return s;
    }

    public byte[] exportLog(List<PriceLog> logs) throws Exception {
        String[] headers = {"食材名","类别","供应商","原价","新价","涨幅%","操作类型","时间"};
        List<List<Object>> data = new ArrayList<>();
        for (PriceLog l : logs) {
            data.add(Arrays.asList(l.getIngredientName(), l.getCategoryName(), l.getSupplierName(),
                    l.getOldPrice(), l.getNewPrice(), l.getChangePct(), l.getChangeType(),
                    l.getCreatedAt() == null ? "" : l.getCreatedAt()));
        }
        return ExcelUtil.export(headers, data);
    }

    // ---------------- 历史版本快照 ----------------
    @Transactional
    public PriceHistory saveSnapshot(String versionName) throws Exception {
        List<Ingredient> list = ingredientMapper.listAll();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Ingredient ing = list.get(i);
            sb.append("{\"id\":").append(ing.getId())
              .append(",\"name\":\"").append(esc(ing.getName()))
              .append("\",\"categoryId\":").append(ing.getCategoryId() == null ? 0 : ing.getCategoryId())
              .append(",\"supplierId\":").append(ing.getSupplierId() == null ? 0 : ing.getSupplierId())
              .append(",\"spec\":\"").append(esc(ing.getSpec()))
              .append("\",\"price\":").append(ing.getPrice() == null ? 0 : ing.getPrice())
              .append(",\"unit\":\"").append(esc(ing.getUnit()))
              .append("\",\"priceUnit\":\"").append(esc(ing.getPriceUnit()))
              .append("\",\"weightPerUnit\":").append(ing.getWeightPerUnit() == null ? 1 : ing.getWeightPerUnit())
              .append("}");
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        PriceHistory h = new PriceHistory();
        h.setVersionName(versionName == null || versionName.isEmpty() ? ("快照-" + new java.text.SimpleDateFormat("yyyyMMddHHmm").format(new Date())) : versionName);
        h.setSnapshotDate(new Date());
        h.setDataJson(sb.toString());
        priceHistoryMapper.insert(h);
        return h;
    }

    @Transactional
    public void loadSnapshot(Integer id) {
        PriceHistory h = priceHistoryMapper.findById(id);
        if (h == null) throw new RuntimeException("版本不存在");
        String json = h.getDataJson();
        // 简易解析: 用Jackson
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            List<com.fasterxml.jackson.databind.JsonNode> nodes = om.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<com.fasterxml.jackson.databind.JsonNode>>() {});
            for (com.fasterxml.jackson.databind.JsonNode n : nodes) {
                Ingredient cur = ingredientMapper.findById(n.get("id").asInt());
                BigDecimal price = new BigDecimal(n.get("price").asText());
                if (cur == null) continue;
                if (cur.getPrice() != null && cur.getPrice().compareTo(price) != 0) {
                    Ingredient update = new Ingredient();
                    update.setId(cur.getId()); update.setPrice(price);
                    // 仅更新价格，其余保留
                    ingredientMapper.updatePriceOnly(cur.getId(), price);
                    writeLog(cur, cur.getPrice(), price, "修改");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("版本载入失败: " + e.getMessage());
        }
    }

    public List<PriceHistory> listSnapshots() {
        return priceHistoryMapper.listAll();
    }

    public void deleteSnapshot(Integer id) {
        priceHistoryMapper.delete(id);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private java.util.Date parseDate(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(s); }
        catch (Exception e) { return null; }
    }

    // ---------------- Excel 批量导入导出食材 ----------------
    public byte[] exportIngredients() throws Exception {
        String[] headers = {"名称","类别","供应商","规格","单价","单位","计价单位","单件重量(克)"};
        List<List<Object>> data = new ArrayList<>();
        for (Ingredient i : ingredientMapper.listAll()) {
            data.add(Arrays.asList(i.getName(), i.getCategoryName(), i.getSupplierName(), i.getSpec(),
                    i.getPrice(), i.getUnit(), i.getPriceUnit(), i.getWeightPerUnit()));
        }
        return ExcelUtil.export(headers, data);
    }

    /** 批量导入：名称/类别/供应商/规格/单价。类别与供应商不存在时自动创建 */
    @Transactional
    public Map<String, Object> importIngredients(byte[] bytes) throws Exception {
        List<Map<String, String>> rows = ExcelUtil.read(bytes);
        int ok = 0; int skip = 0; List<String> errors = new ArrayList<>();
        for (Map<String, String> row : rows) {
            try {
                String name = get(row, "名称"); if (name.isEmpty()) { skip++; continue; }
                String catName = get(row, "类别");
                String supName = get(row, "供应商");
                Ingredient i = new Ingredient();
                i.setName(name);
                if (!catName.isEmpty()) {
                    IngredientCategory c = categoryMapper.findByName(catName);
                    if (c == null) { c = new IngredientCategory(); c.setName(catName); c.setSort(99); categoryMapper.insert(c); }
                    i.setCategoryId(c.getId());
                }
                if (!supName.isEmpty()) {
                    Supplier s = supplierMapper.findByName(supName);
                    if (s == null) { s = new Supplier(); s.setName(supName); supplierMapper.insert(s); }
                    i.setSupplierId(s.getId());
                }
                i.setSpec(get(row, "规格"));
                i.setPrice(ExcelUtil.toDecimal(get(row, "单价"), BigDecimal.ZERO));
                String unit = get(row, "单位"); i.setUnit(unit.isEmpty() ? "KG" : unit);
                i.setPriceUnit(get(row, "计价单位"));
                i.setWeightPerUnit(ExcelUtil.toDecimal(get(row, "单件重量(克)"), BigDecimal.ONE));
                Ingredient exist = ingredientMapper.findByName(name);
                if (exist == null) {
                    add(i);
                    ok++;
                } else {
                    i.setId(exist.getId());
                    update(i);
                    ok++;
                }
            } catch (Exception e) {
                errors.add(get(row, "名称") + ":" + e.getMessage());
            }
        }
        Map<String, Object> res = new HashMap<>();
        res.put("ok", ok); res.put("skip", skip); res.put("errors", errors);
        return res;
    }

    // ---------------- 类别 / 供应商 ----------------
    public List<IngredientCategory> categories() { return categoryMapper.listAll(); }
    public IngredientCategory addCategory(String name) {
        IngredientCategory c = new IngredientCategory(); c.setName(name); c.setSort(99);
        categoryMapper.insert(c); return c;
    }
    public void updateCategory(IngredientCategory c) { categoryMapper.update(c); }
    public void deleteCategory(Integer id) { categoryMapper.delete(id); }

    public List<Supplier> suppliers() { return supplierMapper.listAll(); }
    public Supplier addSupplier(String name) { Supplier s = new Supplier(); s.setName(name); supplierMapper.insert(s); return s; }
    public void updateSupplier(Supplier s) { supplierMapper.update(s); }
    public void deleteSupplier(Integer id) { supplierMapper.delete(id); }

    private String get(Map<String, String> row, String key) {
        String v = row.get(key); return v == null ? "" : v.trim();
    }
}
