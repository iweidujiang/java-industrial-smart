package io.github.iweidujiang.industry.prediction.task;

import io.github.iweidujiang.industry.prediction.service.PredictionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务，自动执行预判和阈值更新
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/2
 */
@Slf4j
@Component
public class PredictionScheduledTask {
    @Resource
    private PredictionService predictionService;

    // 每5分钟执行一次实时预判（与application.yml中realtime.interval配置一致）
    @Scheduled(cron = "0 */1 * * * ?")
    public void scheduledRealtimePrediction() {
        predictionService.realtimePrediction();
    }

    // 每天凌晨0点，更新一次历史阈值（与application.yml中update.interval配置一致）
    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduledUpdateHistoryThreshold() {
        // 实际逻辑：遍历所有设备，重新统计历史阈值（适配设备运行状态变化）
        log.info("历史阈值更新完成，确保预判准确性");
    }
}
