package com.canteen.controller;

import com.canteen.dto.ApiResult;
import com.canteen.entity.SysUser;
import com.canteen.service.OnlineSessionRegistry;
import com.canteen.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private OnlineSessionRegistry sessionRegistry;

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || username.trim().isEmpty() || password == null) {
            return ApiResult.error("用户名或密码不能为空");
        }
        SysUser u = userService.login(username.trim(), password);
        if (u == null) {
            return ApiResult.error("用户名或密码错误");
        }
        HttpSession session = request.getSession(true);
        session.setAttribute("user", u.getUsername());
        sessionRegistry.register(u.getUsername(), session.getId());
        Map<String, Object> m = new HashMap<>();
        m.put("username", u.getUsername());
        m.put("roles", u.getRoles());
        return ApiResult.ok(m);
    }

    /** 前端定时心跳：刷新当前会话的在线时间 */
    @PostMapping("/heartbeat")
    public ApiResult<Void> heartbeat(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            return ApiResult.error("未登录");
        }
        sessionRegistry.touch(session.getId());
        return ApiResult.ok();
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object un = session.getAttribute("user");
            sessionRegistry.unregister(session.getId());
            if (un != null) userService.offlineIfNoActiveSession(String.valueOf(un));
            session.invalidate();
        }
        return ApiResult.ok();
    }

    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            return ApiResult.error("未登录");
        }
        String username = String.valueOf(session.getAttribute("user"));
        List<String> roles = userService.rolesOf(username);
        Map<String, Object> m = new HashMap<>();
        m.put("username", username);
        m.put("roles", roles);
        return ApiResult.ok(m);
    }

    @PostMapping("/changePwd")
    public ApiResult<Void> changePwd(@RequestBody Map<String, String> body, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            return ApiResult.error("未登录");
        }
        String username = String.valueOf(session.getAttribute("user"));
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        String err = userService.changePwd(username, oldPassword, newPassword);
        if (err != null) return ApiResult.error(err);
        // 改密后使会话失效，需重新登录
        session.invalidate();
        return ApiResult.ok();
    }
}
