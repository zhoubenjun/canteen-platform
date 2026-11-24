package com.canteen.mapper;

import com.canteen.entity.Dish;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DishMapper {
    List<Dish> page(@Param("keyword") String keyword, @Param("category") String category,
                    @Param("unPriced") Boolean unPriced,
                    @Param("ingredientKeyword") String ingredientKeyword,
                    @Param("offset") int offset, @Param("size") int size);
    long count(@Param("keyword") String keyword, @Param("category") String category,
               @Param("unPriced") Boolean unPriced,
               @Param("ingredientKeyword") String ingredientKeyword);
    List<Dish> listAll();
    Dish findById(@Param("id") Integer id);
    Integer maxCode();
    int insert(Dish d);
    int update(Dish d);
    int updateCost(Dish d);
    int delete(@Param("id") Integer id);
    int deleteByIds(@Param("ids") List<Integer> ids);
    int deleteAll();
    int batchInsert(List<Dish> list);
    List<Dish> listByRecipe(@Param("weekKey") String weekKey, @Param("mealType") String mealType);
    int updateImage(@Param("id") Integer id, @Param("image") String image);
    int updateAuditPrice(@Param("id") Integer id, @Param("price") java.math.BigDecimal price);
    long countByImage(@Param("image") String image);
}
