package io.github.iweidujiang.industry.plc.adapter;

import com.github.s7connector.api.S7Connector;
import com.github.s7connector.api.factory.S7ConnectorFactory;
import io.github.iweidujiang.industry.plc.exception.PlcException;
import io.github.iweidujiang.industry.plc.model.DataPoint;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 西门子 S7 协议适配器（基于 s7connector）
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/1
 */
public class SiemensS7Adapter implements PlcProtocolAdapter {

    private S7Connector connector;
    private final String host;
    private final int port;

    public SiemensS7Adapter(String host, int port) {
        this.host = host;
        this.port = port;
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
                point.parseSiemensAddress();

                Object value;
                if (point.isBool()) {
                    byte[] bytes = connector.read(
                            point.getDaveArea(),
                            point.getDbNumber(),
                            point.getByteOffset(),
                    1
                            );
                    value = ((bytes[0] >> point.getBitOffset()) & 1) == 1;
                } else {
                    byte[] bytes = connector.read(
                            point.getDaveArea(),
                            point.getDbNumber(),
                            point.getByteOffset(),
                            point.getSize()
                    );

                    value = convertBytesToObject(bytes, point.getSize());
                }

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
        try {
            connector = S7ConnectorFactory.buildTCPConnector()
                    .withHost(host)
                    .withPort(port)
                    .build();

        } catch (Exception e) {
            throw new PlcException("连接西门子 PLC 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 断开连接
     */
    @Override
    public void disconnect() throws PlcException {
        if (connector != null) {
            try {
                connector.close();
            } catch (Exception e) {
                throw new PlcException("断开连接西门子 PLC 失败: " + e.getMessage(), e);
            }
        }
    }

    private Object convertBytesToObject(byte[] bytes, int size) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return switch (size) {
            case 1 -> bytes[0] & 0xFF;
            case 2 -> (int) buffer.getShort();
            case 4 -> buffer.getFloat();
            default -> bytes;
        };
    }
}
