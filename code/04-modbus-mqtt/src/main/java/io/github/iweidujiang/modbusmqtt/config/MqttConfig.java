package io.github.iweidujiang.modbusmqtt.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * 配置 MQTT
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/1/29
 */
@Configuration
@EnableIntegration
public class MqttConfig {

    @Value("${mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${mqtt.client-id:modbus-gateway}")
    private String clientId;

    // 创建 MQTT 出站通道适配器
    @Bean
    public MessageChannel mqttOutboundChannel() {
        return MessageChannels.direct().get();
    }

    // 创建 MQTT 连接选项（EMQX 支持匿名连接）
    private MqttConnectOptions getMqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setAutomaticReconnect(true);
        options.setCleanSession(false); // 保留会话，避免重复订阅
        return options;
    }

    // 创建 MQTT 出站处理器
    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler =
                new MqttPahoMessageHandler(clientId, (MqttPahoClientFactory) getMqttConnectOptions());
        messageHandler.setDefaultTopic("devices/thermostat/telemetry");
        messageHandler.setAsync(true); // 异步发送，不阻塞主线程
        return messageHandler;
    }
}
