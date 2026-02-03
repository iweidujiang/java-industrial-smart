package io.github.iweidujiang.industry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.iweidujiang.industry.model.AlarmRecord;
import io.github.iweidujiang.industry.model.DataPointValue;
import io.github.iweidujiang.industry.websocket.SpringWebSocketHandler;
import io.github.iweidujiang.industry.websocket.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
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

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final AlertService alertService;

    public MockDataGenerator(RedisTemplate<String, String> redisTemplate,
                             ObjectMapper objectMapper,
                             AlertService alertService) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.alertService = alertService;
    }

    @Scheduled(fixedRate = 1000)
    public void generateMockData() {
        double temperature = 60 + Math.sin(System.currentTimeMillis() / 2000.0) * 5;
        double pressure = 0.8 + Math.random() * 0.05;

        Map<String, Double> values = Map.of("温度", temperature, "压力", pressure);

        try {
            String json = objectMapper.writeValueAsString(values);
            redisTemplate.opsForValue().set(
                    "device:mock-boiler:latest",
                    json,
                    Duration.ofSeconds(10)
            );

            if (temperature > 68) {
                alertService.triggerAlert("mock-boiler", "温度过高", String.format("%.1f℃", temperature));
            }

            log.debug("💾 写入模拟数据: {}", values);
        } catch (JsonProcessingException e) {
            log.error("生成模拟数据失败", e);
        }
    }
}
