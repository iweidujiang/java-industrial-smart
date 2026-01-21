package io.github.iweidujiang.industry.modbusoverserial;

import com.fazecast.jSerialComm.SerialPort;
import io.github.iweidujiang.industry.modbusoverserial.util.ModbusRTUUtils;

import java.util.Arrays;

/**
 * Modbus RTU 读取
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/1/21
 */
public class ModbusRtuReader {
    public static void main(String[] args) {
        // 配置串口（必须与 Modbus Slave 完全一致！）
        String portName = "COM4"; // （Linux: "/dev/ttyUSB0"）
        SerialPort serialPort = SerialPort.getCommPort(portName);
        serialPort.setComPortParameters(9600, 8, 1, 0); // 9600,8,N,1
        serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);

        if (!serialPort.openPort()) {
            System.err.println("❌ 无法打开串口: " + portName);
            return;
        }

        try {
            // 构造请求：从站ID=1，读地址0（即40001），读1个寄存器
            byte[] request = ModbusRTUUtils.buildReadHoldingRegistersFrame(1, 0, 1);
            System.out.println("📤 发送请求: " + bytesToHex(request));

            // 发送并等待响应（简单延时，工业场景可优化）
            serialPort.getOutputStream().write(request);
            serialPort.getOutputStream().flush();

            Thread.sleep(100); // 等待设备响应（Modbus RTU 响应通常 < 50ms）

            // 读取响应
            byte[] buffer = new byte[256];
            int len = serialPort.getInputStream().read(buffer);
            if (len <= 0) {
                throw new RuntimeException("未收到响应");
            }
            byte[] response = Arrays.copyOf(buffer, len);
            System.out.println("📥 收到响应: " + bytesToHex(response));

            // 🔍 解析温度值
            int rawValue = ModbusRTUUtils.extractRegisterValue(response);
            double temperature = rawValue / 10.0; // 缩放因子：×10 存储
            System.out.printf("✅ 当前温度: %.1f ℃\n", temperature);

        } catch (Exception e) {
            System.err.println("💥 通信失败: " + e.getMessage());
        } finally {
            serialPort.closePort();
        }
    }

    // 辅助方法：字节数组转十六进制字符串（用于调试）
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim();
    }
}
