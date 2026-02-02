package io.github.iweidujiang.industry.datacache.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工业时序数据实体类
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/2
 */
@Data
@TableName("t_industrial_data")
public class IndustrialData {

    /** 主键（自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备ID（比如PLC的ID，区分不同设备的数据） */
    private String deviceId;

    /** 设备名称（比如「一号车间温度传感器」） */
    private String deviceName;

    /** 数据类型（比如temperature=温度、pressure=压力、speed=转速） */
    private String dataType;

    /** 数据值（比如25.5，存字符串兼容各种数据格式，也可以用Double） */
    private String dataValue;

    /** 采集时间戳（时序数据核心，必须有，精确到秒/毫秒） */
    private LocalDateTime collectTime;

    /** 数据状态（0=正常，1=异常，后续对接告警机制可用） */
    private Integer dataStatus;

    /** 创建时间 */
    private LocalDateTime createTime;

    // 自动填充创建时间（不用手动设置，后续用配置实现）
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = LocalDateTime.now();
    }
}
