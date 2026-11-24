package com.canteen.entity;

import lombok.Data;

@Data
public class WeeklyRecipe {
    private Integer id;
    private String weekKey;
    private Integer dayIndex;
    private String mealType;
    private Integer dishId;
    private String dishName;
    private String category;
    private Integer peopleCount;
    private Integer portions;
    private Integer seq;
}
