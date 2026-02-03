package io.github.iweidujiang.industry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 数据监控主启动类
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/3
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
public class IndustrialMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(IndustrialMonitorApplication.class, args);
        log.info("✅ 工业监控系统已启动！访问: http://localhost:8080");
    }
}
