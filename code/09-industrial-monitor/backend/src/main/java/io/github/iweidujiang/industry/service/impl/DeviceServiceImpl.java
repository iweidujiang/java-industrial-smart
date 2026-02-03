package io.github.iweidujiang.industry.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.iweidujiang.industry.mapper.DeviceMapper;
import io.github.iweidujiang.industry.model.Device;
import io.github.iweidujiang.industry.service.DeviceService;
import org.springframework.stereotype.Service;

/**
 * 设备管理服务实现类
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/3
 */
@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {
}
