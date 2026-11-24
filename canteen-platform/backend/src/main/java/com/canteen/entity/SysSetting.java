package com.canteen.entity;

import lombok.Data;

@Data
public class SysSetting {
    private Integer id;
    private String settingKey;
    private String settingValue;
    private String remark;
}
