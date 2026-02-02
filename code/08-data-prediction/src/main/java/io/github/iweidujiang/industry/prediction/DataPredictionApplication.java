package io.github.iweidujiang.industry.prediction;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动类
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/2
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("io.github.iweidujiang.industry.prediction.mapper")
public class DataPredictionApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataPredictionApplication.class, args);
        System.out.println("轻量化 AI 异常预判服务启动成功");
    }
}
