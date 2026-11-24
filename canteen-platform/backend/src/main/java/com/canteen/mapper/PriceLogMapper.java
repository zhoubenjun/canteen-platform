package com.canteen.mapper;

import com.canteen.entity.PriceLog;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface PriceLogMapper {
    int insert(PriceLog log);
    List<PriceLog> query(@Param("ingredientId") Integer ingredientId,
                         @Param("categoryId") Integer categoryId,
                         @Param("supplierId") Integer supplierId,
                         @Param("changeType") String changeType,
                         @Param("start") Date start, @Param("end") Date end);
    List<Map<String,Object>> history(@Param("ingredientId") Integer ingredientId);
    Map<String,Object> stats();
}
