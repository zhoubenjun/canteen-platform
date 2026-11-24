package com.canteen.service;

import com.canteen.entity.*;
import com.canteen.mapper.*;
import com.canteen.util.ExcelUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class PurchaseService {

    @Resource private WeeklyRecipeMapper weeklyRecipeMapper;
    @Resource private DishIngredientMapper dishIngredientMapper;
    @Resource private IngredientMapper ingredientMapper;
    @Resource private SupplierMapper supplierMapper;
    @Resource private PurchasePlanMapper purchasePlanMapper;

    /**
     * 汇总采购计划
     * @param weekKey 周
     * @param days 选中的天(0-6)，空=全部
     * @param mealType 早餐/中餐/晚餐/全部
     * @return {rows:[...], totalAmount}
     */
    public Map<String, Object> summary(String weekKey, List<Integer> days, String mealType) {
        List<WeeklyRecipe> recipes = weeklyRecipeMapper.listForPurchase(weekKey, days);
        if (mealType != null && !mealType.isEmpty() && !"全部".equals(mealType)) {
            recipes.removeIf(r -> !mealType.equals(r.getMealType()));
        }
        // 收集涉及菜品
        Set<Integer> dishIds = new HashSet<>();
        Map<Integer, Integer> dishPortions = new HashMap<>();
        for (WeeklyRecipe r : recipes) {
            if (r.getDishId() != null && r.getPortions() != null && r.getPortions() > 0) {
                dishIds.add(r.getDishId());
                dishPortions.merge(r.getDishId(), r.getPortions(), Integer::sum);
            }
        }
        // 汇总食材(克)
        Map<Integer, BigDecimal> ingGrams = new LinkedHashMap<>();
        Map<Integer, Integer> ingDish = new HashMap<>();
        for (Integer dishId : dishIds) {
            int portions = dishPortions.get(dishId);
            for (DishIngredient di : dishIngredientMapper.listByDish(dishId)) {
                BigDecimal w = di.getWeightG() == null ? BigDecimal.ZERO : di.getWeightG();
                ingGrams.merge(di.getIngredientId(), w.multiply(BigDecimal.valueOf(portions)), BigDecimal::add);
                ingDish.merge(di.getIngredientId(), 1, Integer::sum);
            }
        }
        // 转换为KG并按供应商分组
        List<PurchasePlan> plans = new ArrayList<>();
        for (Map.Entry<Integer, BigDecimal> e : ingGrams.entrySet()) {
            Ingredient ing = ingredientMapper.findById(e.getKey());
            if (ing == null) continue;
            BigDecimal kg = e.getValue().divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
            PurchasePlan p = new PurchasePlan();
            p.setIngredientId(ing.getId());
            p.setIngredientName(ing.getName());
            p.setCategoryName(catName(ing));
            p.setSpec(ing.getSpec() == null ? "" : ing.getSpec());
            p.setSupplierId(ing.getSupplierId());
            p.setSupplierName(ing.getSupplierId() == null ? "" : supName(ing));
            p.setTotalWeight(kg);
            p.setUnit("KG");
            p.setPrice(ing.getPrice() == null ? BigDecimal.ZERO : ing.getPrice());
            p.setAmount(kg.multiply(p.getPrice()).setScale(2, RoundingMode.HALF_UP));
            p.setMealType(mealType == null || mealType.isEmpty() ? "全部" : mealType);
            plans.add(p);
        }
        // 按供应商分组
        plans.sort(Comparator.comparing(PurchasePlan::getSupplierName).thenComparing(PurchasePlan::getIngredientName));
        Map<String, Object> res = new HashMap<>();
        BigDecimal total = plans.stream().map(PurchasePlan::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        res.put("rows", plans);
        res.put("totalAmount", total.setScale(2, RoundingMode.HALF_UP));
        return res;
    }

    /** 保存采购计划到mysql */
    @Transactional
    public String savePlan(String weekKey, List<Integer> days, String mealType) {
        Map<String, Object> s = summary(weekKey, days, mealType);
        @SuppressWarnings("unchecked")
        List<PurchasePlan> plans = (List<PurchasePlan>) s.get("rows");
        String planKey = "PLAN-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        for (PurchasePlan p : plans) {
            p.setPlanKey(planKey);
            p.setWeekKey(weekKey);
        }
        if (!plans.isEmpty()) purchasePlanMapper.insertBatch(plans);
        return planKey;
    }

    public List<PurchasePlan> listPlan(String planKey) {
        return purchasePlanMapper.listByPlan(planKey);
    }

    public List<String> planKeys() {
        return purchasePlanMapper.listPlanKeys();
    }

    public byte[] exportPlan(List<PurchasePlan> plans) throws Exception {
        String[] headers = {"供应商", "食材", "类别", "规格", "单价(元/KG)", "总重量(KG)", "数量", "金额(元)", "餐次"};
        List<List<Object>> data = new ArrayList<>();
        for (PurchasePlan p : plans) {
            data.add(Arrays.asList(p.getSupplierName(), p.getIngredientName(), p.getCategoryName(),
                    p.getSpec(), p.getPrice(), p.getTotalWeight(), qtyText(p), p.getAmount(), p.getMealType()));
        }
        return ExcelUtil.export(headers, data);
    }

    // 与前端 calcQty 一致：规格为空→数量=总重量(kg)；规格非空→按数值乘积换算
    private String qtyText(PurchasePlan p) {
        double kg = p.getTotalWeight() == null ? 0 : p.getTotalWeight().doubleValue();
        String spec = p.getSpec();
        if (spec == null || spec.trim().isEmpty()) {
            return round2(kg) + "（kg）";
        }
        int slash = spec.lastIndexOf('/');
        String unit = slash >= 0 ? spec.substring(slash + 1).trim() : "";
        String left = slash >= 0 ? spec.substring(0, slash) : spec;
        String[] parts = left.split("\\*");
        double product = 1;
        boolean baseKg = false;
        for (int i = 0; i < parts.length; i++) {
            String pstr = parts[i].trim();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(pstr);
            if (!m.find()) continue;
            if (i == 0) {
                String lower = pstr.toLowerCase();
                if (lower.matches(".*(ml|毫升).*")) baseKg = false;             // ml → 数量=kg*1000/乘积
                else if (lower.matches(".*(kg|克|斤).*")) baseKg = true;        // kg → 数量=kg/乘积
                else if (lower.contains("升")) baseKg = true;                    // 升 → 1升=1kg
                else if (lower.matches(".*(^|[^a-z])l($|[^a-z]).*")) baseKg = true; // l(非ml) → 1l=1kg
                else baseKg = false;                                             // 默认按 g → 数量=kg*1000/乘积
            }
            product *= Double.parseDouble(m.group(1));
        }
        if (product == 0) return round2(kg) + "（" + (unit.isEmpty() ? "kg" : unit) + "）";
        double qty = baseKg ? kg / product : kg * 1000 / product;
        return round2(qty) + "（" + unit + "）";
    }
    private String round2(double v) {
        return String.format("%.2f", v);
    }

    private String catName(Ingredient i) {
        return i.getCategoryName() == null ? "" : i.getCategoryName();
    }
    private String supName(Ingredient i) {
        Supplier s = i.getSupplierId() == null ? null : supplierMapper.findById(i.getSupplierId());
        return s == null ? "" : s.getName();
    }
}
