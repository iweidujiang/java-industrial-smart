package io.github.iweidujiang.industry.indoor.twin.websocket;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.iweidujiang.industry.indoor.twin.model.SensorData;
import io.github.iweidujiang.industry.indoor.twin.sensor.SensorManager;
import io.github.iweidujiang.industry.indoor.twin.location.LocationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 传感器WebSocket处理器
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 */
@Component
public class SensorWebSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new ArrayList<>();
    private final SensorManager sensorManager;
    private final LocationService locationService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService executorService;

    /**
     * 构造函数
     * @param sensorManager 传感器管理器
     * @param locationService 位置服务
     */
    public SensorWebSocketHandler(SensorManager sensorManager, LocationService locationService) {
        this.sensorManager = sensorManager;
        this.locationService = locationService;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        startDataPushTask();
    }

    /**
     * 启动数据推送任务
     */
    private void startDataPushTask() {
        // 每1秒推送一次数据
        executorService.scheduleAtFixedRate(() -> {
            try {
                pushSensorData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * 推送传感器数据
     * @throws Exception 异常
     */
    private void pushSensorData() throws Exception {
        // 采集所有传感器数据
        List<SensorData> sensorDataList = sensorManager.collectAllSensorData();

        // 按仓库分组处理数据
        Map<String, Map<String, Object>> warehouseData = new HashMap<>();
        
        for (SensorData data : sensorDataList) {
            String warehouseName = data.getArea();
            String sensorType = data.getSensorType();
            
            warehouseData.computeIfAbsent(warehouseName, k -> {
                Map<String, Object> info = new HashMap<>();
                info.put("name", warehouseName);
                info.put("status", "正常");
                // 根据仓库名称设置粮种
                switch (warehouseName) {
                    case "1号仓":
                        info.put("grainType", "小麦");
                        break;
                    case "2号仓":
                        info.put("grainType", "玉米");
                        break;
                    case "3号仓":
                        info.put("grainType", "大豆");
                        break;
                    case "4号仓":
                        info.put("grainType", "水稻");
                        break;
                    case "5号仓":
                        info.put("grainType", "高粱");
                        break;
                    case "6号仓":
                        info.put("grainType", "大麦");
                        break;
                    case "7号仓":
                        info.put("grainType", "燕麦");
                        break;
                    case "8号仓":
                        info.put("grainType", "荞麦");
                        break;
                    default:
                        info.put("grainType", "未知");
                }
                return info;
            });
            
            if (sensorType.equals("temperature")) {
                warehouseData.get(warehouseName).put("temperature", data.getValue());
            } else if (sensorType.equals("humidity")) {
                warehouseData.get(warehouseName).put("humidity", data.getValue());
            }
        }

        // 构建推送数据
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("warehouseData", warehouseData);

        // 序列化数据
        String jsonData = objectMapper.writeValueAsString(data);

        // 推送到所有连接的客户端
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(jsonData));
            }
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("客户端连接成功: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("客户端连接关闭: " + session.getId());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 处理客户端消息
        System.out.println("收到客户端消息: " + message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket传输错误: " + exception.getMessage());
    }

}
