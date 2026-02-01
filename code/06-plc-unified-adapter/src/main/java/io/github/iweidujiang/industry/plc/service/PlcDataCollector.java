package io.github.iweidujiang.industry.plc.service;

import io.github.iweidujiang.industry.plc.adapter.PlcProtocolAdapter;
import io.github.iweidujiang.industry.plc.config.PlcConfigLoader;
import io.github.iweidujiang.industry.plc.connection.PlcConnectionManager;
import io.github.iweidujiang.industry.plc.exception.PlcException;
import io.github.iweidujiang.industry.plc.factory.PlcConnectionFactory;
import io.github.iweidujiang.industry.plc.model.DeviceConfig;
import io.github.iweidujiang.industry.plc.model.PlcConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PLC 数据采集调度服务
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/1
 */
@Slf4j
@Service
public class PlcDataCollector {
    private final List<DeviceConfig> devices = PlcConfigLoader.loadDevices();
    private final PlcConnectionManager connectionManager;

    public PlcDataCollector(PlcConnectionManager plcConnectionManager) {
        this.connectionManager = plcConnectionManager;
    }

    @Scheduled(fixedDelay = 10_000)
    public void collectAll() {
        for (DeviceConfig device : devices) {
            try {
                PlcProtocolAdapter adapter = connectionManager.getConnection(device);
                Map<String, Object> data = adapter.readDataPoints(device.getPoints());
                log.info("✅ 设备 [{}] 采集成功: {}", device.getName(), data);
            } catch (Exception e) {
                log.error("❌ 设备 [{}] 采集失败: {}", device.getName(), e.getMessage());
            }
        }
    }
}
