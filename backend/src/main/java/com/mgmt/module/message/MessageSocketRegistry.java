package com.mgmt.module.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线 WebSocket 会话登记表：一个用户可能有多端连接，推送时逐个发送。
 * 发送失败或连接已关闭时顺手清理，避免集合无限增长。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSocketRegistry {

    private final ObjectMapper objectMapper;

    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void add(Long userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void remove(Long userId, WebSocketSession session) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) {
            return;
        }
        userSessions.remove(session);
        if (userSessions.isEmpty()) {
            sessions.remove(userId, userSessions);
        }
    }

    /** 给指定用户的所有连接推送同一个 JSON 负载 */
    public void send(Long userId, Object payload) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null || userSessions.isEmpty()) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("消息序列化失败", e);
            return;
        }
        TextMessage message = new TextMessage(json);
        userSessions.forEach(session -> sendTo(session, message, userId));
    }

    private void sendTo(WebSocketSession session, TextMessage message, Long userId) {
        if (!session.isOpen()) {
            remove(userId, session);
            return;
        }
        try {
            // WebSocketSession 不是线程安全的，同一连接可能被多个线程推送
            synchronized (session) {
                session.sendMessage(message);
            }
        } catch (Exception e) {
            log.debug("消息推送失败，断开会话：{}", e.getMessage());
            remove(userId, session);
        }
    }

    /** 强制下线等场景：先推完消息再关闭连接 */
    public void close(Long userId) {
        Set<WebSocketSession> userSessions = sessions.remove(userId);
        if (userSessions == null) {
            return;
        }
        userSessions.forEach(session -> {
            try {
                session.close(CloseStatus.NORMAL);
            } catch (Exception e) {
                log.debug("关闭 WebSocket 失败：{}", e.getMessage());
            }
        });
    }
}
