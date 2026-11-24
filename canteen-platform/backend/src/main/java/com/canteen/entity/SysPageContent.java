package com.canteen.entity;

import lombok.Data;

@Data
public class SysPageContent {
    private Integer id;
    private String pageKey;
    private String pageTitle;
    private String content;
    private String remark;
    private String updatedBy;
    private String updatedAt;
}
