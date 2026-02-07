package io.github.iweidujiang.industry.prediction.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预判结果实体
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/2
 */
@Data
@TableName("t_data_prediction")
public class DataPrediction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deviceId;    // 设备ID
    private String dataType;    // 数据类型（温度/压力/转速）
    private String dataValue;   // 实时数据值
    private Double historyAvg;  // 历史平均值
    private Double historyMin;  // 历史最小值
    private Double historyMax;  // 历史最大值
    private Integer predictionStatus; // 预判状态（0/1/2）
    private String predictionDesc;    // 预判描述
    private LocalDateTime predictionTime; // 预判时间
}
