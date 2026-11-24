package com.canteen.controller;

import com.canteen.dto.ApiResult;
import com.canteen.entity.Dish;
import com.canteen.entity.DishIngredient;
import com.canteen.entity.WeeklyRecipe;
import com.canteen.mapper.DishIngredientMapper;
import com.canteen.mapper.DishMapper;
import com.canteen.mapper.WeeklyRecipeMapper;
import com.canteen.service.SysSettingService;
import com.canteen.service.WeeklyRecipeService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;

/** 菜品公示 / 大屏展示 数据接口 */
@RestController
@RequestMapping("/api/publicity")
public class PublicityController {

    @Resource private WeeklyRecipeMapper weeklyRecipeMapper;
    @Resource private DishMapper dishMapper;
    @Resource private DishIngredientMapper dishIngredientMapper;
    @Resource private SysSettingService settingService;

    /** 某天(weekKey+dayIndex)的三餐数据，含菜品价格/配料/图片 */
    @GetMapping("/day")
    public ApiResult<Map<String, Object>> day(@RequestParam String weekKey, @RequestParam int dayIndex) {
        List<WeeklyRecipe> recipes = weeklyRecipeMapper.listByWeek(weekKey);
        List<WeeklyRecipe> dayRecipes = new ArrayList<>();
        for (WeeklyRecipe r : recipes) if (r.getDayIndex() == dayIndex) dayRecipes.add(r);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", settingService.get("publicity_title", "郧西县职业技术学校每日食谱"));
        result.put("date", weekKeyToDate(weekKey, dayIndex));
        result.put("dayName", WeeklyRecipeService.DAY_NAMES[dayIndex]);

        List<Map<String, Object>> meals = new ArrayList<>();
        for (String meal : new String[]{"早餐", "中餐", "晚餐"}) {
            List<Map<String, Object>> dishes = new ArrayList<>();
            for (WeeklyRecipe r : dayRecipes) {
                if (!meal.equals(r.getMealType()) || r.getDishId() == null) continue;
                Dish d = dishMapper.findById(r.getDishId());
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("dishId", r.getDishId());
                item.put("name", d == null ? r.getDishName() : d.getName());
                item.put("category", r.getCategory());
                item.put("portions", r.getPortions());
                if (d != null) {
                    item.put("image", d.getImage());
                    item.put("suggestPrice", d.getSuggestPrice());
                    item.put("auditPrice", d.getAuditPrice());
                    item.put("ingredientCost", d.getIngredientCost());
                    List<DishIngredient> ings = dishIngredientMapper.listByDish(d.getId());
                    StringBuilder sb = new StringBuilder();
                    for (DishIngredient di : ings) {
                        if (sb.length() > 0) sb.append("、");
                        sb.append(di.getIngredientName()).append(" ").append(di.getWeightG()).append("g");
                    }
                    item.put("ingredientsText", sb.toString());
                }
                dishes.add(item);
            }
            Map<String, Object> mealMap = new LinkedHashMap<>();
            mealMap.put("meal", meal);
            mealMap.put("dishes", dishes);
            meals.add(mealMap);
        }
        result.put("meals", meals);
        return ApiResult.ok(result);
    }

    /** 大屏数据：全部天，含菜品详情 */
    @GetMapping("/bigscreen")
    public ApiResult<Map<String, Object>> bigscreen(@RequestParam String weekKey) {
        List<WeeklyRecipe> recipes = weeklyRecipeMapper.listByWeek(weekKey);
        Map<Integer, Dish> dishCache = new HashMap<>();
        List<Map<String, Object>> days = new ArrayList<>();
        for (int day = 0; day < 7; day++) {
            List<Map<String, Object>> meals = new ArrayList<>();
            for (String meal : new String[]{"早餐", "中餐", "晚餐"}) {
                List<Map<String, Object>> dishes = new ArrayList<>();
                for (WeeklyRecipe r : recipes) {
                    if (r.getDayIndex() != day || !meal.equals(r.getMealType()) || r.getDishId() == null) continue;
                    Dish d = dishCache.get(r.getDishId());
                    if (d == null) { d = dishMapper.findById(r.getDishId()); dishCache.put(r.getDishId(), d); }
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("dishId", r.getDishId());
                    item.put("name", d == null ? r.getDishName() : d.getName());
                    item.put("category", r.getCategory());
                    if (d != null) {
                        item.put("image", d.getImage());
                        item.put("suggestPrice", d.getSuggestPrice());
                        item.put("auditPrice", d.getAuditPrice());
                        item.put("ingredientCost", d.getIngredientCost());
                        List<DishIngredient> ings = dishIngredientMapper.listByDish(d.getId());
                        StringBuilder sb = new StringBuilder();
                        for (DishIngredient di : ings) {
                            if (sb.length() > 0) sb.append("、");
                            sb.append(di.getIngredientName());
                        }
                        item.put("ingredientsText", sb.toString());
                    }
                    dishes.add(item);
                }
                if (!dishes.isEmpty()) {
                    Map<String, Object> mealMap = new LinkedHashMap<>();
                    mealMap.put("meal", meal);
                    mealMap.put("dishes", dishes);
                    meals.add(mealMap);
                }
            }
            Map<String, Object> dayMap = new LinkedHashMap<>();
            dayMap.put("day", day);
            dayMap.put("dayName", WeeklyRecipeService.DAY_NAMES[day]);
            dayMap.put("meals", meals);
            days.add(dayMap);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("canteenName", settingService.get("canteen_name", "郧西县职业技术学校食堂"));
        result.put("days", days);
        return ApiResult.ok(result);
    }

    private String weekKeyToDate(String weekKey, int dayIndex) {
        try {
            Calendar c = Calendar.getInstance();
            c.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(weekKey));
            c.add(Calendar.DATE, dayIndex);
            return new SimpleDateFormat("yyyy-MM-dd").format(c.getTime());
        } catch (Exception e) {
            return weekKey;
        }
    }
}
