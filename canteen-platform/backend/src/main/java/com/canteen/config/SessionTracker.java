package com.canteen.config;

import com.canteen.service.OnlineSessionRegistry;
import com.canteen.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * 监听会话销毁（退出登录、会话超时、容器清理），及时把在线会话登记表与数据库 online 标记同步为离线。
 */
@Component
public class SessionTracker implements HttpSessionListener {

    @Autowired
    private OnlineSessionRegistry registry;

    @Autowired
    private UserService userService;

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        String sessionId = session.getId();
        registry.unregister(sessionId);
        String username = null;
        try {
            Object un = session.getAttribute("user");
            if (un != null) username = String.valueOf(un);
        } catch (Exception ignored) {
            // 会话销毁阶段部分容器已不允许读取属性
        }
        if (username != null) {
            try {
                userService.offlineIfNoActiveSession(username);
            } catch (Exception ignored) {
                // 会话销毁回调中不应抛出异常
            }
        }
    }
}
