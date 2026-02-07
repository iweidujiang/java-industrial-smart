package io.github.iweidujiang.industry.indoor.twin.location;

import io.github.iweidujiang.industry.indoor.twin.model.SensorData;
import io.github.iweidujiang.industry.indoor.twin.model.IndoorEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * 空间定位服务
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/5
 */
@Slf4j
@Service
public class LocationService {
    /**
     * 基于传感器数据进行三角定位
     *
     * @param sensorDataList 传感器数据列表
     * @return 估计位置（x, y, z）
     */
    public double[] triangulatePosition(List<SensorData> sensorDataList) {
        if (sensorDataList == null || sensorDataList.size() < 3) {
            log.warn("三角定位需要至少3个传感器数据，当前只有 {} 个", sensorDataList != null ? sensorDataList.size() : 0);
            return new double[]{0, 0, 0};
        }
        
        // 这里使用简化的加权平均定位算法
        // 实际应用中可以使用更复杂的算法，如RSSI指纹定位、TOA/TDOA等
        
        double totalWeight = 0;
        double weightedX = 0;
        double weightedY = 0;
        double weightedZ = 0;
        
        for (SensorData data : sensorDataList) {
            // 根据传感器值计算权重
            // 这里假设传感器值越大，权重越高
            double weight = calculateWeight(data);
            totalWeight += weight;
            
            double[] position = data.getPosition();
            if (position != null && position.length >= 3) {
                weightedX += position[0] * weight;
                weightedY += position[1] * weight;
                weightedZ += position[2] * weight;
            }
        }
        
        if (totalWeight == 0) {
            log.warn("总权重为0，无法进行定位");
            return new double[]{0, 0, 0};
        }
        
        double[] position = new double[3];
        position[0] = weightedX / totalWeight;
        position[1] = weightedY / totalWeight;
        position[2] = weightedZ / totalWeight;
        
        log.debug("三角定位结果: [{}, {}, {}]", position[0], position[1], position[2]);
        return position;
    }
    
    /**
     * 计算传感器权重
     *
     * @param data 传感器数据
     * @return 权重值
     */
    private double calculateWeight(SensorData data) {
        double weight = 1.0;
        
        switch (data.getSensorType()) {
            case "temperature":
                // 温度传感器权重
                weight = 1.0;
                break;
            case "humidity":
                // 湿度传感器权重
                weight = 1.0;
                break;
            case "light":
                // 光照传感器权重，值越大权重越高
                weight = Math.min(2.0, data.getValue() / 500.0 + 0.5);
                break;
            case "motion":
                // 运动传感器权重，检测到运动时权重最高
                weight = data.getValue() > 0 ? 3.0 : 0.1;
                break;
            case "pressure":
                // 压力传感器权重
                weight = 1.0;
                break;
            default:
                weight = 1.0;
        }
        
        return weight;
    }
    
    /**
     * 基于区域的位置估计
     *
     * @param sensorDataList 传感器数据列表
     * @return 估计的区域名称
     */
    public String estimateArea(List<SensorData> sensorDataList) {
        if (sensorDataList == null || sensorDataList.isEmpty()) {
            log.warn("区域估计需要至少1个传感器数据");
            return "未知区域";
        }
        
        // 统计每个区域的传感器数据数量
        Map<String, Integer> areaCount = new HashMap<>();
        for (SensorData data : sensorDataList) {
            String area = data.getArea();
            areaCount.put(area, areaCount.getOrDefault(area, 0) + 1);
        }
        
        // 找出传感器数据最多的区域
        String estimatedArea = "未知区域";
        int maxCount = 0;
        
        for (Map.Entry<String, Integer> entry : areaCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                estimatedArea = entry.getKey();
            }
        }
        
        log.debug("区域估计结果: {}", estimatedArea);
        return estimatedArea;
    }
    
    /**
     * 构建传感器位置地图
     *
     * @param sensorDataList 传感器数据列表
     * @return 传感器位置地图
     */
    public Map<String, double[]> buildSensorMap(List<SensorData> sensorDataList) {
        Map<String, double[]> sensorMap = new HashMap<>();
        
        for (SensorData data : sensorDataList) {
            double[] position = data.getPosition();
            if (position != null && position.length >= 3) {
                sensorMap.put(data.getSensorId(), position);
            }
        }
        
        log.debug("构建传感器位置地图，包含 {} 个传感器", sensorMap.size());
        return sensorMap;
    }
    
    /**
     * 计算两点之间的距离
     *
     * @param point1 点1（x, y, z）
     * @param point2 点2（x, y, z）
     * @return 距离
     */
    public double calculateDistance(double[] point1, double[] point2) {
        if (point1 == null || point2 == null || point1.length < 3 || point2.length < 3) {
            log.warn("计算距离需要有效的三维坐标");
            return 0;
        }
        
        double dx = point1[0] - point2[0];
        double dy = point1[1] - point2[1];
        double dz = point1[2] - point2[2];
        
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * 计算点到区域的距离
     *
     * @param point 点（x, y, z）
     * @param area 区域
     * @return 距离
     */
    public double calculateDistanceToArea(double[] point, IndoorEnvironment.Area area) {
        if (point == null || area == null || area.getBoundaries() == null || area.getBoundaries().isEmpty()) {
            log.warn("计算点到区域的距离需要有效的点和区域边界");
            return 0;
        }
        
        // 计算点到区域边界的最小距离
        double minDistance = Double.MAX_VALUE;
        
        for (double[] boundaryPoint : area.getBoundaries()) {
            double[] boundaryCoords = new double[3];
            boundaryCoords[0] = boundaryPoint[0];
            boundaryCoords[1] = boundaryPoint[1];
            boundaryCoords[2] = boundaryPoint.length > 2 ? boundaryPoint[2] : 0;
            double distance = calculateDistance(point, boundaryCoords);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        
        return minDistance;
    }
    
    /**
     * 检查点是否在区域内
     *
     * @param point 点（x, y, z）
     * @param area 区域
     * @return 是否在区域内
     */
    public boolean isPointInArea(double[] point, IndoorEnvironment.Area area) {
        if (point == null || area == null || area.getBoundaries() == null || area.getBoundaries().size() < 3) {
            log.warn("检查点是否在区域内需要有效的点和区域边界");
            return false;
        }
        
        // 这里使用简化的2D多边形包含测试（忽略z坐标）
        // 实际应用中可以使用更复杂的3D空间包含测试
        
        int crossings = 0;
        List<double[]> boundaries = area.getBoundaries();
        int n = boundaries.size();
        
        for (int i = 0; i < n; i++) {
            double[] p1 = boundaries.get(i);
            double[] p2 = boundaries.get((i + 1) % n);
            
            // 检查射线与边界线段的交点
            if (((p1[1] > point[1]) != (p2[1] > point[1])) &&
                (point[0] < (p2[0] - p1[0]) * (point[1] - p1[1]) / (p2[1] - p1[1]) + p1[0])) {
                crossings++;
            }
        }
        
        // 交点数为奇数，则点在区域内
        return crossings % 2 == 1;
    }
    
    /**
     * 生成空间热图数据
     *
     * @param sensorDataList 传感器数据列表
     * @param resolution 分辨率（网格大小）
     * @return 热图数据（x, y, value）
     */
    public List<double[]> generateHeatmap(List<SensorData> sensorDataList, double resolution) {
        List<double[]> heatmapData = new ArrayList<>();
        
        if (sensorDataList == null || sensorDataList.isEmpty()) {
            log.warn("生成热图需要至少1个传感器数据");
            return heatmapData;
        }
        
        // 计算空间范围
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        
        for (SensorData data : sensorDataList) {
            double[] position = data.getPosition();
            if (position != null && position.length >= 2) {
                minX = Math.min(minX, position[0]);
                maxX = Math.max(maxX, position[0]);
                minY = Math.min(minY, position[1]);
                maxY = Math.max(maxY, position[1]);
            }
        }
        
        // 扩展边界，确保覆盖所有传感器
        minX -= resolution;
        maxX += resolution;
        minY -= resolution;
        maxY += resolution;
        
        // 生成网格点并计算每个点的值
        for (double x = minX; x <= maxX; x += resolution) {
            for (double y = minY; y <= maxY; y += resolution) {
                double[] point = {x, y, 0};
                double value = calculateHeatmapValue(point, sensorDataList);
                heatmapData.add(new double[]{x, y, value});
            }
        }
        
        log.debug("生成热图数据，包含 {} 个点", heatmapData.size());
        return heatmapData;
    }
    
    /**
     * 计算热图中某个点的值
     *
     * @param point 点（x, y, z）
     * @param sensorDataList 传感器数据列表
     * @return 热图值
     */
    private double calculateHeatmapValue(double[] point, List<SensorData> sensorDataList) {
        double totalValue = 0;
        double totalWeight = 0;
        
        for (SensorData data : sensorDataList) {
            double[] sensorPos = data.getPosition();
            if (sensorPos != null && sensorPos.length >= 3) {
                double distance = calculateDistance(point, sensorPos);
                
                // 距离越近，权重越高
                double weight = Math.max(0.1, 1.0 / (1.0 + distance * 0.1));
                totalWeight += weight;
                totalValue += data.getValue() * weight;
            }
        }
        
        return totalWeight > 0 ? totalValue / totalWeight : 0;
    }
}
