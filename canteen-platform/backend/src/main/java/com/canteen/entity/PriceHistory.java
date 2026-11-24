package com.canteen.entity;

import lombok.Data;
import java.util.Date;

@Data
public class PriceHistory {
    private Integer id;
    private String versionName;
    private Date snapshotDate;
    private String dataJson;
}
