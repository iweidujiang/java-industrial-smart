package io.github.iweidujiang.industry.datacache;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 主启动类
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/2
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("io.github.iweidujiang.industry.datacache.mapper")
public class DataCachePersistenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataCachePersistenceApplication.class, args);
        System.out.println("Java + Redis + MySQL 时序数据缓存与持久化项目启动成功！");
    }
}
