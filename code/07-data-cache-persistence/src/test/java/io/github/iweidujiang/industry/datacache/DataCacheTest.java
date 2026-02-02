package io.github.iweidujiang.industry.datacache;

import io.github.iweidujiang.industry.datacache.model.IndustrialData;
import io.github.iweidujiang.industry.datacache.service.DataCacheService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 测试类（模拟高频采集数据）
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/2
 */
@SpringBootTest
public class DataCacheTest {
    @Resource
    private DataCacheService dataCacheService;

    // 模拟高频采集：循环100次，每秒采集1次，模拟设备高频产生数据
    @Test
    public void testCacheIndustrialData() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            IndustrialData data = new IndustrialData();
            data.setDeviceId("device001"); // 设备ID，自定义
            data.setDeviceName("一号车间温度传感器");
            data.setDataType("temperature"); // 数据类型：温度
            data.setDataValue(String.valueOf(25.0 + i % 5)); // 模拟温度数据（25-30℃）

            // 调用方法，写入Redis
            dataCacheService.cacheIndustrialData(data);

            // 暂停1秒，模拟每秒采集1次
            Thread.sleep(1000);
        }
    }
}
