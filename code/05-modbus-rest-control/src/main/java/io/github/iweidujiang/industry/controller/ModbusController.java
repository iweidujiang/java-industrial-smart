package io.github.iweidujiang.industry.controller;

import io.github.iweidujiang.industry.service.ModbusControlService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Modbus REST 控制网关主启动类
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/1/31
 */
@RestController
@RequestMapping("/api/modbus")
public class ModbusController {

    private final ModbusControlService modbusService;

    public ModbusController(ModbusControlService modbusService) {
        this.modbusService = modbusService;
    }

    /**
     * 设置目标温度
     * 请求示例：{"temperature": 25.5}
     */
    @PostMapping("/temperature")
    public String setTemperature(@RequestBody Map<String, Double> request) {
        Double temp = request.get("temperature");
        if (temp == null) {
            throw new IllegalArgumentException("请求缺少 'temperature' 字段");
        }
        modbusService.setTargetTemperature(temp);
        return "目标温度已设置为 " + temp + "℃";
    }

    /**
     * 控制水泵启停
     * 请求示例：{"start": true}
     */
    @PostMapping("/pump")
    public String controlPump(@RequestBody Map<String, Boolean> request) {
        Boolean start = request.get("start");
        if (start == null) {
            throw new IllegalArgumentException("请求缺少 'start' 字段");
        }
        modbusService.controlPump(start);
        return "水泵已" + (start ? "启动" : "停止");
    }

    /**
     * 通用寄存器写入
     * 请求示例：{"address": 20, "value": 50}
     */
    @PostMapping("/write")
    public String writeRegister(@RequestBody Map<String, Integer> request) {
        Integer address = request.get("address");
        Integer value = request.get("value");
        if (address == null || value == null) {
            throw new IllegalArgumentException("请求缺少 'address' 或 'value' 字段");
        }
        modbusService.writeRegister(address, value);
        return "寄存器 " + address + " 已写入值 " + value;
    }
}
