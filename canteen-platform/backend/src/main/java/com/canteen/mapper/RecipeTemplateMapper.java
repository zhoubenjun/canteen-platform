package com.canteen.mapper;

import com.canteen.entity.RecipeTemplate;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RecipeTemplateMapper {
    List<RecipeTemplate> listAll(@Param("keyword") String keyword);
    RecipeTemplate findById(@Param("id") Integer id);
    int insert(RecipeTemplate t);
    int update(RecipeTemplate t);
    int delete(@Param("id") Integer id);
}
