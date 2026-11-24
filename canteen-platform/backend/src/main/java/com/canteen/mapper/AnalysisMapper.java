package com.canteen.mapper;

import com.canteen.entity.PaymentAnalysis;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AnalysisMapper {

    List<PaymentAnalysis> page(@Param("name") String name,
                               @Param("start") String start,
                               @Param("end") String end,
                               @Param("mealType") String mealType,
                               @Param("paid") String paid,
                               @Param("offset") int offset,
                               @Param("size") int size);

    long count(@Param("name") String name,
               @Param("start") String start,
               @Param("end") String end,
               @Param("mealType") String mealType,
               @Param("paid") String paid);

    List<PaymentAnalysis> listFiltered(@Param("name") String name,
                                      @Param("start") String start,
                                      @Param("end") String end,
                                      @Param("mealType") String mealType,
                                      @Param("paid") String paid);

    int batchInsert(@Param("list") List<PaymentAnalysis> list);

    int deleteAll();

    int deleteByMonth(@Param("month") String month);
}
