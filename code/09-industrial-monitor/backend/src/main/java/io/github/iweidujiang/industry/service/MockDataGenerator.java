package io.github.iweidujiang.industry.service;

import io.github.iweidujiang.industry.model.AlarmRecord;
import io.github.iweidujiang.industry.model.DataPointValue;
import io.github.iweidujiang.industry.websocket.DataWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
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

    private final AlarmRecordService alarmRecordService;

    // 模拟设备ID
    private static final String MOCK_DEVICE_ID = "mock-boiler";

    public MockDataGenerator(AlarmRecordService alarmRecordService) {
        this.alarmRecordService = alarmRecordService;
    }

    @Scheduled(fixedRate = 1000)
    public void generateAndPush() {
        double temperature = 60 + ThreadLocalRandom.current().nextDouble(-5, 5);
        double pressure = 0.8 + ThreadLocalRandom.current().nextDouble(-0.1, 0.1);

        Map<String, Object> values = new ConcurrentHashMap<>();
        values.put("温度", temperature);
        values.put("压力", pressure);

        DataPointValue data = new DataPointValue();
        data.setDeviceId(MOCK_DEVICE_ID);
        data.setTimestamp(System.currentTimeMillis());
        data.setValues(values);

        // 推送
        DataWebSocket.pushToSubscribers(MOCK_DEVICE_ID, data);
        log.debug("📡 模拟数据推送: 温度={}℃, 压力={}MPa", temperature, pressure);

        // 简单告警检查
        if (temperature > 65) {
            AlarmRecord alarm = new AlarmRecord();
            alarm.setDeviceId(MOCK_DEVICE_ID);
            alarm.setPointName("温度");
            alarm.setCurrentValue(temperature);
            alarm.setThreshold(65.0);
            alarm.setLevel("WARNING");
            alarm.setAcknowledged(false);
            alarm.setCreateTime(LocalDateTime.now());
            alarmRecordService.save(alarm);
            log.warn("⚠️ 触发告警: 温度超限 ({}℃ > 65℃)", temperature);
        }
    }
}
