package com.canteen.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 数据统计：按实际用餐日期区间汇总食材用量与菜品使用次数。
 * weekly_recipe.week_key 为该周周一日期(yyyy-MM-dd)，day_index 0=周一..6=周日，
 * 实际用餐日期 = week_key + day_index。
 */
public interface StatsMapper {

    /** 食材统计：区间内每种食材的总重量(KG)、当前单价、总成本，按总重量降序 */
    List<Map<String, Object>> ingredientStats(@Param("startDate") String startDate,
                                              @Param("endDate") String endDate);

    /** 菜品统计：区间内每道菜的使用餐次，按次数降序 */
    List<Map<String, Object>> dishStats(@Param("startDate") String startDate,
                                        @Param("endDate") String endDate);
}
