package io.github.iweidujiang.industry.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据展示
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/3
 */
@Slf4j
@RestController
@RequestMapping("/api/data")
public class DataController {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public DataController(RedisTemplate<String, String> redisTemplate,
                          ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/latest/{deviceId}")
    public ResponseEntity<Map<String, Object>> getLatestData(@PathVariable String deviceId) {
        String key = "device:" + deviceId + ":latest";
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            log.warn("⚠️ 设备 {} 无缓存数据", deviceId);
            return ResponseEntity.notFound().build();
        }

        try {
            Map<String, Object> data = objectMapper.readValue(json, Map.class);
            log.debug("📡 返回设备 {} 最新数据: {}", deviceId, data);
            return ResponseEntity.ok(data);
        } catch (JsonProcessingException e) {
            log.error("解析设备 {} 数据失败", deviceId, e);
            return ResponseEntity.status(500).build();
        }
    }
}
