package com.canteen.service;

import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线会话登记表。
 *
 * 用户是否在线不再只看数据库里的 online 标记（该标记在关闭浏览器、会话超时、
 * 服务重启后都会残留），而是由服务端会话心跳决定：
 * 浏览器端每 60 秒调用一次 /api/auth/heartbeat，服务端记录该会话最近活跃时间；
 * 只有最近一次心跳在 ONLINE_WINDOW_MS 之内的会话，才认为对应用户在线。
 */
@Component
public class OnlineSessionRegistry {

    /** 心跳有效期：150 秒（心跳间隔 60 秒，允许丢一到两次心跳） */
    public static final long ONLINE_WINDOW_MS = 150_000L;

    private static class SessionEntry {
        final String username;
        volatile long lastActive;
        SessionEntry(String username) {
            this.username = username;
            this.lastActive = System.currentTimeMillis();
        }
    }

    private final Map<String, SessionEntry> sessions = new ConcurrentHashMap<>();

    /** 登录成功后登记会话 */
    public void register(String username, String sessionId) {
        if (username == null || sessionId == null) return;
        sessions.put(sessionId, new SessionEntry(username));
    }

    /** 心跳：刷新该会话的最近活跃时间；会话不存在时返回 false */
    public boolean touch(String sessionId) {
        if (sessionId == null) return false;
        SessionEntry e = sessions.get(sessionId);
        if (e == null) return false;
        e.lastActive = System.currentTimeMillis();
        return true;
    }

    /** 会话销毁 / 退出登录时注销 */
    public void unregister(String sessionId) {
        if (sessionId == null) return;
        sessions.remove(sessionId);
    }

    /** 该用户名当前是否存在活跃会话 */
    public boolean isOnline(String username) {
        if (username == null) return false;
        long now = System.currentTimeMillis();
        boolean online = false;
        Iterator<Map.Entry<String, SessionEntry>> it = sessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, SessionEntry> en = it.next();
            SessionEntry e = en.getValue();
            if (now - e.lastActive > ONLINE_WINDOW_MS) {
                it.remove();
                continue;
            }
            if (e.username.equalsIgnoreCase(username)) online = true;
        }
        return online;
    }

    /** 清理所有已过期的会话，返回仍在线的用户名数量（用于维护） */
    public int purgeExpired() {
        long now = System.currentTimeMillis();
        int alive = 0;
        Iterator<Map.Entry<String, SessionEntry>> it = sessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, SessionEntry> en = it.next();
            if (now - en.getValue().lastActive > ONLINE_WINDOW_MS) {
                it.remove();
            } else {
                alive++;
            }
        }
        return alive;
    }
}
