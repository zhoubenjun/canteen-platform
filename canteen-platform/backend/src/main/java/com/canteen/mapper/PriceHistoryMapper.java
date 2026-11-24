package com.canteen.mapper;

import com.canteen.entity.PriceHistory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PriceHistoryMapper {
    int insert(PriceHistory h);
    List<PriceHistory> listAll();
    PriceHistory findById(@Param("id") Integer id);
    int delete(@Param("id") Integer id);
}
