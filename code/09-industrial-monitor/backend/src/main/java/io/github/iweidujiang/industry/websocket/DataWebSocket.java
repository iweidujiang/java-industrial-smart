package io.github.iweidujiang.industry.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时数据 WebSocket 服务端点
 * - 客户端通过 /ws/data/{deviceId} 连接
 * - 服务端主动推送最新数据点值
 * - 支持多客户端订阅同一设备
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/3
 */
@Slf4j
@Component
@ServerEndpoint("/ws/data/{deviceId}")
public class DataWebSocket {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    // deviceId -> (sessionId -> Session)
    private static final Map<String, Map<String, Session>> subscribers = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("deviceId") String deviceId) {
        subscribers.computeIfAbsent(deviceId, k -> new ConcurrentHashMap<>())
                .put(session.getId(), session);
        log.info("🔌 WebSocket 连接建立: deviceId={}, sessionId={}", deviceId, session.getId());
    }

    @OnClose
    public void onClose(Session session, @PathParam("deviceId") String deviceId) {
        Map<String, Session> sessions = subscribers.get(deviceId);
        if (sessions != null) {
            sessions.remove(session.getId());
            if (sessions.isEmpty()) {
                subscribers.remove(deviceId);
            }
        }
        log.info("📴 WebSocket 连接关闭: deviceId={}, sessionId={}", deviceId, session.getId());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("❌ WebSocket 错误", error);
    }

    /**
     * 静态方法，供服务层调用推送数据
     */
    public static void pushToSubscribers(String deviceId, Object data) {
        Map<String, Session> sessions = subscribers.get(deviceId);
        if (sessions == null || sessions.isEmpty()) return;

        String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("序列化失败", e);
            return;
        }

        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(json);
                }
            } catch (IOException e) {
                log.warn("推送失败: {}", e.getMessage());
            }
        });
    }
}
