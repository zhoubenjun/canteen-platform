package com.canteen.mapper;

import com.canteen.entity.Supplier;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SupplierMapper {
    List<Supplier> listAll();
    List<Supplier> listByName(@Param("keyword") String keyword);
    long countByName(@Param("keyword") String keyword);
    List<Supplier> pageByName(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);
    Supplier findByName(@Param("name") String name);
    Supplier findById(@Param("id") Integer id);
    int insert(Supplier s);
    int update(Supplier s);
    int delete(@Param("id") Integer id);
}
