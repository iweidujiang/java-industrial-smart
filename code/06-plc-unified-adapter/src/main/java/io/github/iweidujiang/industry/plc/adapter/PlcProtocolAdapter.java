package io.github.iweidujiang.industry.plc.adapter;

import io.github.iweidujiang.industry.plc.exception.PlcException;
import io.github.iweidujiang.industry.plc.model.DataPoint;

import java.util.List;
import java.util.Map;

/**
 * PLC 协议适配器接口
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/1
 */
public interface PlcProtocolAdapter {
    /**
     * 读取一批数据点
     * @param points 采集点列表
     * @return 点名 -> 值 的映射
     * @throws PlcException 通信失败
     */
    Map<String, Object> readDataPoints(List<DataPoint> points) throws PlcException;

    /**
     * 建立连接
     */
    void connect() throws PlcException;

    /**
     * 断开连接
     */
    void disconnect() throws PlcException;
}
