package com.canteen.mapper;

import com.canteen.entity.SysRole;

import java.util.List;

public interface SysRoleMapper {
    List<SysRole> list();
    SysRole findById(Integer id);
    SysRole findByName(String roleName);
    int insert(SysRole r);
    int delete(Integer id);
}
