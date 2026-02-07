package io.github.iweidujiang.industry.indoor.twin.sensor;

import io.github.iweidujiang.industry.indoor.twin.model.SensorData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 传感器管理器
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/5
 */
@Slf4j
@Component
public class SensorManager {
    // 所有传感器实例，按传感器ID索引
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    
    // 按区域分组的传感器
    private final Map<String, List<Sensor>> sensorsByArea = new ConcurrentHashMap<>();
    
    // 按类型分组的传感器
    private final Map<String, List<Sensor>> sensorsByType = new ConcurrentHashMap<>();
    
    /**
     * 构造函数
     */
    public SensorManager() {
        initializeDefaultSensors();
    }
    
    /**
     * 初始化默认传感器
     */
    private void initializeDefaultSensors() {
        // 1号仓传感器
        registerSensor(new TemperatureSensor("temp-001", "1号仓", new double[]{-100, 0, 1}));
        registerSensor(new HumiditySensor("humid-001", "1号仓", new double[]{-100, 0, 1}));

        // 2号仓传感器
        registerSensor(new TemperatureSensor("temp-002", "2号仓", new double[]{-30, 0, 1}));
        registerSensor(new HumiditySensor("humid-002", "2号仓", new double[]{-30, 0, 1}));

        // 3号仓传感器
        registerSensor(new TemperatureSensor("temp-003", "3号仓", new double[]{40, 0, 1}));
        registerSensor(new HumiditySensor("humid-003", "3号仓", new double[]{40, 0, 1}));

        // 4号仓传感器
        registerSensor(new TemperatureSensor("temp-004", "4号仓", new double[]{110, 0, 1}));
        registerSensor(new HumiditySensor("humid-004", "4号仓", new double[]{110, 0, 1}));

        // 5号仓传感器
        registerSensor(new TemperatureSensor("temp-005", "5号仓", new double[]{-100, 80, 1}));
        registerSensor(new HumiditySensor("humid-005", "5号仓", new double[]{-100, 80, 1}));

        // 6号仓传感器
        registerSensor(new TemperatureSensor("temp-006", "6号仓", new double[]{-30, 80, 1}));
        registerSensor(new HumiditySensor("humid-006", "6号仓", new double[]{-30, 80, 1}));

        // 7号仓传感器
        registerSensor(new TemperatureSensor("temp-007", "7号仓", new double[]{40, 80, 1}));
        registerSensor(new HumiditySensor("humid-007", "7号仓", new double[]{40, 80, 1}));

        // 8号仓传感器
        registerSensor(new TemperatureSensor("temp-008", "8号仓", new double[]{110, 80, 1}));
        registerSensor(new HumiditySensor("humid-008", "8号仓", new double[]{110, 80, 1}));
    }
    
    /**
     * 注册传感器
     *
     * @param sensor 传感器实例
     */
    public void registerSensor(Sensor sensor) {
        // 注册到全局传感器列表
        sensors.put(sensor.getId(), sensor);
        
        // 按区域分组
        String area = sensor.getArea();
        sensorsByArea.computeIfAbsent(area, k -> new ArrayList<>()).add(sensor);
        
        // 按类型分组
        String type = sensor.getType();
        sensorsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(sensor);
        
        log.debug("已注册传感器: {} (类型: {}, 区域: {})位置: [{}, {}, {}]",
            sensor.getId(), sensor.getType(), sensor.getArea(),
            sensor.getPosition()[0], sensor.getPosition()[1], sensor.getPosition()[2]);
    }
    
    /**
     * 获取所有传感器
     *
     * @return 传感器列表
     */
    public List<Sensor> getAllSensors() {
        return new ArrayList<>(sensors.values());
    }
    
    /**
     * 按区域获取传感器
     *
     * @param area 区域名称
     * @return 传感器列表
     */
    public List<Sensor> getSensorsByArea(String area) {
        return sensorsByArea.getOrDefault(area, new ArrayList<>());
    }
    
    /**
     * 按类型获取传感器
     *
     * @param type 传感器类型
     * @return 传感器列表
     */
    public List<Sensor> getSensorsByType(String type) {
        return sensorsByType.getOrDefault(type, new ArrayList<>());
    }
    
    /**
     * 获取传感器实例
     *
     * @param sensorId 传感器ID
     * @return 传感器实例
     */
    public Sensor getSensor(String sensorId) {
        return sensors.get(sensorId);
    }
    
    /**
     * 采集所有传感器数据
     *
     * @return 传感器数据列表
     */
    public List<SensorData> collectAllSensorData() {
        List<SensorData> dataList = new ArrayList<>();
        
        for (Sensor sensor : sensors.values()) {
            try {
                SensorData data = sensor.collectData();
                dataList.add(data);
            } catch (Exception e) {
                log.error("采集传感器数据失败: {}", sensor.getId(), e);
            }
        }
        
        return dataList;
    }
    
    /**
     * 按区域采集传感器数据
     *
     * @param area 区域名称
     * @return 传感器数据列表
     */
    public List<SensorData> collectSensorDataByArea(String area) {
        List<SensorData> dataList = new ArrayList<>();
        List<Sensor> areaSensors = sensorsByArea.getOrDefault(area, new ArrayList<>());
        
        for (Sensor sensor : areaSensors) {
            try {
                SensorData data = sensor.collectData();
                dataList.add(data);
            } catch (Exception e) {
                log.error("采集区域 {} 传感器数据失败: {}", area, sensor.getId(), e);
            }
        }
        
        return dataList;
    }
    
    /**
     * 按类型采集传感器数据
     *
     * @param type 传感器类型
     * @return 传感器数据列表
     */
    public List<SensorData> collectSensorDataByType(String type) {
        List<SensorData> dataList = new ArrayList<>();
        List<Sensor> typeSensors = sensorsByType.getOrDefault(type, new ArrayList<>());
        
        for (Sensor sensor : typeSensors) {
            try {
                SensorData data = sensor.collectData();
                dataList.add(data);
            } catch (Exception e) {
                log.error("采集类型 {} 传感器数据失败: {}", type, sensor.getId(), e);
            }
        }
        
        return dataList;
    }
    
    /**
     * 获取传感器统计信息
     *
     * @return 统计信息
     */
    public Map<String, Object> getSensorStats() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalSensors = sensors.size();
        
        stats.put("totalSensors", totalSensors);
        stats.put("areas", sensorsByArea.keySet().size());
        stats.put("sensorTypes", sensorsByType.keySet().size());
        
        // 按类型统计
        Map<String, Integer> typeStats = new HashMap<>();
        for (Map.Entry<String, List<Sensor>> entry : sensorsByType.entrySet()) {
            typeStats.put(entry.getKey(), entry.getValue().size());
        }
        stats.put("typeStats", typeStats);
        
        // 按区域统计
        Map<String, Integer> areaStats = new HashMap<>();
        for (Map.Entry<String, List<Sensor>> entry : sensorsByArea.entrySet()) {
            areaStats.put(entry.getKey(), entry.getValue().size());
        }
        stats.put("areaStats", areaStats);
        
        return stats;
    }
}
