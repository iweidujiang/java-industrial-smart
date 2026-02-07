package io.github.iweidujiang.industry.modbus;

import com.fazecast.jSerialComm.SerialPort;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Modbus RTU 写寄存器工具类（功能码 06：写单个保持寄存器）
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/1/31
 */
public class ModbusRtuWriter {

    /**
     * 向 Modbus 设备写入一个 16 位寄存器值
     *
     * @param portName   串口名称，例如 "COM4"（Windows）或 "/dev/ttyUSB0"（Linux）
     * @param baudRate   波特率，常见值：9600、19200
     * @param deviceId   设备地址，取值范围 1~247
     * @param regAddress 寄存器地址（0 起始）
     * @param value      要写入的值，范围 0~65535
     * @throws Exception 串口打开失败、设备无响应等异常
     */
    public static void writeRegister(String portName, int baudRate,
                                     int deviceId, int regAddress, int value) throws Exception {
        if (deviceId < 1 || deviceId > 247) {
            throw new IllegalArgumentException("设备地址必须在 1 到 247 之间");
        }
        if (value < 0 || value > 65535) {
            throw new IllegalArgumentException("寄存器值必须在 0 到 65535 之间");
        }

        SerialPort serialPort = SerialPort.getCommPort(portName);
        serialPort.setComPortParameters(baudRate, 8, 1, SerialPort.NO_PARITY);
        serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        boolean opened = serialPort.openPort();
        if (!opened) {
            throw new RuntimeException("无法打开串口：" + portName);
        }

        try {
            // 构造 Modbus 请求帧（6 字节：设备ID + 功能码 + 寄存器地址 + 值）
            ByteBuffer buffer = ByteBuffer.allocate(6).order(ByteOrder.BIG_ENDIAN);
            buffer.put((byte) deviceId);
            buffer.put((byte) 0x06); // 功能码 06：写单寄存器
            buffer.putShort((short) regAddress);
            buffer.putShort((short) value);

            byte[] frame = buffer.array();
            int crc = calculateCRC16(frame, frame.length);

            // 添加 CRC 校验（小端序：低字节在前）
            ByteBuffer fullFrame = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            fullFrame.put(frame);
            fullFrame.putShort((short) crc);
            byte[] finalFrame = fullFrame.array();

            // 发送数据
            int bytesWritten = serialPort.writeBytes(finalFrame, finalFrame.length);
            if (bytesWritten != finalFrame.length) {
                throw new RuntimeException("串口写入不完整");
            }

            Thread.sleep(100); // 等待设备响应

            // 尝试读取响应（简化处理，仅检查是否有返回）
            byte[] response = new byte[8];
            int bytesRead = serialPort.readBytes(response, response.length);
            if (bytesRead == 0) {
                throw new RuntimeException("设备无响应，请检查接线、波特率或设备地址");
            }
            // TODO: 可进一步校验响应内容是否匹配请求
        } finally {
            serialPort.closePort();
        }
    }

    /**
     * 计算 Modbus RTU 协议使用的 CRC16 校验值（多项式 0xA001）
     *
     * @param data   待校验的数据
     * @param length 数据长度
     * @return CRC16 校验值
     */
    private static int calculateCRC16(byte[] data, int length) {
        int crc = 0xFFFF;
        for (int i = 0; i < length; i++) {
            crc ^= (data[i] & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 1) == 1) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        return crc;
    }
}
