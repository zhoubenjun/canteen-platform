package com.canteen.entity;

import lombok.Data;

/** 陪餐数据 */
@Data
public class Companion {
    private Long id;
    /** 学校名称 */
    private String schoolName;
    /** 陪餐日期 yyyy-MM-dd */
    private String companionDate;
    /** 星期 */
    private String week;
    /** 餐次：早餐/午餐/晚餐 */
    private String mealType;
    /** 菜品名称 */
    private String dishName;
    /** 陪餐人员 */
    private String companionName;
    /** 陪餐身份 */
    private String companionIdentity;
    /** 评价等级 */
    private String rating;
    /** 陪餐评价 */
    private String comment;
    /** 创建时间 */
    private String createdTime;
}
