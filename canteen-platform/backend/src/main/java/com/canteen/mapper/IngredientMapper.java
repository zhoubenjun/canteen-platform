package com.canteen.mapper;

import com.canteen.entity.Ingredient;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface IngredientMapper {
    List<Ingredient> page(@Param("keyword") String keyword, @Param("categoryId") Integer categoryId,
                          @Param("supplierId") Integer supplierId, @Param("offset") int offset, @Param("size") int size);
    long count(@Param("keyword") String keyword, @Param("categoryId") Integer categoryId,
               @Param("supplierId") Integer supplierId);
    List<Ingredient> listAll();
    Ingredient findById(@Param("id") Integer id);
    Ingredient findByName(@Param("name") String name);
    int insert(Ingredient i);
    int update(Ingredient i);
    int updatePriceOnly(@Param("id") Integer id, @Param("price") java.math.BigDecimal price);
    int delete(@Param("id") Integer id);
    int deleteAll();
    int deleteByIds(@Param("ids") List<Integer> ids);
}
