package io.github.iweidujiang.industry.twin.mock;

import io.github.iweidujiang.industry.twin.model.EnvironmentData;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * 温湿度环境模拟器
 * <p>
 * 模拟逻辑：
 * - 温度缓慢上升（模拟设备发热或日照）
 * - 湿度与温度呈负相关（高温通常伴随低湿）
 * - 数据平滑变化，避免突变
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/4
 */
@Component
public class EnvironmentSimulator {
    private double currentTemp = 25.0; // 初始温度（℃）
    private double currentHumidity = 60.0; // 初始湿度（%）
    private final Random random = new Random();

    /**
     * 生成下一时刻的环境数据
     *
     * @return 包含温度、湿度、时间戳和告警状态的数据对象
     */
    public EnvironmentData generateNext() {
        // 温度变化：缓慢上升趋势 + 小幅随机波动
        currentTemp += (random.nextDouble() - 0.3) * 0.5;
        // 限制温度在合理范围 [10, 45]℃
        currentTemp = Math.max(10.0, Math.min(45.0, currentTemp));

        // 湿度变化：与温度负相关 + 随机扰动
        currentHumidity = 80.0 - (currentTemp - 20.0) * 1.2 + (random.nextDouble() - 0.5) * 5.0;
        // 限制湿度在合理范围 [20, 90]%
        currentHumidity = Math.max(20.0, Math.min(90.0, currentHumidity));

        // 判断是否触发告警
        boolean isAlert = currentTemp > 35.0 || currentHumidity > 80.0;

        return new EnvironmentData(currentTemp, currentHumidity, System.currentTimeMillis(), isAlert);
    }
}
