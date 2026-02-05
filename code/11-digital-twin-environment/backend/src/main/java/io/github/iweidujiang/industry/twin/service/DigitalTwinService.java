package io.github.iweidujiang.industry.twin.service;

import io.github.iweidujiang.industry.twin.websocket.EnvironmentBroadcaster;
import io.github.iweidujiang.industry.twin.model.EnvironmentData;
import io.github.iweidujiang.industry.twin.mock.EnvironmentSimulator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;

/**
 * 数字孪生主服务
 * <p>
 * 负责：
 * 1. 定时生成模拟环境数据
 * 2. 将数据存入 Redis（供其他服务查询）
 * 3. 通过 WebSocket 广播数据到前端
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/4
 */
@Configuration
@EnableScheduling
public class DigitalTwinService {
    @Autowired
    private EnvironmentSimulator simulator;

    @Autowired
    private EnvironmentBroadcaster broadcaster;

    @Autowired
    private RedisTemplate<String, EnvironmentData> redisTemplate;

    /**
     * 每秒执行一次：生成数据 → 存 Redis → 广播到前端
     */
    @Scheduled(fixedDelay = 1000)
    public void simulateAndBroadcast() {
        EnvironmentData data = simulator.generateNext();
        // 保存到 Redis，有效期10秒（避免陈旧数据）
        redisTemplate.opsForValue().set("env:latest", data, Duration.ofSeconds(10));
        // 推送至所有前端
        broadcaster.broadcast(data);
    }
}
