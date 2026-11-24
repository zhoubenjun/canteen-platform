package com.canteen.service;

import com.canteen.entity.SysRole;
import com.canteen.entity.SysUser;
import com.canteen.mapper.SysRoleMapper;
import com.canteen.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private SysRoleMapper roleMapper;
    @Autowired
    private OnlineSessionRegistry sessionRegistry;

    /** 登录：校验用户名密码，成功后标记在线并记录登录时间 */
    public SysUser login(String username, String password) {
        SysUser u = userMapper.findByUsername(username);
        if (u == null) return null;
        if (!u.getPassword().equals(password)) return null;
        userMapper.markLogin(u.getId());
        u.setOnline(1);
        u.setLastLoginTime(new Date());
        u.setRoles(userMapper.findRolesByUsername(username));
        return u;
    }

    /** 退出登录 / 会话销毁：仅当该用户已无其他活跃会话时才置为离线 */
    public void offlineIfNoActiveSession(String username) {
        SysUser u = userMapper.findByUsername(username);
        if (u == null) return;
        if (sessionRegistry.isOnline(username)) return;
        userMapper.updateOnline(u.getId(), 0);
    }

    public List<String> rolesOf(String username) {
        return userMapper.findRolesByUsername(username);
    }

    public boolean isAdmin(String username) {
        if (username == null) return false;
        if ("admin".equalsIgnoreCase(username)) return true;
        return rolesOf(username).contains("admin");
    }

    /** 列表：在线状态按会话心跳实时校准；非 admin 时对密码中间4位打码 */
    public List<SysUser> list(String username, boolean admin) {
        List<SysUser> list = userMapper.list(username);
        for (SysUser u : list) {
            u.setRoles(userMapper.findRolesByUsername(u.getUsername()));
            int realOnline = sessionRegistry.isOnline(u.getUsername()) ? 1 : 0;
            if (u.getOnline() == null || u.getOnline() != realOnline) {
                // 数据库标记与实际会话不一致时以会话为准，并回写，避免长期残留
                userMapper.updateOnline(u.getId(), realOnline);
            }
            u.setOnline(realOnline);
            if (!admin) u.setPassword(maskMiddle4(u.getPassword()));
        }
        return list;
    }

    private String maskMiddle4(String pwd) {
        if (pwd == null) return "";
        int len = pwd.length();
        if (len <= 4) return "****";
        char[] c = pwd.toCharArray();
        int start = (len - 4) / 2;
        for (int i = start; i < start + 4; i++) c[i] = '*';
        return new String(c);
    }

    /** 新增用户（可携带角色） */
    public SysUser add(String username, String password, String createdBy, List<Integer> roleIds) {
        if (userMapper.findByUsername(username) != null) return null;
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword(password);
        u.setOnline(0);
        u.setCreatedBy(createdBy);
        userMapper.insert(u);
        bindRoles(u.getId(), roleIds);
        return u;
    }

    public SysUser findById(Integer id) {
        return userMapper.findById(id);
    }

    public boolean resetPwd(Integer id, String password) {
        return userMapper.updatePassword(id, password) > 0;
    }

    public boolean delete(Integer id) {
        return userMapper.delete(id) > 0;
    }

    public List<SysRole> roles() {
        return roleMapper.list();
    }

    /** 编辑用户角色（仅修改角色） */
    public void updateRoles(Integer userId, List<Integer> roleIds) {
        userMapper.deleteUserRolesByUserId(userId);
        bindRoles(userId, roleIds);
    }

    private void bindRoles(Integer userId, List<Integer> roleIds) {
        if (roleIds == null) return;
        for (Integer rid : roleIds) {
            if (rid == null) continue;
            userMapper.insertUserRole(userId, rid);
        }
    }

    /** 新增角色 */
    public SysRole addRole(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) return null;
        if (roleMapper.findByName(roleName.trim()) != null) return null;
        SysRole r = new SysRole();
        r.setRoleName(roleName.trim());
        roleMapper.insert(r);
        return r;
    }

    /** 删除角色（admin 角色不可删，删除时清理用户关联） */
    public String deleteRole(Integer id) {
        SysRole r = roleMapper.findById(id);
        if (r == null) return "角色不存在";
        if ("admin".equalsIgnoreCase(r.getRoleName())) return "admin角色不可删除";
        userMapper.deleteUserRolesByRoleId(id);
        roleMapper.delete(id);
        return null;
    }

    /** 修改自己的密码 */
    public String changePwd(String username, String oldPassword, String newPassword) {
        if (username == null) return "未登录";
        SysUser u = userMapper.findByUsername(username);
        if (u == null) return "用户不存在";
        if (!u.getPassword().equals(oldPassword)) return "原密码不正确";
        if (newPassword == null || newPassword.length() < 6) return "新密码必须大于等于6个字符";
        for (char ch : newPassword.toCharArray()) {
            if (Character.isWhitespace(ch)) return "密码不能包含空格";
            if (ch > 127) return "密码只能由数字、字母或特殊符号组成";
        }
        if (newPassword.equals(oldPassword)) return "新密码不能与原密码相同";
        userMapper.updatePassword(u.getId(), newPassword);
        return null;
    }
}
