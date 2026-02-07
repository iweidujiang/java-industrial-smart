package io.github.iweidujiang.industry.indoor.twin.config;

import io.github.iweidujiang.industry.indoor.twin.websocket.SensorWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SensorWebSocketHandler sensorWebSocketHandler;

    /**
     * 构造函数
     * @param sensorWebSocketHandler 传感器WebSocket处理器
     */
    public WebSocketConfig(SensorWebSocketHandler sensorWebSocketHandler) {
        this.sensorWebSocketHandler = sensorWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册WebSocket处理器，路径为 /ws/sensor
        registry.addHandler(sensorWebSocketHandler, "/ws/sensor")
                .setAllowedOrigins("*"); // 允许所有来源（生产环境应限制）
    }

}
