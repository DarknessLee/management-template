package com.mgmt.module.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 消息通道：只负责登记连接与心跳，真正的业务推送由 MessageService 调用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageWebSocketHandler extends TextWebSocketHandler {

    private static final String PING = "ping";
    private static final String PONG = "pong";

    private final MessageSocketRegistry registry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = userId(session);
        if (userId != null) {
            registry.add(userId, session);
            log.debug("用户 {} 建立消息连接", userId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = userId(session);
        if (userId != null) {
            registry.remove(userId, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 前端定时发心跳，顺便让 nginx 等代理保持连接
        if (PING.equals(message.getPayload())) {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(PONG));
                }
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("消息连接异常：{}", exception.getMessage());
    }

    private Long userId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId instanceof Long value ? value : null;
    }
}
