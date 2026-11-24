package com.canteen.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class PriceLog {
    private Integer id;
    private Integer ingredientId;
    private String ingredientName;
    private String categoryName;
    private String supplierName;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private BigDecimal changePct;
    private String changeType;
    private Date createdAt;
}
