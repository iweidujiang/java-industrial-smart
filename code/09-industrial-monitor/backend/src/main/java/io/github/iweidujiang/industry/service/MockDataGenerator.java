package io.github.iweidujiang.industry.service;

import io.github.iweidujiang.industry.model.AlarmRecord;
import io.github.iweidujiang.industry.model.DataPointValue;
import io.github.iweidujiang.industry.websocket.SpringWebSocketHandler;
import io.github.iweidujiang.industry.websocket.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 模拟数据
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/3
 */
@Slf4j
@Service
public class MockDataGenerator {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final Random random = new Random();

    // 每 2 秒推送一次设备数据
//    @Scheduled(fixedDelay = 2000)
    public void broadcastDeviceData() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("temperature", 50 + random.nextDouble() * 20); // 50～70℃
            data.put("pressure", 0.6 + random.nextDouble() * 0.4);  // 0.6～1.0 MPa
            data.put("deviceId", "mock-boiler");
            data.put("timestamp", Instant.now().toString());

            WebSocketMessage message = new WebSocketMessage();
            message.setType("DEVICE_DATA");
            message.setData(data);
            message.setTimestamp(Instant.now().toString());

            // 推送到特定设备 topic
            messagingTemplate.convertAndSend("/topic/device/mock-boiler", message);
            log.debug("📡 推送设备数据: {}", data);

        } catch (Exception e) {
            log.error("推送设备数据失败", e);
        }
    }
}
