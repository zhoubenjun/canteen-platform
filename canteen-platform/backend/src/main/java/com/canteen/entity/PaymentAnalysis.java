package com.canteen.entity;

import lombok.Data;

/** 陪餐支付分析结果 */
@Data
public class PaymentAnalysis {
    private Long id;
    /** 陪餐人员 */
    private String companionName;
    /** 陪餐日期 yyyy-MM-dd */
    private String companionDate;
    /** 餐次 */
    private String mealType;
    /** 是否支付：已支付/未支付 */
    private String paid;
}
