package io.github.iweidujiang.industry.controller;

import org.springframework.web.bind.annotation.GetMapping;
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
@RestController
@RequestMapping("/api/data")
public class DataController {

    @GetMapping("/latest")
    public Map<String, Object> getLatestData() {
        // 模拟设备数据（替换为你的真实逻辑）
        Map<String, Object> data = new HashMap<>();
        data.put("temperature", 50 + Math.random() * 20);
        data.put("pressure", 0.6 + Math.random() * 0.4);
        data.put("deviceId", "mock-boiler");
        data.put("timestamp", System.currentTimeMillis());
        return data;
    }
}
