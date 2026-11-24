package com.mgmt.config;

import com.mgmt.module.message.MessageHandshakeInterceptor;
import com.mgmt.module.message.MessageWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 站内消息 WebSocket 端点：ws(s)://host/api/ws?token=xxx
 * 路径受 server.servlet.context-path=/api 影响，前端与 nginx 都按 /api/ws 访问。
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final MessageWebSocketHandler messageWebSocketHandler;
    private final MessageHandshakeInterceptor messageHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(messageWebSocketHandler, "/ws")
                .addInterceptors(messageHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
