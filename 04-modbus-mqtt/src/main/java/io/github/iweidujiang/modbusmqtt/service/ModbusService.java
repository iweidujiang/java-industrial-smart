package io.github.iweidujiang.modbusmqtt.service;

import io.github.iweidujiang.modbusmqtt.util.ModbusRTUUtils;
import org.springframework.stereotype.Service;

/**
 * 业务逻辑
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/1/29
 */
@Service
public class ModbusService {

    // 串口名
    private final String PORT_NAME = "COM4"; // Windows 示例
    // private final String PORT_NAME = "/dev/ttyUSB0"; // Linux 示例
    // 波特率
    private final int BAUD_RATE = 9600;

    public double readTemperature() {
        try {
            return ModbusRTUUtils.readTemperature(PORT_NAME, BAUD_RATE);
        } catch (Exception e) {
            throw new RuntimeException("Modbus 读取失败: " + e.getMessage(), e);
        }
    }
}
