package com.canteen.entity;

import lombok.Data;
import java.math.BigDecimal;

/** 消费数据（仅登记类型为教职工） */
@Data
public class Consumption {
    private Long id;
    /** 登记类型 */
    private String registerType;
    /** 登记姓名 */
    private String registerName;
    /** 班级名称 */
    private String className;
    /** 消费金额(元) */
    private BigDecimal amount;
    /** 支付状态 */
    private String payStatus;
    /** 消费日期 yyyy-MM-dd */
    private String consumeDate;
    /** 消费业务类型：早餐/午餐/晚餐/夜宵/其他 */
    private String businessType;
    /** 消费订单号 */
    private String orderNo;
    /** 消费菜品 */
    private String dish;
}
