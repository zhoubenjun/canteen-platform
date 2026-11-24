package com.canteen.mapper;

import com.canteen.entity.SysUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysUserMapper {
    List<SysUser> list(@Param("username") String username);
    SysUser findByUsername(@Param("username") String username);
    SysUser findById(@Param("id") Integer id);
    int insert(SysUser u);
    int updatePassword(@Param("id") Integer id, @Param("password") String password);
    int updateOnline(@Param("id") Integer id, @Param("online") int online);
    int markLogin(@Param("id") Integer id);
    int clearAllOnline();
    int delete(@Param("id") Integer id);
    List<String> findRolesByUsername(@Param("username") String username);
    int insertUserRole(@Param("userId") Integer userId, @Param("roleId") Integer roleId);
    int deleteUserRolesByUserId(@Param("userId") Integer userId);
    int deleteUserRolesByRoleId(@Param("roleId") Integer roleId);
}
