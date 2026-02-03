package io.github.iweidujiang.industry.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket 配置类
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/3
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    public WebSocketConfig() {
        System.out.println("✅ WebSocketConfig loaded!");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        System.out.println("🔌 Registering STOMP endpoint /ws");
        // 前端连接的 WebSocket 端点
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:5173") // Vue 默认端口
                .withSockJS(); // 支持降级
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单内存消息代理，支持 /topic/** 广播
        registry.enableSimpleBroker("/topic");
        // 客户端发送命令的前缀（如 /app/command）
        registry.setApplicationDestinationPrefixes("/app");
    }
}
