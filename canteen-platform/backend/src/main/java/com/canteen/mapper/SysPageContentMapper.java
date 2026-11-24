package com.canteen.mapper;

import com.canteen.entity.SysPageContent;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysPageContentMapper {
    List<SysPageContent> listAll();
    SysPageContent findByKey(@Param("key") String key);
    int upsert(SysPageContent p);
}
