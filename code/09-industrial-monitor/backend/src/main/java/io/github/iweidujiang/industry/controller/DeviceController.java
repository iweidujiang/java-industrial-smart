package io.github.iweidujiang.industry.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.iweidujiang.industry.model.Device;
import io.github.iweidujiang.industry.service.DeviceService;
import org.springframework.web.bind.annotation.*;

/**
 * 设备管理 REST 控制器
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/3
 */
@RestController
@RequestMapping("/api/device")
public class DeviceController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping("/list")
    public IPage<Device> listDevices(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return deviceService.page(new Page<>(page, size));
    }

    @PostMapping
    public Device addDevice(@RequestBody Device device) {
        deviceService.save(device);
        return device;
    }
}
