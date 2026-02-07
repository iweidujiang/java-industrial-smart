package io.github.iweidujiang.modbusmqtt.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.iweidujiang.modbusmqtt.service.ModbusService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 定时采集发布
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/1/29
 */
@Component
public class DataCollector {
    private final ModbusService modbusService;
    private final MessageChannel mqttOutboundChannel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DataCollector(ModbusService modbusService,
                         @Qualifier("mqttOutboundChannel") MessageChannel mqttOutboundChannel) {
        this.modbusService = modbusService;
        this.mqttOutboundChannel = mqttOutboundChannel;
    }

    @Scheduled(fixedRate = 10_000)
    public void collectAndPublish() {
        try {
            double temperature = modbusService.readTemperature();
            String json = objectMapper.writeValueAsString(Map.of(
                    "temperature", temperature,
                    "timestamp", System.currentTimeMillis()
            ));

            Message<String> message = MessageBuilder.withPayload(json).build();
            boolean sent = mqttOutboundChannel.send(message, 5000);
            System.out.println(sent ? "📤 MQTT 发布成功: " + json : "❌ 发送超时");
        } catch (Exception e) {
            System.err.println("❌ 采集或发送失败: " + e.getMessage());
        }
    }
}
