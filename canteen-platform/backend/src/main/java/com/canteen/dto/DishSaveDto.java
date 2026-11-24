package com.canteen.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/** 菜品保存请求（含配料） */
@Data
public class DishSaveDto {
    private Integer id;
    private String name;
    private String category;
    private Integer excludeSeasoning;
    private BigDecimal auditPrice;
    private String image;
    private List<IngredientItem> ingredients;

    @Data
    public static class IngredientItem {
        private Integer ingredientId;
        private String ingredientName;
        private BigDecimal weightG;
    }
}
