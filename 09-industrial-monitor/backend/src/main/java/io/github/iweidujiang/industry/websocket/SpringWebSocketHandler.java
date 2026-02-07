package io.github.iweidujiang.industry.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 消息处理器
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/3
 */
@Slf4j
@Component
public class SpringWebSocketHandler extends TextWebSocketHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // deviceId -> List<WebSocketSession>
    private static final Map<String, List<WebSocketSession>> subscribers = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从路径中提取 deviceId
        String path = session.getUri().getPath(); // e.g. /ws/data/mock-boiler
        String[] parts = path.split("/");
        String deviceId = parts.length > 3 ? parts[3] : "default";

        subscribers.computeIfAbsent(deviceId, k -> new ArrayList<>()).add(session);
        log.info("✅ WebSocket 连接建立: deviceId='{}', sessionId='{}'", deviceId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        String deviceId = parts.length > 3 ? parts[3] : "default";

        List<WebSocketSession> sessions = subscribers.get(deviceId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                subscribers.remove(deviceId);
            }
        }
        log.info("📴 WebSocket 连接关闭: deviceId='{}'", deviceId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 前端一般只收不发，可留空
        log.debug("收到前端消息: {}", message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("❌ WebSocket 传输错误", exception);
        try {
            session.close();
        } catch (IOException e) {
            log.error("关闭会话失败", e);
        }
    }

    /**
     * 从路径中提取 deviceId
     * e.g. /ws/data/mock-boiler → mock-boiler
     */
    private String extractDeviceId(String path) {
        String[] parts = path.split("/");
        return parts.length > 3 ? parts[3] : "default";
    }

    /**
     * 静态方法：向指定设备的所有订阅者推送数据
     */
    public static void pushToSubscribers(String deviceId, Object data) {
        List<WebSocketSession> sessions = subscribers.get(deviceId);
        if (sessions == null || sessions.isEmpty()) {
            log.warn("⚠️ 无订阅者，deviceId='{}'", deviceId);
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(data);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            log.error("推送失败", e);
        }
    }
}
