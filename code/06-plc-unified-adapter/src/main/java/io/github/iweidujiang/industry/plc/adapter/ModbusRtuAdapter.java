package io.github.iweidujiang.industry.plc.adapter;

import com.fazecast.jSerialComm.SerialPort;
import io.github.iweidujiang.industry.plc.exception.PlcException;
import io.github.iweidujiang.industry.plc.model.DataPoint;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modbus RTU 协议适配器（功能码 03：读保持寄存器）
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/1
 */
public class ModbusRtuAdapter implements PlcProtocolAdapter {

    private SerialPort serialPort;
    private final String serialPortName;
    private final int baudRate;
    private final int deviceId;

    public ModbusRtuAdapter(String serialPortName, int baudRate, int deviceId) {
        this.serialPortName = serialPortName;
        this.baudRate = baudRate;
        this.deviceId = deviceId;
    }

    /**
     * 读取一批数据点
     *
     * @param points 采集点列表
     * @return 点名 -> 值 的映射
     * @throws PlcException 通信失败
     */
    @Override
    public Map<String, Object> readDataPoints(List<DataPoint> points) throws PlcException {
        Map<String, Object> result = new HashMap<>();
        for (DataPoint point : points) {
            try {
                int address = Integer.parseInt(point.getAddress());
                int value = readRegister(address);
                // 简单类型转换（实际项目应根据 dataType 处理）
                result.put(point.getName(), value);
            } catch (Exception e) {
                throw new PlcException("读取点 [" + point.getName() + "] 失败: " + e.getMessage(), e);
            }
        }
        return result;
    }

    /**
     * 建立连接
     */
    @Override
    public void connect() throws PlcException {
        serialPort = SerialPort.getCommPort(serialPortName);
        serialPort.setComPortParameters(baudRate, 8, 1, SerialPort.NO_PARITY);
        serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        if (!serialPort.openPort()) {
            throw new PlcException("无法打开串口：" + serialPortName);
        }
    }

    /**
     * 断开连接
     */
    @Override
    public void disconnect() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
    }

    private int readRegister(int regAddress) throws PlcException {
        // 构造读请求帧（功能码 03）
        ByteBuffer buffer = ByteBuffer.allocate(6).order(ByteOrder.BIG_ENDIAN);
        buffer.put((byte) deviceId)
                .put((byte) 0x03)
                .putShort((short) regAddress)
                .putShort((short) 1); // 读1个寄存器

        byte[] frame = buffer.array();
        int crc = calculateCRC16(frame, frame.length);
        ByteBuffer fullFrame = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        fullFrame.put(frame).putShort((short) crc);
        byte[] request = fullFrame.array();

        serialPort.writeBytes(request, request.length);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PlcException("读取中断");
        }

        byte[] response = new byte[7];
        int read = serialPort.readBytes(response, response.length);
        if (read < 5) {
            throw new PlcException("设备无响应或响应不完整");
        }

        // 解析返回值（跳过设备ID、功能码、字节数）
        return ((response[3] & 0xFF) << 8) | (response[4] & 0xFF);
    }

    private int calculateCRC16(byte[] data, int length) {
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
