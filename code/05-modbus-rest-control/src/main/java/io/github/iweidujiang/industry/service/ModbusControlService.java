package io.github.iweidujiang.industry.service;

import io.github.iweidujiang.industry.modbus.ModbusRtuWriter;
import org.springframework.stereotype.Service;

/**
 * Modbus 控制业务服务类
 * 封装具体设备操作逻辑，对外提供语义化接口
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/1/31
 */
@Service
public class ModbusControlService {
    // ========== 配置项（也可读取 application.yml） ==========
    private final String SERIAL_PORT = "COM4"; // Windows 示例
    // private final String SERIAL_PORT = "/dev/ttyUSB0"; // Linux 示例
    private final int BAUD_RATE = 9600;
    private final int DEVICE_ID = 1;

    /**
     * 设置目标温度（单位：摄氏度，支持1位小数）
     * 对应设备寄存器地址：1
     * 例如：25.5℃ → 写入值 255
     */
    public void setTargetTemperature(double temperature) {
        if (temperature < 0 || temperature > 100) {
            throw new IllegalArgumentException("温度必须在 0 到 100 摄氏度之间");
        }
        int rawValue = (int) Math.round(temperature * 10); // 转换为整数（×10）
        try {
            ModbusRtuWriter.writeRegister(SERIAL_PORT, BAUD_RATE, DEVICE_ID, 1, rawValue);
        } catch (Exception e) {
            throw new RuntimeException("设置目标温度失败：" + e.getMessage(), e);
        }
    }

    /**
     * 控制水泵启停
     * 寄存器地址：10
     * 值：0=停止，1=启动
     */
    public void controlPump(boolean start) {
        try {
            ModbusRtuWriter.writeRegister(SERIAL_PORT, BAUD_RATE, DEVICE_ID, 10, start ? 1 : 0);
        } catch (Exception e) {
            throw new RuntimeException("控制水泵失败：" + e.getMessage(), e);
        }
    }

    /**
     * 通用寄存器写入接口
     */
    public void writeRegister(int address, int value) {
        try {
            ModbusRtuWriter.writeRegister(SERIAL_PORT, BAUD_RATE, DEVICE_ID, address, value);
        } catch (Exception e) {
            throw new RuntimeException("写入寄存器失败：" + e.getMessage(), e);
        }
    }
}
