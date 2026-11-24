package com.canteen.mapper;

import com.canteen.entity.PurchasePlan;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PurchasePlanMapper {
    int insertBatch(List<PurchasePlan> list);
    List<PurchasePlan> listByPlan(@Param("planKey") String planKey);
    List<String> listPlanKeys();
    int delete(@Param("id") Integer id);
}
