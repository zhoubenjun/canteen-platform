package com.canteen.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class Dish {
    private Integer id;
    private Integer code;
    private String name;
    private String category;
    private String image;
    private Integer excludeSeasoning;
    private BigDecimal seasoningCost;
    private BigDecimal ingredientCost;
    private BigDecimal laborFee;
    private BigDecimal otherFee;
    private BigDecimal suggestPrice;
    private BigDecimal auditPrice;
    private Date createdAt;
    // 联查
    private List<DishIngredient> ingredients;
    private String ingredientsText;   // 配料汇总文本
    private String priceDiff;         // 价格差异提示(前端可自行计算)
}
