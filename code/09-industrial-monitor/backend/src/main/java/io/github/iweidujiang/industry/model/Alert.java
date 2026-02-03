package io.github.iweidujiang.industry.model;

import lombok.Data;

import java.time.Instant;

/**
 * 告警
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/3
 */
@Data
public class Alert {
    private String deviceId;
    private String message;
    private String value;
    private Instant timestamp;

    public Alert(String deviceId, String message, String value, Instant timestamp) {
        this.deviceId = deviceId;
        this.message = message;
        this.value = value;
        this.timestamp = timestamp;
    }
}
