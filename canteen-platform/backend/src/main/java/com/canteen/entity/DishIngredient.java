package com.canteen.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DishIngredient {
    private Integer id;
    private Integer dishId;
    private Integer ingredientId;
    private String ingredientName;
    private BigDecimal weightG;
}
