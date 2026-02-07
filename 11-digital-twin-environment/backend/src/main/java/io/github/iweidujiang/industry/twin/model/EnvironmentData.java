package io.github.iweidujiang.industry.twin.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 温湿度环境数据实体
 * <p>
 * 字段说明：
 * - temperature: 摄氏度，有效范围 [0, 50]
 * - humidity: 相对湿度百分比，有效范围 [0, 100]
 * - timestamp: 数据生成时间戳（毫秒）
 * - alert: 是否触发告警（温度>35℃ 或 湿度>80%）
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/4
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnvironmentData {

    private double temperature;
    private double humidity;
    private long timestamp;
    private boolean alert;
}
