package com.canteen.service;

import com.canteen.dto.CostSettingsDto;
import com.canteen.dto.DishSaveDto;
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
public class DishService {

    @Resource private DishMapper dishMapper;
    @Resource private DishIngredientMapper dishIngredientMapper;
    @Resource private IngredientMapper ingredientMapper;
    @Resource private IngredientCategoryMapper categoryMapper;
    @Resource private SysSettingService settingService;
    @Resource private FileService fileService;

    /** 类别列表（用于筛选） */
    public List<String> categories() {
        return Arrays.asList("鲜荤", "冻荤", "素菜", "主食");
    }

    public PageResult<Dish> page(String keyword, String category, Boolean unPriced,
                                String ingredientKeyword, int page, int size) {
        int offset = (page - 1) * size;
        long total = dishMapper.count(keyword, category, unPriced, ingredientKeyword);
        List<Dish> rows = dishMapper.page(keyword, category, unPriced, ingredientKeyword, offset, size);
        fillIngredients(rows);
        return new PageResult<>(total, rows);
    }

    public List<Dish> listAll() {
        List<Dish> rows = dishMapper.listAll();
        fillIngredients(rows);
        return rows;
    }

    public Dish findById(Integer id) {
        Dish d = dishMapper.findById(id);
        if (d != null) d.setIngredients(dishIngredientMapper.listByDish(id));
        return d;
    }

    private void fillIngredients(List<Dish> dishes) {
        if (dishes.isEmpty()) return;
        List<Integer> ids = new ArrayList<>();
        for (Dish d : dishes) ids.add(d.getId());
        Map<Integer, List<DishIngredient>> map = new HashMap<>();
        for (DishIngredient di : dishIngredientMapper.listByDishIds(ids)) {
            map.computeIfAbsent(di.getDishId(), k -> new ArrayList<>()).add(di);
        }
        for (Dish d : dishes) {
            List<DishIngredient> ings = map.getOrDefault(d.getId(), Collections.emptyList());
            d.setIngredients(ings);
            StringBuilder sb = new StringBuilder();
            for (DishIngredient di : ings) {
                if (sb.length() > 0) sb.append("、");
                sb.append(di.getIngredientName()).append(" ").append(di.getWeightG()).append("g");
            }
            d.setIngredientsText(sb.toString());
        }
    }

    /** 保存菜品（含配料），并重算成本 */
    @Transactional
    public Dish save(DishSaveDto dto) {
        // trim 菜名
        String name = dto.getName() == null ? "" : dto.getName().trim();
        if (name.isEmpty()) throw new RuntimeException("菜名不能为空");
        // 唯一性校验（排除自己）
        for (Dish d : dishMapper.listAll()) {
            if (name.equals(d.getName() == null ? "" : d.getName().trim())
                && (dto.getId() == null || !d.getId().equals(dto.getId()))) {
                throw new RuntimeException("菜品「" + name + "」已存在，不能重复保存");
            }
        }
        dto.setName(name);
        Dish dish;
        if (dto.getId() == null) {
            dish = new Dish();
            dish.setName(dto.getName());
            dish.setCategory(dto.getCategory());
            Integer code = dishMapper.maxCode() + 1;
            dish.setCode(code);
            dish.setExcludeSeasoning(dto.getExcludeSeasoning() == null ? 0 : dto.getExcludeSeasoning());
            dish.setImage(dto.getImage());
            dish.setSeasoningCost(settingService.getDecimal("seasoning_cost", new BigDecimal("0.50")));
            dishMapper.insert(dish);
        } else {
            dish = dishMapper.findById(dto.getId());
            if (dish == null) throw new RuntimeException("菜品不存在");
            dish.setName(dto.getName());
            dish.setCategory(dto.getCategory());
            dish.setExcludeSeasoning(dto.getExcludeSeasoning() == null ? 0 : dto.getExcludeSeasoning());
            dish.setImage(dto.getImage());
            dishMapper.update(dish);
            dishIngredientMapper.deleteByDish(dish.getId());
        }
        // 保存配料（同名食材在食材库有多条时，统一取第一条）
        if (dto.getIngredients() != null) {
            for (DishSaveDto.IngredientItem item : dto.getIngredients()) {
                if (item.getIngredientId() == null) continue;
                Integer ingId = item.getIngredientId();
                String ingName = item.getIngredientName();
                if (ingName != null && !ingName.trim().isEmpty()) {
                    Ingredient first = ingredientMapper.findByName(ingName.trim());
                    if (first != null) { ingId = first.getId(); ingName = first.getName(); }
                }
                DishIngredient di = new DishIngredient();
                di.setDishId(dish.getId());
                di.setIngredientId(ingId);
                di.setIngredientName(ingName);
                di.setWeightG(item.getWeightG() == null ? BigDecimal.ZERO : item.getWeightG());
                dishIngredientMapper.insert(di);
            }
        }
        recalcCost(dish);
        // 审核价
        if (dto.getAuditPrice() != null) {
            Dish up = new Dish();
            up.setId(dish.getId());
            up.setAuditPrice(dto.getAuditPrice());
            dishMapper.updateAuditPrice(dish.getId(), dto.getAuditPrice());
            dish.setAuditPrice(dto.getAuditPrice());
        }
        return findById(dish.getId());
    }

    /** 成本核算 */
    public void recalcCost(Dish dish) {
        List<DishIngredient> ings = dishIngredientMapper.listByDish(dish.getId());
        BigDecimal entranceRate = settingService.getDecimal("entrance_rate", new BigDecimal("0.90"));
        BigDecimal laborRate = settingService.getDecimal("labor_rate", new BigDecimal("0.10"));
        BigDecimal otherRate = settingService.getDecimal("other_rate", new BigDecimal("0.05"));
        BigDecimal seasoning = dish.getExcludeSeasoning() != null && dish.getExcludeSeasoning() == 1
                ? BigDecimal.ZERO
                : settingService.getDecimal("seasoning_cost", new BigDecimal("0.50"));

        BigDecimal ingredientCost = BigDecimal.ZERO;
        for (DishIngredient di : ings) {
            // 同名食材在食材库有多条时，成本一律按第一条（id 最小）计价
            Ingredient ing = null;
            if (di.getIngredientName() != null && !di.getIngredientName().trim().isEmpty()) {
                ing = ingredientMapper.findByName(di.getIngredientName().trim());
            }
            if (ing == null) ing = ingredientMapper.findById(di.getIngredientId());
            if (ing == null || ing.getPrice() == null) continue;
            BigDecimal wpu = ing.getWeightPerUnit() == null ? BigDecimal.valueOf(1000) : ing.getWeightPerUnit();
            BigDecimal w = di.getWeightG() == null ? BigDecimal.ZERO : di.getWeightG();
            // cost = 克重 / 单位克重 * 单价
            BigDecimal cost = w.divide(wpu, 6, RoundingMode.HALF_UP).multiply(ing.getPrice());
            ingredientCost = ingredientCost.add(cost);
        }
        ingredientCost = ingredientCost.add(seasoning).setScale(2, RoundingMode.HALF_UP);
        BigDecimal labor = ingredientCost.multiply(laborRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal other = ingredientCost.multiply(otherRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCost = ingredientCost.add(labor).add(other);
        BigDecimal suggest = BigDecimal.ZERO;
        if (entranceRate.compareTo(BigDecimal.ZERO) > 0) {
            suggest = totalCost.divide(entranceRate, 0, RoundingMode.CEILING);
        }

        Dish up = new Dish();
        up.setId(dish.getId());
        up.setIngredientCost(ingredientCost);
        up.setLaborFee(labor);
        up.setOtherFee(other);
        up.setSuggestPrice(suggest);
        up.setAuditPrice(suggest);
        up.setSeasoningCost(seasoning);
        up.setExcludeSeasoning(dish.getExcludeSeasoning());
        dishMapper.updateCost(up);
    }

    /** 重新核算所有菜品成本 */
    public void recalcAll() {
        for (Dish d : dishMapper.listAll()) {
            recalcCost(d);
        }
    }

    /** 保存成本参数 */
    public void saveCostSettings(CostSettingsDto dto) {
        if (dto.getEntranceRate() != null) settingService.set("entrance_rate", dto.getEntranceRate().setScale(2, RoundingMode.HALF_UP).toPlainString(), "食材入口率");
        if (dto.getLaborRate() != null) settingService.set("labor_rate", dto.getLaborRate().setScale(2, RoundingMode.HALF_UP).toPlainString(), "劳务费比例");
        if (dto.getOtherRate() != null) settingService.set("other_rate", dto.getOtherRate().setScale(2, RoundingMode.HALF_UP).toPlainString(), "其他费用比例");
        if (dto.getSeasoningCost() != null) settingService.set("seasoning_cost", dto.getSeasoningCost().setScale(2, RoundingMode.HALF_UP).toPlainString(), "调料费");
        recalcAll();
    }

    public Map<String, Object> costSettings() {
        Map<String, Object> m = new HashMap<>();
        m.put("entranceRate", settingService.getDecimal("entrance_rate", new BigDecimal("0.90")));
        m.put("laborRate", settingService.getDecimal("labor_rate", new BigDecimal("0.10")));
        m.put("otherRate", settingService.getDecimal("other_rate", new BigDecimal("0.05")));
        m.put("seasoningCost", settingService.getDecimal("seasoning_cost", new BigDecimal("0.50")));
        return m;
    }

    // 删除（同步删除不再被引用的菜品图片文件）
    @Transactional
    public void delete(Integer id) {
        Dish d = dishMapper.findById(id);
        dishIngredientMapper.deleteByDish(id);
        dishMapper.delete(id);
        if (d != null && d.getImage() != null && !d.getImage().isEmpty()
                && dishMapper.countByImage(d.getImage()) == 0) {
            fileService.deleteImage(d.getImage());
        }
    }

    @Transactional
    public void deleteByIds(List<Integer> ids) {
        Set<String> imgs = new LinkedHashSet<>();
        for (Integer id : ids) {
            Dish d = dishMapper.findById(id);
            if (d != null && d.getImage() != null && !d.getImage().isEmpty()) imgs.add(d.getImage());
        }
        for (Integer id : ids) dishIngredientMapper.deleteByDish(id);
        dishMapper.deleteByIds(ids);
        for (String url : imgs) {
            if (dishMapper.countByImage(url) == 0) fileService.deleteImage(url);
        }
    }

    /** 筛选某天食谱已选的菜品 */
    public List<Dish> listByRecipe(String weekKey, String mealType) {
        return dishMapper.listByRecipe(weekKey, mealType);
    }

    /** 导出菜品Excel(含内嵌图片) */
    public byte[] exportDishes() throws Exception {
        String[] headers = {"编号", "菜名", "类别", "配料", "食材成本", "建议售价", "审核价格", "排除调料", "图片"};
        List<List<Object>> data = new ArrayList<>();
        List<byte[]> images = new ArrayList<>();
        for (Dish d : listAll()) {
            StringBuilder ingSb = new StringBuilder();
            for (DishIngredient di : dishIngredientMapper.listByDish(d.getId())) {
                if (ingSb.length() > 0) ingSb.append("、");
                ingSb.append(di.getIngredientName()).append(" ")
                     .append(di.getWeightG() == null ? 0 : di.getWeightG().toPlainString()).append("克");
            }
            data.add(Arrays.asList(d.getCode(), d.getName(), d.getCategory(), ingSb.toString(),
                    d.getIngredientCost(), d.getSuggestPrice(), d.getAuditPrice(),
                    (d.getExcludeSeasoning() != null && d.getExcludeSeasoning() == 1) ? "是" : "", ""));
            images.add(fileService.readImageBytes(d.getImage()));
        }
        return ExcelUtil.exportWithImages(headers, data, images);
    }

    /** 导入菜品Excel（含内嵌图片 + 可选配料列 + 旧格式图片文件名兼容） */
    @Transactional
    public Map<String, Object> importDishes(byte[] bytes) throws Exception {
        List<ExcelUtil.RowData> rows = ExcelUtil.readWithImages(bytes);
        int ok = 0; int skip = 0; List<String> errors = new ArrayList<>();
        for (ExcelUtil.RowData rowData : rows) {
            Map<String, String> row = rowData.fields;
            try {
                String name = get(row, "菜名");
                if (name.isEmpty()) { skip++; continue; }
                String category = get(row, "类别");
                Integer code = dishMapper.maxCode() + 1;
                Dish d = new Dish();
                d.setCode(code); d.setName(name);
                d.setCategory(category.isEmpty() ? null : category);
                // 优先取 Excel 内嵌图片
                if (rowData.image != null) {
                    d.setImage(fileService.saveImageBytes(rowData.image, 800, 0.8));
                } else {
                    String img = get(row, "图片");
                    d.setImage(img.isEmpty() ? null : "/upload/" + img);
                }
                d.setExcludeSeasoning("是".equals(get(row, "排除调料")) ? 1 : 0);
                d.setSeasoningCost(settingService.getDecimal("seasoning_cost", new BigDecimal("0.50")));
                dishMapper.insert(d);
                // 配料列: 新格式 "冬瓜 120克、五花肉 140克"；兼容旧格式 "名称1:克重1g;名称2:克重2g"
                String ingStr = get(row, "配料");
                if (!ingStr.isEmpty()) {
                    for (String part : ingStr.split("[,，、|；;]")) {
                        if (part.trim().isEmpty()) continue;
                        String ingName; BigDecimal w;
                        int colon = part.indexOf(':');
                        if (colon < 0) colon = part.indexOf('：');
                        if (colon >= 0) {
                            // 旧格式：名称:克重
                            ingName = part.substring(0, colon).trim();
                            w = ExcelUtil.toDecimal(part.substring(colon + 1), BigDecimal.ZERO);
                        } else {
                            // 新格式：名称 克数克（最后一个空白后为克数）
                            int sp = -1;
                            for (int i = part.length() - 1; i >= 0; i--) {
                                char c = part.charAt(i);
                                if (c == ' ' || c == '\t' || c == '　') { sp = i; break; }
                            }
                            if (sp >= 0) {
                                ingName = part.substring(0, sp).trim();
                                w = ExcelUtil.toDecimal(part.substring(sp + 1), BigDecimal.ZERO);
                            } else {
                                ingName = part.trim();
                                w = BigDecimal.ZERO;
                            }
                        }
                        Ingredient ing = ingredientMapper.findByName(ingName);
                        if (ing != null) {
                            DishIngredient di = new DishIngredient();
                            di.setDishId(d.getId()); di.setIngredientId(ing.getId());
                            di.setIngredientName(ingName); di.setWeightG(w);
                            dishIngredientMapper.insert(di);
                        }
                    }
                }
                recalcCost(d);
                ok++;
            } catch (Exception e) {
                errors.add(get(row, "菜名") + ":" + e.getMessage());
            }
        }
        Map<String, Object> res = new HashMap<>();
        res.put("ok", ok); res.put("skip", skip); res.put("errors", errors);
        return res;
    }

    public void updateImage(Integer id, String image) {
        dishMapper.updateImage(id, image);
    }

    private String get(Map<String, String> row, String key) {
        String v = row.get(key); return v == null ? "" : v.trim();
    }
}
