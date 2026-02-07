package io.github.iweidujiang.industry.plc.factory;

import io.github.iweidujiang.industry.plc.adapter.PlcProtocolAdapter;
import io.github.iweidujiang.industry.plc.model.DeviceConfig;

/**
 * PLC 连接器工厂
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/1
 */
public interface PlcConnectionFactory {
    PlcProtocolAdapter createAdapter(DeviceConfig config);
}
