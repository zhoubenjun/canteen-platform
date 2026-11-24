package com.canteen.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IngredientCategory {
    private Integer id;
    private String name;
    private String code;
    private Integer status;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
