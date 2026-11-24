package com.canteen.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class PurchasePlan {
    private Integer id;
    private String planKey;
    private String weekKey;
    private Integer supplierId;
    private String supplierName;
    private Integer ingredientId;
    private String ingredientName;
    private String categoryName;
    private String spec;
    private BigDecimal totalWeight;
    private String unit;
    private BigDecimal price;
    private BigDecimal amount;
    private String mealType;
    private Date createdAt;
}
