package com.canteen.mapper;

import com.canteen.entity.Consumption;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ConsumptionMapper {

    List<Consumption> page(@Param("name") String name,
                           @Param("registerType") String registerType,
                           @Param("businessType") String businessType,
                           @Param("start") String start,
                           @Param("end") String end,
                           @Param("offset") int offset,
                           @Param("size") int size);

    long count(@Param("name") String name,
               @Param("registerType") String registerType,
               @Param("businessType") String businessType,
               @Param("start") String start,
               @Param("end") String end);

    /** 取用于比对的三元组：登记姓名|消费日期|消费业务类型 */
    List<String> listMatchKeys();

    int batchInsert(@Param("list") List<Consumption> list);

    int deleteAll();

    int deleteByMonth(@Param("month") String month);
}
