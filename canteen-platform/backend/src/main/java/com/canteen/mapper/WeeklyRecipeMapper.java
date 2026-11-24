package com.canteen.mapper;

import com.canteen.entity.WeeklyRecipe;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WeeklyRecipeMapper {
    List<WeeklyRecipe> listByWeek(@Param("weekKey") String weekKey);
    int insert(WeeklyRecipe r);
    int insertBatch(List<WeeklyRecipe> list);
    int update(WeeklyRecipe r);
    int deleteByWeek(@Param("weekKey") String weekKey);
    int delete(@Param("id") Integer id);
    List<String> listWeekKeys();
    List<WeeklyRecipe> listForPurchase(@Param("weekKey") String weekKey, @Param("days") List<Integer> days);
}
