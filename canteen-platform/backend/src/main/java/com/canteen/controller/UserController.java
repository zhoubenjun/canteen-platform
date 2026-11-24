package com.canteen.controller;

import com.canteen.dto.ApiResult;
import com.canteen.entity.SysRole;
import com.canteen.entity.SysUser;
import com.canteen.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public ApiResult<List<SysUser>> list(@RequestParam(value = "username", required = false) String username, HttpSession session) {
        String current = currentUser(session);
        boolean admin = userService.isAdmin(current);
        return ApiResult.ok(userService.list(username, admin));
    }

    @PostMapping("/add")
    public ApiResult<Map<String, Object>> add(@RequestBody Map<String, Object> body, HttpSession session) {
        String current = currentUser(session);
        if (!userService.isAdmin(current)) return ApiResult.error("无权限，仅admin可新增用户");
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        String err = validate(username, password);
        if (err != null) return ApiResult.error(err);
        List<Integer> roleIds = toRoleIds(body.get("roles"));
        SysUser u = userService.add(username.trim(), password, current, roleIds);
        if (u == null) return ApiResult.error("用户名已存在");
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        return ApiResult.ok(m);
    }

    @PostMapping("/updateRole")
    public ApiResult<Void> updateRole(@RequestBody Map<String, Object> body, HttpSession session) {
        String current = currentUser(session);
        if (!userService.isAdmin(current)) return ApiResult.error("无权限，仅admin可修改用户角色");
        Object idObj = body.get("id");
        if (idObj == null) return ApiResult.error("参数缺失");
        Integer id = Integer.valueOf(String.valueOf(idObj));
        SysUser u = userService.findById(id);
        if (u == null) return ApiResult.error("用户不存在");
        List<Integer> roleIds = toRoleIds(body.get("roles"));
        userService.updateRoles(id, roleIds);
        return ApiResult.ok();
    }

    private List<Integer> toRoleIds(Object obj) {
        java.util.List<Integer> ids = new java.util.ArrayList<>();
        if (obj instanceof List) {
            for (Object o : (List<?>) obj) {
                try { ids.add(Integer.valueOf(String.valueOf(o))); } catch (Exception ignored) {}
            }
        }
        return ids;
    }

    @PostMapping("/resetPwd")
    public ApiResult<Void> resetPwd(@RequestBody Map<String, Object> body, HttpSession session) {
        String current = currentUser(session);
        if (!userService.isAdmin(current)) return ApiResult.error("无权限，仅admin可重置密码");
        Object idObj = body.get("id");
        String password = (String) body.get("password");
        if (idObj == null) return ApiResult.error("参数缺失");
        Integer id = Integer.valueOf(String.valueOf(idObj));
        String err = validate(null, password);
        if (err != null) return ApiResult.error(err);
        if (!userService.resetPwd(id, password)) return ApiResult.error("用户不存在");
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable("id") Integer id, HttpSession session) {
        String current = currentUser(session);
        if (!userService.isAdmin(current)) return ApiResult.error("无权限，仅admin可注销用户");
        SysUser u = userService.findById(id);
        if (u == null) return ApiResult.error("用户不存在");
        if ("admin".equalsIgnoreCase(u.getUsername())) return ApiResult.error("不能注销admin账号");
        userService.delete(id);
        return ApiResult.ok();
    }

    @GetMapping("/roles")
    public ApiResult<List<SysRole>> roles() {
        return ApiResult.ok(userService.roles());
    }

    private String currentUser(HttpSession session) {
        if (session == null) return null;
        Object un = session.getAttribute("user");
        return un == null ? null : String.valueOf(un);
    }

    private String validate(String username, String password) {
        if (username != null) {
            if (username.trim().isEmpty()) return "用户名不能为空";
            if (username.contains(" ")) return "用户名不能包含空格";
        }
        if (password == null || password.length() < 6) return "密码必须大于等于6个字符";
        for (char ch : password.toCharArray()) {
            if (Character.isWhitespace(ch)) return "密码不能包含空格";
            if (ch > 127) return "密码只能由数字、字母或特殊符号组成";
        }
        return null;
    }
}
