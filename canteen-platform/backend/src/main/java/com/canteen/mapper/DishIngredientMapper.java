package com.canteen.mapper;

import com.canteen.entity.DishIngredient;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DishIngredientMapper {
    int insert(DishIngredient di);
    int deleteByDish(@Param("dishId") Integer dishId);
    List<DishIngredient> listByDish(@Param("dishId") Integer dishId);
    List<DishIngredient> listByDishIds(@Param("ids") List<Integer> ids);
    int batchInsert(List<DishIngredient> list);
}
