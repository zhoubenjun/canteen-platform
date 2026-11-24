package com.canteen.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class Ingredient {
    private Integer id;
    private String name;
    private Integer categoryId;
    private String categoryName;
    private Integer supplierId;
    private String supplierName;
    private String spec;
    private BigDecimal price;
    private String unit;
    private String priceUnit;
    private BigDecimal weightPerUnit;
    private Date updatedAt;
}
