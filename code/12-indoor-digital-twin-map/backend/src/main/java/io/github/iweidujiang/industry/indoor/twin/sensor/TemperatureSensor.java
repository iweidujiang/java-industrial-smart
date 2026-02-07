package io.github.iweidujiang.industry.indoor.twin.sensor;

import io.github.iweidujiang.industry.indoor.twin.model.SensorData;

import java.util.Random;

/**
 * 温度传感器实现
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 */
public class TemperatureSensor implements Sensor {

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
    public TemperatureSensor(String id, String area, double[] position) {
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
        return "temperature";
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
        // 模拟温度数据，范围 20-30℃
        double baseTemp = 25.0;
        double variation = random.nextDouble() * 2 - 1; // -1 到 1 的随机值
        double tempValue = baseTemp + variation;
        
        return new SensorData(
                id,
                getType(),
                Math.round(tempValue * 100) / 100.0, // 保留两位小数
                "℃",
                position,
                area
        );
    }

}
