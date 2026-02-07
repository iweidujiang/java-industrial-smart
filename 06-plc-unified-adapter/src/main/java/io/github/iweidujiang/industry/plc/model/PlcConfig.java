package io.github.iweidujiang.industry.plc.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 全局 PLC 配置（从 plc-config.yml 加载）
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/1
 */
@Data
@Component
@ConfigurationProperties(prefix = "plc")
public class PlcConfig {
    private List<DeviceConfig> devices;
}
