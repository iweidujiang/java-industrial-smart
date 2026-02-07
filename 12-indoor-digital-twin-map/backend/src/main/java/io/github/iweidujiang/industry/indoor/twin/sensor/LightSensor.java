package io.github.iweidujiang.industry.indoor.twin.sensor;

import io.github.iweidujiang.industry.indoor.twin.model.SensorData;

import java.util.Random;

/**
 * 光照传感器实现
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 */
public class LightSensor implements Sensor {

    private final String id;
    private final String area;
    private final double[] position;
    private final Random random;

    /**
     * 构造函数
     * @param id 传感器ID
     * @param area 所在区域
     * @param position 位置坐标
     */
    public LightSensor(String id, String area, double[] position) {
        this.id = id;
        this.area = area;
        this.position = position;
        this.random = new Random();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return "light";
    }

    @Override
    public double[] getPosition() {
        return position;
    }

    @Override
    public String getArea() {
        return area;
    }

    @Override
    public SensorData collectData() {
        // 模拟光照数据，范围 100-1000 lux
        double baseLight = 500.0;
        double variation = random.nextDouble() * 400 - 200; // -200 到 200 的随机值
        double lightValue = baseLight + variation;
        
        return new SensorData(
                id,
                getType(),
                Math.round(lightValue * 100) / 100.0, // 保留两位小数
                "lux",
                position,
                area
        );
    }

}
