package com.canteen.filter;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/** 登录鉴权过滤器：未登录时拦截除 auth 之外的 /api/ 请求 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) return true;
        // 登录/登出/当前用户 不拦截（me 由控制器返回未登录）
        if (path.startsWith("/api/auth/")) return true;
        // 页面内容公开读取（法律声明等免登录页面）：GET /api/page/{key}，但列表接口仍需登录
        if ("GET".equalsIgnoreCase(request.getMethod())
                && path.matches("/api/page/[A-Za-z0-9_-]+")
                && !"/api/page/list".equals(path)) return true;
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录\",\"data\":null}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
