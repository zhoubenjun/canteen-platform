package com.canteen.dto;

import lombok.Data;
import java.util.List;

/** 自动编排食谱请求 */
@Data
public class AutoArrangeDto {
    // 每个餐次一个配置
    private List<MealConfig> meals;
    private Boolean allowRepeat;     // 是否允许重复
    private Integer minRepeatGap;    // 最小重复间隔(天)

    @Data
    public static class MealConfig {
        private String mealType;      // 早餐/中餐/晚餐
        private Integer dayIndex;     // 0-6
        private Integer peopleCount;  // 计划人数
        private Integer perPersonDishes; // 每人打菜数量
        private Integer meatCount;    // 荤菜数
        private Integer vegCount;     // 素菜数
        private Integer stapleCount;  // 主食数
    }
}
