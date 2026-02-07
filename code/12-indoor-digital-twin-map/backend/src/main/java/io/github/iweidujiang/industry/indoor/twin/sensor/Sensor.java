package io.github.iweidujiang.industry.indoor.twin.sensor;

import io.github.iweidujiang.industry.indoor.twin.model.SensorData;

/**
 * 传感器接口
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 */
public interface Sensor {

    /**
     * 获取传感器ID
     * @return 传感器唯一标识
     */
    String getId();

    /**
     * 获取传感器类型
     * @return 传感器类型
     */
    String getType();

    /**
     * 获取传感器位置
     * @return 位置坐标 [x, y, z]
     */
    double[] getPosition();

    /**
     * 获取传感器所在区域
     * @return 区域名称
     */
    String getArea();

    /**
     * 采集数据
     * @return 传感器数据
     */
    SensorData collectData();

}
