package io.github.iweidujiang.industry.prediction.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 阈值配置
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/2
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "prediction")
public class PredictionThresholdConfig {
    private History history;
    private Fluctuation fluctuation;
    private Realtime realtime;
    private Update update;
    private Status status;

    // 内部静态类，对应application.yml中预判配置层级
    @Data
    public static class History {
        private Integer days;    // 统计历史数据天数
        private Integer count;   // 最小历史数据量
    }
    @Data
    public static class Fluctuation {
        private Double threshold; // 数据波动阈值
    }
    @Data
    public static class Realtime {
        private Integer interval; // 实时预判间隔（分钟）
    }
    @Data
    public static class Update {
        private Integer interval; // 阈值更新间隔（分钟）
    }
    @Data
    public static class Status {
        private Integer normal;   // 正常状态码
        private Integer warning;  // 预警状态码
        private Integer abnormal; // 异常状态码
    }
}
