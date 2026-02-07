package io.github.iweidujiang.industry.plc.model;

import lombok.Data;

import java.util.List;

/**
 * PLC 设备配置
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/1
 */
@Data
public class DeviceConfig {

    private String name;                // 设备名称
    private String protocol;            // 协议类型：modbus, siemens-s7
    private String host;                // TCP 主机地址
    private Integer port;               // TCP 端口
    private String serialPort;          // 串口名称（Modbus RTU 用）
    private Integer baudRate;           // 波特率
    private Integer deviceId;           // Modbus 设备ID
    private List<DataPoint> points;     // 采集点列表
}
