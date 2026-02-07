package io.github.iweidujiang.industry.datacache.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置类（配置线程池，优化异步任务执行效率）
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/2
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数（根据服务器配置调整，比如8）
        executor.setCorePoolSize(8);
        // 最大线程数
        executor.setMaxPoolSize(16);
        // 队列容量（缓存异步任务）
        executor.setQueueCapacity(100);
        // 线程空闲时间（超过这个时间，空闲线程会被销毁）
        executor.setKeepAliveSeconds(60);
        // 线程名称前缀（方便日志调试）
        executor.setThreadNamePrefix("industrial-async-");
        // 初始化线程池
        executor.initialize();
        return executor;
    }
}
