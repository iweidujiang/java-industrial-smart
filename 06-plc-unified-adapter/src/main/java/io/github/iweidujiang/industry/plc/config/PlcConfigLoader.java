package io.github.iweidujiang.industry.plc.config;

import io.github.iweidujiang.industry.plc.model.DeviceConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;

/**
 * 手动加载 plc-config.yml
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/1
 */
public class PlcConfigLoader {
    public static List<DeviceConfig> loadDevices() {
        Yaml yaml = new Yaml();
        try (InputStream in = PlcConfigLoader.class
                .getClassLoader()
                .getResourceAsStream("plc-config.yml")) {

            if (in == null) {
                throw new RuntimeException("未找到配置文件：plc-config.yml");
            }

            // 使用 ConfigWrapper 包装结构
            PlcConfigWrapper wrapper = yaml.loadAs(in, PlcConfigWrapper.class);
            return wrapper.getDevices();

        } catch (Exception e) {
            throw new RuntimeException("加载 plc-config.yml 失败", e);
        }
    }

}
