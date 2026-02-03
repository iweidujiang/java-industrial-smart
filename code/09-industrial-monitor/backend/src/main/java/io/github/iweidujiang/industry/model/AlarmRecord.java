package io.github.iweidujiang.industry.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警记录实体类
 * 存储历史告警事件
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/3
 */
@Data
@TableName("t_alarm_record")
public class AlarmRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deviceId;      // 触发设备
    private String pointName;     // 触发点名，如 "温度"
    private Double currentValue;  // 当前值
    private Double threshold;     // 阈值
    private String level;         // 告警级别：WARNING / CRITICAL
    private Boolean acknowledged; // 是否已确认
    private LocalDateTime createTime;
}
