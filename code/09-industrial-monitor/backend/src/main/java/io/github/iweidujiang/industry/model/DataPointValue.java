package io.github.iweidujiang.industry.model;

import lombok.Data;

import java.util.Map;

/**
 * 实时数据点值封装类
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/3
 */
@Data
public class DataPointValue {
    private String deviceId;      // 设备唯一标识
    private long timestamp;       // 时间戳（毫秒）
    private Map<String, Object> values; // 点名 -> 值，如 {"温度": 62.3, "压力": 0.85}
}
