package io.github.iweidujiang.industry.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 设备实体类
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/3
 */
@Data
@TableName("t_device")
public class Device {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;          // 设备名称，如 "锅炉-Modbus"
    private String deviceId;      // 唯一ID，如 "boiler-01"
    private String protocol;      // 协议类型：modbus / siemens-s7 / mitsubishi-mc
    private String host;          // IP 地址或串口号
    private Integer port;         // 端口（TCP）或波特率（串口）
    private String status;        // ONLINE / OFFLINE
}
