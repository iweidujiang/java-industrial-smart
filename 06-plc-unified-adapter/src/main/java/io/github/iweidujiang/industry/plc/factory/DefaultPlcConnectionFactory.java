package io.github.iweidujiang.industry.plc.factory;

import io.github.iweidujiang.industry.plc.adapter.ModbusRtuAdapter;
import io.github.iweidujiang.industry.plc.adapter.PlcProtocolAdapter;
import io.github.iweidujiang.industry.plc.adapter.SiemensS7Adapter;
import io.github.iweidujiang.industry.plc.model.DeviceConfig;
import org.springframework.stereotype.Component;

/**
 * 默认的PLC连接工厂
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/1
 */
@Component
public class DefaultPlcConnectionFactory implements PlcConnectionFactory {

    @Override
    public PlcProtocolAdapter createAdapter(DeviceConfig config) {
        switch (config.getProtocol()) {
            case "modbus":
                return new ModbusRtuAdapter(
                        config.getSerialPort(),
                        config.getBaudRate(),
                        config.getDeviceId()
                );
            case "siemens-s7":
                return new SiemensS7Adapter(config.getHost(), config.getPort());
            default:
                throw new IllegalArgumentException("不支持的协议: " + config.getProtocol());
        }
    }
}
