package io.github.iweidujiang.industry.plc.config;

import io.github.iweidujiang.industry.plc.model.DeviceConfig;

import java.util.List;

/**
 * PlcConfigWrapper
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/1
 */
public class PlcConfigWrapper {
    private List<DeviceConfig> devices;

    public List<DeviceConfig> getDevices() {
        return devices;
    }

    public void setDevices(List<DeviceConfig> devices) {
        this.devices = devices;
    }
}
