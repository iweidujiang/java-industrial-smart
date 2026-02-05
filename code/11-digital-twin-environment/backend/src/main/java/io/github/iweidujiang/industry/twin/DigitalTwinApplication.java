package io.github.iweidujiang.industry.twin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 数字孪生服务主启动类
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/4
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
public class DigitalTwinApplication {
    public static void main(String[] args) {
        SpringApplication.run(DigitalTwinApplication.class, args);
        log.info("数字孪生服务已启动！WebSocket 端点: ws://localhost/ws");
    }
}
