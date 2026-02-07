package io.github.iweidujiang.industry.service;

import io.github.iweidujiang.industry.model.Alert;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 告警服务
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/3
 */
@Service
public class AlertService {
    private final Queue<Alert> recentAlerts = new ConcurrentLinkedQueue<>();

    public void triggerAlert(String deviceId, String message, String value) {
        Alert alert = new Alert(deviceId, message, value, Instant.now());
        recentAlerts.offer(alert);
        // 只保留 5 条最近的告警
        while (recentAlerts.size() > 5) {
            recentAlerts.poll();
        }
    }

    public List<Alert> getRecentAlerts() {
        return new ArrayList<>(recentAlerts);
    }
}
