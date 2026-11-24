package com.canteen.service;

import com.canteen.dto.AutoArrangeDto;
import com.canteen.entity.*;
import com.canteen.mapper.*;
import com.canteen.util.ExcelUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class WeeklyRecipeService {

    @Resource private WeeklyRecipeMapper weeklyRecipeMapper;
    @Resource private DishMapper dishMapper;
    @Resource private DishIngredientMapper dishIngredientMapper;
    @Resource private IngredientMapper ingredientMapper;
    @Resource private RecipeTemplateMapper recipeTemplateMapper;
    @Resource private SysSettingService settingService;

    public static final String[] MEAL_TYPES = {"早餐", "中餐", "晚餐"};
    public static final String[] DAY_NAMES = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    // ---------- 周/天 ----------
    public List<WeeklyRecipe> listByWeek(String weekKey) {
        return weeklyRecipeMapper.listByWeek(weekKey);
    }

    public List<String> weekKeys() {
        return weeklyRecipeMapper.listWeekKeys();
    }

    /** 本周周一日期 */
    public String currentMonday() {
        return mondayOf(new Date());
    }

    public static String mondayOf(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        return new SimpleDateFormat("yyyy-MM-dd").format(c.getTime());
    }

    /** 计算周序号：公历/学期 */
    public Map<String, Object> weekNumber(String weekKey) {
        String mode = settingService.get("semester_mode", "0");
        Map<String, Object> m = new HashMap<>();
        m.put("mode", mode);
        try {
            Date monday = new SimpleDateFormat("yyyy-MM-dd").parse(weekKey);
            if ("1".equals(mode)) {
                String s1 = settingService.get("semester1_start", "2026-09-01");
                String s2 = settingService.get("semester2_start", "2027-02-22");
                Calendar mc = Calendar.getInstance(); mc.setTime(monday);
                Calendar c1 = Calendar.getInstance(); c1.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(s1));
                Calendar c2 = Calendar.getInstance(); c2.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(s2));
                if (!mc.before(c1)) {
                    long diff = (mc.getTimeInMillis() - c1.getTimeInMillis()) / (7L * 24 * 3600 * 1000);
                    m.put("semester", 1);
                    m.put("week", diff + 1);
                } else if (!mc.before(c2)) {
                    long diff = (mc.getTimeInMillis() - c2.getTimeInMillis()) / (7L * 24 * 3600 * 1000);
                    m.put("semester", 2);
                    m.put("week", diff + 1);
                } else {
                    m.put("semester", 0); m.put("week", 0);
                }
            } else {
                Calendar mc = Calendar.getInstance(); mc.setTime(monday);
                int weekOfYear = mc.get(Calendar.WEEK_OF_YEAR);
                m.put("week", weekOfYear);
            }
        } catch (Exception e) {
            m.put("week", 0);
        }
        return m;
    }

    /** 保存整周（删除旧数据后重建） */
    @Transactional
    public void saveWeek(String weekKey, List<WeeklyRecipe> list) {
        weeklyRecipeMapper.deleteByWeek(weekKey);
        for (WeeklyRecipe r : list) {
            r.setWeekKey(weekKey);
            if (r.getDishId() != null) {
                Dish d = dishMapper.findById(r.getDishId());
                if (d != null) {
                    r.setDishName(d.getName());
                    r.setCategory(d.getCategory());
                }
            }
        }
        if (!list.isEmpty()) weeklyRecipeMapper.insertBatch(list);
    }

    /** 份数计算 */
    public void calcPortions(List<WeeklyRecipe> list) {
        // 按 天+餐 分组
        Map<String, List<WeeklyRecipe>> groups = new LinkedHashMap<>();
        for (WeeklyRecipe r : list) {
            groups.computeIfAbsent(r.getDayIndex() + "-" + r.getMealType(), k -> new ArrayList<>()).add(r);
        }
        for (List<WeeklyRecipe> g : groups.values()) {
            Integer people = g.get(0).getPeopleCount();
            if (people == null || people <= 0) {
                for (WeeklyRecipe r : g) r.setPortions(0);
                continue;
            }
            String meal = g.get(0).getMealType();
            if ("早餐".equals(meal)) {
                for (WeeklyRecipe r : g) {
                    if ("主食".equals(r.getCategory())) r.setPortions(people);
                    else r.setPortions(0); // 豆浆/鸡蛋/烤肠等不算
                }
            } else {
                int total = people * 2; // 每人2菜
                int n = g.size();
                if (n == 0) continue;
                int base = total / n;
                int rem = total % n;
                int idx = 0;
                for (WeeklyRecipe r : g) {
                    r.setPortions(base + (idx < rem ? 1 : 0));
                    idx++;
                }
            }
        }
    }

    /** 复制前一天 / 后一天：把目标天的食谱复制到当前天 */
    @Transactional
    public void copyDay(String weekKey, int currentDay, int sourceDay) {
        List<WeeklyRecipe> all = weeklyRecipeMapper.listByWeek(weekKey);
        // 删除当前天
        for (WeeklyRecipe r : all) {
            if (r.getDayIndex() == currentDay) weeklyRecipeMapper.delete(r.getId());
        }
        for (WeeklyRecipe r : all) {
            if (r.getDayIndex() == sourceDay) {
                WeeklyRecipe n = new WeeklyRecipe();
                n.setWeekKey(weekKey);
                n.setDayIndex(currentDay);
                n.setMealType(r.getMealType());
                n.setDishId(r.getDishId());
                n.setDishName(r.getDishName());
                n.setCategory(r.getCategory());
                n.setPeopleCount(r.getPeopleCount());
                n.setPortions(r.getPortions());
                n.setSeq(r.getSeq());
                weeklyRecipeMapper.insert(n);
            }
        }
    }

    /** 复制上周整周食谱到当前周（覆盖当前周） */
    @Transactional
    public Map<String, Object> copyLastWeek(String weekKey) throws Exception {
        // 上周周一 = 当前 weekKey - 7 天
        Calendar c = Calendar.getInstance();
        c.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(weekKey));
        c.add(Calendar.DAY_OF_YEAR, -7);
        String lastWeekKey = new SimpleDateFormat("yyyy-MM-dd").format(c.getTime());

        List<WeeklyRecipe> lastWeek = weeklyRecipeMapper.listByWeek(lastWeekKey);
        if (lastWeek.isEmpty()) {
            throw new RuntimeException("上周（" + lastWeekKey + "）尚无食谱数据，无法复制");
        }
        // 删除当前周
        weeklyRecipeMapper.deleteByWeek(weekKey);
        // 复制上周记录到当前周
        List<WeeklyRecipe> list = new ArrayList<>();
        for (WeeklyRecipe r : lastWeek) {
            WeeklyRecipe n = new WeeklyRecipe();
            n.setWeekKey(weekKey);
            n.setDayIndex(r.getDayIndex());
            n.setMealType(r.getMealType());
            n.setDishId(r.getDishId());
            n.setDishName(r.getDishName());
            n.setCategory(r.getCategory());
            n.setPeopleCount(r.getPeopleCount());
            n.setPortions(r.getPortions());
            n.setSeq(r.getSeq());
            list.add(n);
        }
        if (!list.isEmpty()) weeklyRecipeMapper.insertBatch(list);
        Map<String, Object> res = new HashMap<>();
        res.put("lastWeekKey", lastWeekKey);
        res.put("count", list.size());
        return res;
    }

    /** 复制本周整周食谱到下一周（覆盖下一周现有食谱） */
    @Transactional
    public Map<String, Object> copyToNextWeek(String weekKey) throws Exception {
        Calendar c = Calendar.getInstance();
        c.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(weekKey));
        c.add(Calendar.DAY_OF_YEAR, 7);
        String nextWeekKey = new SimpleDateFormat("yyyy-MM-dd").format(c.getTime());

        List<WeeklyRecipe> current = weeklyRecipeMapper.listByWeek(weekKey);
        if (current.isEmpty()) {
            throw new RuntimeException("本周（" + weekKey + "）尚无食谱数据，无法复制");
        }
        // 清空下一周原有数据（仅限本周复制的目标周）
        weeklyRecipeMapper.deleteByWeek(nextWeekKey);
        List<WeeklyRecipe> list = new ArrayList<>();
        for (WeeklyRecipe r : current) {
            WeeklyRecipe n = new WeeklyRecipe();
            n.setWeekKey(nextWeekKey);
            n.setDayIndex(r.getDayIndex());
            n.setMealType(r.getMealType());
            n.setDishId(r.getDishId());
            n.setDishName(r.getDishName());
            n.setCategory(r.getCategory());
            n.setPeopleCount(r.getPeopleCount());
            n.setPortions(r.getPortions());
            n.setSeq(r.getSeq());
            list.add(n);
        }
        if (!list.isEmpty()) weeklyRecipeMapper.insertBatch(list);
        Map<String, Object> res = new HashMap<>();
        res.put("nextWeekKey", nextWeekKey);
        res.put("count", list.size());
        return res;
    }

    // ---------- 模板 ----------
    @Transactional
    public RecipeTemplate saveTemplate(String weekKey, String name) throws Exception {
        List<WeeklyRecipe> list = weeklyRecipeMapper.listByWeek(weekKey);
        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(list);
        RecipeTemplate t = new RecipeTemplate();
        t.setName(name == null || name.isEmpty() ? weekKey : name);
        t.setDataJson(json);
        recipeTemplateMapper.insert(t);
        return t;
    }

    public List<RecipeTemplate> listTemplates(String keyword) {
        return recipeTemplateMapper.listAll(keyword);
    }

    @Transactional
    public void loadTemplate(Integer id, String weekKey) throws Exception {
        RecipeTemplate t = recipeTemplateMapper.findById(id);
        if (t == null) throw new RuntimeException("模板不存在");
        ObjectMapper om = new ObjectMapper();
        List<WeeklyRecipe> list = om.readValue(t.getDataJson(),
                om.getTypeFactory().constructCollectionType(List.class, WeeklyRecipe.class));
        for (WeeklyRecipe r : list) r.setId(null);
        saveWeek(weekKey, list);
    }

    public void renameTemplate(Integer id, String name) {
        RecipeTemplate t = new RecipeTemplate();
        t.setId(id); t.setName(name);
        recipeTemplateMapper.update(t);
    }

    public void deleteTemplate(Integer id) {
        recipeTemplateMapper.delete(id);
    }

    // ---------- 自动编排 ----------
    public List<WeeklyRecipe> autoArrange(AutoArrangeDto dto) {
        List<WeeklyRecipe> result = new ArrayList<>();
        List<Dish> allDishes = dishMapper.listAll();
        // 分类
        Map<String, List<Dish>> byCat = new HashMap<>();
        for (Dish d : allDishes) byCat.computeIfAbsent(d.getCategory() == null ? "" : d.getCategory(), k -> new ArrayList<>()).add(d);

        Map<String, Integer> usage = new HashMap<>(); // dishId -> 出现次数(用于重复控制)

        if (dto.getMeals() != null) {
            for (AutoArrangeDto.MealConfig cfg : dto.getMeals()) {
                List<String> picks = new ArrayList<>();
                int meat = cfg.getMeatCount() == null ? 0 : cfg.getMeatCount();
                int veg = cfg.getVegCount() == null ? 0 : cfg.getVegCount();
                int staple = cfg.getStapleCount() == null ? 0 : cfg.getStapleCount();
                picks.addAll(pick(cfg, byCat.getOrDefault("鲜荤", new ArrayList<>()), meat, usage, dto, allDishes, "鲜荤"));
                picks.addAll(pick(cfg, byCat.getOrDefault("冻荤", new ArrayList<>()), meat / 2, usage, dto, allDishes, "冻荤"));
                picks.addAll(pick(cfg, byCat.getOrDefault("素菜", new ArrayList<>()), veg, usage, dto, allDishes, "素菜"));
                picks.addAll(pick(cfg, byCat.getOrDefault("主食", new ArrayList<>()), staple, usage, dto, allDishes, "主食"));
                int people = cfg.getPeopleCount() == null ? 0 : cfg.getPeopleCount();
                int seq = 0;
                for (String dishId : picks) {
                    WeeklyRecipe r = new WeeklyRecipe();
                    r.setDayIndex(cfg.getDayIndex());
                    r.setMealType(cfg.getMealType());
                    r.setDishId(Integer.parseInt(dishId));
                    Dish d = dishMapper.findById(Integer.parseInt(dishId));
                    r.setDishName(d.getName()); r.setCategory(d.getCategory());
                    r.setPeopleCount(people);
                    r.setSeq(seq++);
                    result.add(r);
                }
            }
        }
        // 计算份数
        calcPortions(result);
        return result;
    }

    private List<String> pick(AutoArrangeDto.MealConfig cfg, List<Dish> pool, int count, Map<String, Integer> usage,
                              AutoArrangeDto dto, List<Dish> allDishes, String cat) {
        List<String> out = new ArrayList<>();
        if (count <= 0 || pool.isEmpty()) return out;
        // 按权重：价格高权重略高
        List<Dish> candidates = new ArrayList<>(pool);
        boolean allowRepeat = dto.getAllowRepeat() == null ? true : dto.getAllowRepeat();
        int gap = dto.getMinRepeatGap() == null ? 0 : dto.getMinRepeatGap();
        if (!allowRepeat) {
            candidates.removeIf(d -> usage.getOrDefault(String.valueOf(d.getId()), 0) > 0);
        }
        if (candidates.isEmpty()) candidates = new ArrayList<>(pool);
        Collections.shuffle(candidates);
        for (int i = 0; i < count && i < candidates.size(); i++) {
            Dish d = candidates.get(i);
            String key = String.valueOf(d.getId());
            out.add(key);
            usage.merge(key, 1, Integer::sum);
        }
        return out;
    }

    // ---------- Excel 导入导出周食谱 ----------
    public byte[] exportWeek(String weekKey) throws Exception {
        List<WeeklyRecipe> list = weeklyRecipeMapper.listByWeek(weekKey);

        // 第 1 页：周食谱主表
        String[] headers = {"星期", "餐次", "菜名", "人数", "份数", "序号"};
        List<List<Object>> data = new ArrayList<>();
        List<Integer> dishIds = new ArrayList<>();
        for (WeeklyRecipe r : list) {
            data.add(Arrays.asList(DAY_NAMES[r.getDayIndex()], r.getMealType(), r.getDishName(),
                    r.getPeopleCount(), r.getPortions(), r.getSeq()));
            if (r.getDishId() != null && !dishIds.contains(r.getDishId())) dishIds.add(r.getDishId());
        }

        // 第 2 页：每个菜品一行，配料用顿号合并（例：瘦肉 50.00克、黄豆芽 50.00克）
        Map<Integer, List<DishIngredient>> ingMap = new HashMap<>();
        if (!dishIds.isEmpty()) {
            for (DishIngredient di : dishIngredientMapper.listByDishIds(dishIds)) {
                ingMap.computeIfAbsent(di.getDishId(), k -> new ArrayList<>()).add(di);
            }
        }
        String[] ingHeaders = {"星期", "餐次", "菜名", "类别", "配料（每份）"};
        List<List<Object>> ingRows = new ArrayList<>();
        for (WeeklyRecipe r : list) {
            List<DishIngredient> ings = r.getDishId() == null ? null : ingMap.get(r.getDishId());
            String text;
            if (ings == null || ings.isEmpty()) {
                text = "（未维护配料）";
            } else {
                StringBuilder sb = new StringBuilder();
                for (DishIngredient di : ings) {
                    if (sb.length() > 0) sb.append("、");
                    sb.append(di.getIngredientName()).append(' ');
                    sb.append(String.format("%.2f", di.getWeightG() == null ? BigDecimal.ZERO : di.getWeightG())).append("克");
                }
                text = sb.toString();
            }
            ingRows.add(Arrays.asList(DAY_NAMES[r.getDayIndex()], r.getMealType(), r.getDishName(), r.getCategory(), text));
        }

        List<ExcelUtil.SheetSpec> sheets = new ArrayList<>();
        sheets.add(new ExcelUtil.SheetSpec("周食谱", headers, data));
        sheets.add(new ExcelUtil.SheetSpec("菜品配料", ingHeaders, ingRows));
        return ExcelUtil.exportMulti(sheets);
    }

    @Transactional
    public Map<String, Object> importWeek(String weekKey, byte[] bytes) throws Exception {
        // 导入只读「周食谱」页，配料页不参与导入（无需填写配料）
        List<Map<String, String>> rows = ExcelUtil.read(bytes, "周食谱");
        List<WeeklyRecipe> list = new ArrayList<>();
        int ok = 0; int skip = 0; List<String> errors = new ArrayList<>();
        for (Map<String, String> row : rows) {
            try {
                String dayName = get(row, "星期");
                String meal = get(row, "餐次");
                String dishName = get(row, "菜名");
                int dayIdx = -1;
                for (int i = 0; i < DAY_NAMES.length; i++) if (DAY_NAMES[i].equals(dayName)) dayIdx = i;
                if (dayIdx < 0 || dishName.isEmpty()) { skip++; continue; }
                Dish d = dishMapper.listAll().stream().filter(x -> x.getName().equals(dishName)).findFirst().orElse(null);
                WeeklyRecipe r = new WeeklyRecipe();
                r.setWeekKey(weekKey); r.setDayIndex(dayIdx); r.setMealType(meal);
                r.setDishId(d == null ? null : d.getId());
                r.setDishName(dishName); r.setCategory(d == null ? null : d.getCategory());
                r.setPeopleCount(ExcelUtil.toInt(get(row, "人数"), 0));
                r.setPortions(ExcelUtil.toInt(get(row, "份数"), 0));
                r.setSeq(ExcelUtil.toInt(get(row, "序号"), 0));
                list.add(r); ok++;
            } catch (Exception e) {
                errors.add(get(row, "菜名") + ":" + e.getMessage());
            }
        }
        if (!list.isEmpty()) weeklyRecipeMapper.insertBatch(list);
        Map<String, Object> res = new HashMap<>();
        res.put("ok", ok); res.put("skip", skip); res.put("errors", errors);
        return res;
    }

    private String get(Map<String, String> row, String key) {
        String v = row.get(key); return v == null ? "" : v.trim();
    }
}
