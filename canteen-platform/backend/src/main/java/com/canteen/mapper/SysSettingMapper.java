package com.canteen.mapper;

import com.canteen.entity.SysSetting;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysSettingMapper {
    List<SysSetting> listAll();
    SysSetting findByKey(@Param("key") String key);
    int upsert(SysSetting s);
}
