package io.github.iweidujiang.industry.datacache.config;

import io.github.iweidujiang.industry.datacache.service.DataCacheService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务执行类（定时执行持久化和重试逻辑）
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/2
 */
@Component
public class ScheduledTask {

    @Resource
    private DataCacheService dataCacheService;

    /**
     * 定时执行异步持久化（每30分钟执行一次，可自定义 cron 表达式）
     * cron表达式说明：0 0/30 * * * ? 表示每30分钟执行一次
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    public void scheduledBatchPersist() {
        dataCacheService.batchPersistData();
    }

    /**
     * 定时执行失败重试（每1分钟执行一次，确保失败数据及时重试）
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void scheduledRetryFailedData() {
        dataCacheService.retryFailedPersistData();
    }
}
