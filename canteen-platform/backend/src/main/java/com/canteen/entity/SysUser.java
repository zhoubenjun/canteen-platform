package com.canteen.entity;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class SysUser {
    private Integer id;
    private String username;
    private String password;
    private Integer online;
    private Date lastLoginTime;
    private Date createdAt;
    private String createdBy;
    // 联查
    private List<String> roles;
}
