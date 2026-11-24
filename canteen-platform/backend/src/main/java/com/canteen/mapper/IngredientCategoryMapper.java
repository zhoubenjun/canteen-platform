package com.canteen.mapper;

import com.canteen.entity.IngredientCategory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface IngredientCategoryMapper {
    List<IngredientCategory> listAll();
    List<IngredientCategory> listByName(@Param("keyword") String keyword);
    long countByName(@Param("keyword") String keyword);
    List<IngredientCategory> pageByName(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);
    IngredientCategory findById(@Param("id") Integer id);
    IngredientCategory findByName(@Param("name") String name);
    int insert(IngredientCategory c);
    int update(IngredientCategory c);
    int updateStatus(IngredientCategory c);
    int delete(@Param("id") Integer id);
}
