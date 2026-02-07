package io.github.iweidujiang.industry.indoor.twin.model;

import java.time.LocalDateTime;

/**
 * 传感器数据模型
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 */
public class SensorData {

    /**
     * 传感器ID
     */
    private String sensorId;

    /**
     * 传感器类型
     */
    private String sensorType;

    /**
     * 数据值
     */
    private double value;

    /**
     * 单位
     */
    private String unit;

    /**
     * 采集时间
     */
    private LocalDateTime timestamp;

    /**
     * 传感器位置
     */
    private double[] position;

    /**
     * 所在区域
     */
    private String area;

    // 构造函数
    public SensorData() {
    }

    public SensorData(String sensorId, String sensorType, double value, String unit, double[] position, String area) {
        this.sensorId = sensorId;
        this.sensorType = sensorType;
        this.value = value;
        this.unit = unit;
        this.timestamp = LocalDateTime.now();
        this.position = position;
        this.area = area;
    }

    // Getter 和 Setter 方法
    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public String getSensorType() {
        return sensorType;
    }

    public void setSensorType(String sensorType) {
        this.sensorType = sensorType;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public double[] getPosition() {
        return position;
    }

    public void setPosition(double[] position) {
        this.position = position;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

}
