package io.github.iweidujiang.industry.plc.connection;

import io.github.iweidujiang.industry.plc.adapter.PlcProtocolAdapter;
import io.github.iweidujiang.industry.plc.factory.PlcConnectionFactory;
import io.github.iweidujiang.industry.plc.model.DeviceConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * PLC 连接管理器（带缓存和健康检查）
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/1
 */
@Slf4j
@Service
public class PlcConnectionManager {
    private final ConcurrentHashMap<String, PlcConnection> connections = new ConcurrentHashMap<>();

    private final PlcConnectionFactory plcConnectionFactory;

    public PlcConnectionManager(PlcConnectionFactory plcConnectionFactory) {
        this.plcConnectionFactory = plcConnectionFactory;
    }

    public PlcProtocolAdapter getConnection(DeviceConfig config) {
        String key = config.getName();
        PlcConnection conn = connections.get(key);

        if (conn == null || !conn.isHealthy()) {
            try {
                PlcProtocolAdapter adapter = plcConnectionFactory.createAdapter(config);
                adapter.connect();
                conn = new PlcConnection(adapter, config);
                connections.put(key, conn);
            } catch (Exception e) {
                throw new RuntimeException("创建连接失败: " + e.getMessage(), e);
            }
        }

        return conn.getAdapter();
    }

    private static class PlcConnection {
        @Getter
        private final PlcProtocolAdapter adapter;
        private final long createTime = System.currentTimeMillis();

        public PlcConnection(PlcProtocolAdapter adapter, DeviceConfig config) {
            this.adapter = adapter;
        }

        public boolean isHealthy() {
            return System.currentTimeMillis() - createTime < 300_000; // 5分钟超时
        }
    }
}
