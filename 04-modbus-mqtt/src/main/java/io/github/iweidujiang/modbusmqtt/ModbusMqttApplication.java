package io.github.iweidujiang.modbusmqtt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 主程序
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/1/29
 */
@SpringBootApplication
@EnableScheduling
public class ModbusMqttApplication {
    public static void main(String[] args) {
        SpringApplication.run(ModbusMqttApplication.class, args);
    }
}
