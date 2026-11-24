package com.canteen.controller;

import com.canteen.dto.ApiResult;
import com.canteen.entity.SysPageContent;
import com.canteen.service.SysPageContentService;
import com.canteen.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/page")
public class PageContentController {

    @Resource private SysPageContentService service;
    @Resource private UserService userService;

    /** 公开读取：法律声明等免登录页面使用 */
    @GetMapping("/{key}")
    public ApiResult<SysPageContent> get(@PathVariable String key) {
        SysPageContent p = service.get(key);
        if (p == null) return ApiResult.error("页面不存在：" + key);
        return ApiResult.ok(p);
    }

    /** 列表：后台维护页面使用 */
    @GetMapping("/list")
    public ApiResult<List<SysPageContent>> list(HttpServletRequest request) {
        String user = currentUser(request);
        if (user == null) return ApiResult.error("未登录");
        return ApiResult.ok(service.list());
    }

    /** 保存标题与正文：仅管理员 */
    @PutMapping("/{key}")
    public ApiResult<Void> save(@PathVariable String key, @RequestBody Map<String, String> body, HttpServletRequest request) {
        String user = currentUser(request);
        if (user == null) return ApiResult.error("未登录");
        if (!userService.isAdmin(user)) return ApiResult.error("仅管理员可修改页面内容");
        String title = body.get("title");
        String content = body.get("content");
        if (title == null || title.trim().isEmpty()) return ApiResult.error("标题不能为空");
        if (content == null || content.trim().isEmpty()) return ApiResult.error("正文不能为空");
        service.save(key, title, content, user);
        return ApiResult.ok();
    }

    private String currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) return null;
        return String.valueOf(session.getAttribute("user"));
    }
}
