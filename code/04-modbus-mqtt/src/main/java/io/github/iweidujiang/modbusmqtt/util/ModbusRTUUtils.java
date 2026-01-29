package io.github.iweidujiang.modbusmqtt.util;

import com.fazecast.jSerialComm.SerialPort;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Modbus RTU 工具类（含 CRC16 计算）
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/1/21
 */
public class ModbusRTUUtils {

    /**
     * 模拟读取温度
     */
    public static double readTemperature(String portName, int baudRate) throws Exception {
        SerialPort serialPort = SerialPort.getCommPort(portName);
        serialPort.setComPortParameters(baudRate, 8, 1, 0);
        serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        serialPort.openPort();

        try {
            // 构造 Modbus RTU 请求：设备ID=1, 功能码=3 (读保持寄存器), 地址=0, 数量=1
            byte[] request = {0x01, 0x03, 0x00, 0x00, 0x00, 0x01, (byte) 0xCD, (byte) 0xCA};
            serialPort.writeBytes(request, request.length);

            Thread.sleep(100); // 等待响应

            byte[] response = new byte[9];
            int read = serialPort.readBytes(response, response.length);
            if (read < 7) throw new RuntimeException("响应过短");

            // 解析温度：寄存器值 / 10.0
            ByteBuffer buffer = ByteBuffer.wrap(response);
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.position(3); // 跳过设备ID、功能码、字节数
            int rawValue = buffer.getShort() & 0xFFFF;
            return rawValue / 10.0;
        } finally {
            serialPort.closePort();
        }
    }

    /** 计算 Modbus RTU 标准 CRC16（低位在前） */
    public static int calculateCRC(byte[] data, int offset, int length) {
        int crc = 0xFFFF;
        for (int i = offset; i < offset + length; i++) {
            crc ^= (data[i] & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 1) == 1) {
                    crc >>= 1;
                    crc ^= 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        return crc;
    }

    /** 构建“读保持寄存器”请求帧（功能码 03） */
    public static byte[] buildReadHoldingRegistersFrame(int slaveId, int startAddress, int quantity) {
        byte[] pdu = new byte[6];
        pdu[0] = (byte) slaveId;
        pdu[1] = 0x03; // 功能码
        pdu[2] = (byte) (startAddress >> 8);
        pdu[3] = (byte) (startAddress & 0xFF);
        pdu[4] = (byte) (quantity >> 8);
        pdu[5] = (byte) (quantity & 0xFF);

        int crc = calculateCRC(pdu, 0, 6);
        byte[] frame = Arrays.copyOf(pdu, 8);
        frame[6] = (byte) (crc & 0xFF);      // CRC 低字节
        frame[7] = (byte) ((crc >> 8) & 0xFF); // CRC 高字节
        return frame;
    }

    /** 从响应帧中提取第一个寄存器的值（假设数量=1） */
    public static int extractRegisterValue(byte[] response) {
        if (response.length < 7) {
            throw new RuntimeException("响应长度不足");
        }
        if ((response[1] & 0xFF) != 0x03) {
            // 可能是异常响应（功能码 | 0x80）
            if ((response[1] & 0x80) == 0x80) {
                int exceptionCode = response[2] & 0xFF;
                throw new RuntimeException("设备返回异常码: " + exceptionCode);
            }
            throw new RuntimeException("非预期的功能码: " + (response[1] & 0xFF));
        }
        return ((response[3] & 0xFF) << 8) | (response[4] & 0xFF);
    }
}
