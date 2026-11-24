package com.canteen.entity;

import lombok.Data;
import java.util.Date;

@Data
public class RecipeTemplate {
    private Integer id;
    private String name;
    private String dataJson;
    private Date createdAt;
}
