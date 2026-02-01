package io.github.iweidujiang.industry.plc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * PLC 统一采集网关主程序
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/1
 */
@SpringBootApplication
@EnableScheduling
public class PlcUnifiedAdapterApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlcUnifiedAdapterApplication.class, args);
    }
}
