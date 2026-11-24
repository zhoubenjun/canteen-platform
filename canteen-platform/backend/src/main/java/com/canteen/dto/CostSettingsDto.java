package com.canteen.dto;

import lombok.Data;
import java.math.BigDecimal;

/** 成本核算参数 */
@Data
public class CostSettingsDto {
    private BigDecimal entranceRate;   // 食材入口率
    private BigDecimal laborRate;      // 劳务费比例
    private BigDecimal otherRate;      // 其他费用比例
    private BigDecimal seasoningCost;  // 调料费
}
