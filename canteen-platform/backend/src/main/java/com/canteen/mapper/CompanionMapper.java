package com.canteen.mapper;

import com.canteen.entity.Companion;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CompanionMapper {

    List<Companion> page(@Param("name") String name,
                         @Param("start") String start,
                         @Param("end") String end,
                         @Param("mealType") String mealType,
                         @Param("offset") int offset,
                         @Param("size") int size);

    long count(@Param("name") String name,
               @Param("start") String start,
               @Param("end") String end,
               @Param("mealType") String mealType);

    List<Companion> listAll();

    int batchInsert(@Param("list") List<Companion> list);

    int deleteAll();

    int deleteByMonth(@Param("month") String month);
}
