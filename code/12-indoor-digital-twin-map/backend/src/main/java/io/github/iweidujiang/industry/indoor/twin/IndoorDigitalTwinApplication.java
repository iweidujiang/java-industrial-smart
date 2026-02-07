package io.github.iweidujiang.industry.indoor.twin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 室内环境数字孪生地图应用主类
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 */
@SpringBootApplication
@EnableScheduling
public class IndoorDigitalTwinApplication {

    public static void main(String[] args) {
        SpringApplication.run(IndoorDigitalTwinApplication.class, args);
    }

}
