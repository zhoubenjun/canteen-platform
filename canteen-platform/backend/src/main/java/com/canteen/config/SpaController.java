package com.canteen.config;

import org.springframework.stereotype.Controller;

/**
 * SPA 回退已由 WebConfig 的静态资源处理器实现，
 * 此控制器保留为空占位，不再注册任何路由映射，避免递归转发。
 */
@Controller
public class SpaController {
    // 无映射，交由资源处理器统一回退到 index.html
}
