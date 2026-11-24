package com.canteen.controller;

import com.canteen.dto.ApiResult;
import com.canteen.entity.SysRole;
import com.canteen.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/role")
public class RoleController {

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public ApiResult<List<SysRole>> list() {
        return ApiResult.ok(userService.roles());
    }

    @PostMapping("/add")
    public ApiResult<Void> add(@RequestBody Map<String, String> body, HttpSession session) {
        String current = currentUser(session);
        if (!userService.isAdmin(current)) return ApiResult.error("无权限，仅admin可新增角色");
        String roleName = body.get("roleName");
        if (roleName == null || roleName.trim().isEmpty()) return ApiResult.error("角色名称不能为空");
        SysRole r = userService.addRole(roleName);
        if (r == null) return ApiResult.error("角色已存在或名称无效");
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable("id") Integer id, HttpSession session) {
        String current = currentUser(session);
        if (!userService.isAdmin(current)) return ApiResult.error("无权限，仅admin可删除角色");
        String err = userService.deleteRole(id);
        if (err != null) return ApiResult.error(err);
        return ApiResult.ok();
    }

    private String currentUser(HttpSession session) {
        if (session == null) return null;
        Object un = session.getAttribute("user");
        return un == null ? null : String.valueOf(un);
    }
}
