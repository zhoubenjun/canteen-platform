package com.canteen.entity;

import lombok.Data;
import java.util.Date;

@Data
public class SysRole {
    private Integer id;
    private String roleName;
    private Date createdAt;
}
