package io.github.iweidujiang.industry.prediction.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.iweidujiang.industry.prediction.config.PredictionThresholdConfig;
import io.github.iweidujiang.industry.prediction.mapper.DataPredictionMapper;
import io.github.iweidujiang.industry.prediction.model.DataPrediction;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据预判核心业务类
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/2
 */
@Slf4j
@Service
public class PredictionService extends ServiceImpl<DataPredictionMapper, DataPrediction> {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private PredictionThresholdConfig predictionConfig;

    // Redis key 规范
    private static final String REDIS_HISTORY_THRESHOLD = "industrial:prediction:history:";
    private static final String REDIS_REALTIME_DATA = "industrial:data:realtime";

    // 统计历史数据，计算阈值
    public void calculateHistoryThreshold(String deviceId, String dataType) {
        // 实际逻辑：从MySQL查询该设备、该类型最近30天正常数据，计算平均/最大/最小
        double avg = 27.5; // 模拟历史平均值（实际从MySQL查询计算）
        double min = 25.0;
        double max = 30.0;

        // 存入Redis，供后续预判使用
        String key = REDIS_HISTORY_THRESHOLD + deviceId + ":" + dataType;
        stringRedisTemplate.opsForHash().put(key, "avg", String.valueOf(avg));
        stringRedisTemplate.opsForHash().put(key, "min", String.valueOf(min));
        stringRedisTemplate.opsForHash().put(key, "max", String.valueOf(max));
    }

    // 实时预判（核心逻辑：对比历史阈值+趋势判断）
    @Transactional(rollbackFor = Exception.class)
    public void realtimePrediction() {
        List<DataPrediction> predictionList = new ArrayList<>();
        // 获取Redis中所有实时数据
        Map<Object, Object> realtimeDataMap = stringRedisTemplate.opsForHash().entries(REDIS_REALTIME_DATA);
        if (CollUtil.isEmpty(realtimeDataMap)) {
            log.info("无实时数据，跳过本次预判");
            return;
        }

        // 遍历实时数据，逐一生成预判结果
        for (Map.Entry<Object, Object> entry : realtimeDataMap.entrySet()) {
            String key = entry.getKey().toString(); // 格式：deviceId:dataType
            String dataJson = entry.getValue().toString();
            // 解析设备ID、数据类型、实时值
            String[] keyArr = key.split(":");
            String deviceId = keyArr[0];
            String dataType = keyArr[1];
            double realtimeValue = Double.parseDouble(JSONUtil.parseObj(dataJson).getStr("dataValue"));

            // 获取历史阈值，无阈值则先统计
            String historyKey = REDIS_HISTORY_THRESHOLD + deviceId + ":" + dataType;
            Map<Object, Object> historyMap = stringRedisTemplate.opsForHash().entries(historyKey);
            if (CollUtil.isEmpty(historyMap)) {
                calculateHistoryThreshold(deviceId, dataType);
                continue;
            }

            // 核心预判判断（纯Java实现，无AI框架）
            double historyAvg = Double.parseDouble(historyMap.get("avg").toString());
            double historyMin = Double.parseDouble(historyMap.get("min").toString());
            double historyMax = Double.parseDouble(historyMap.get("max").toString());
            double fluctuation = historyAvg * predictionConfig.getFluctuation().getThreshold();
            Integer status = predictionConfig.getStatus().getNormal();
            String desc = "数据正常，无异常趋势";

            // 预警判断（有异常趋势，未超标）
            if (realtimeValue > historyAvg + fluctuation || realtimeValue < historyAvg - fluctuation) {
                status = predictionConfig.getStatus().getWarning();
                desc = "数据超出正常波动，有异常趋势（当前：" + realtimeValue + "）";
            }
            // 异常判断（已超标，联动后续告警）
            if (realtimeValue > historyMax || realtimeValue < historyMin) {
                status = predictionConfig.getStatus().getAbnormal();
                desc = "数据已超标，需立即检查（当前：" + realtimeValue + "）";
            }

            // 构建预判结果，加入列表
            DataPrediction prediction = new DataPrediction();
            prediction.setDeviceId(deviceId);
            prediction.setDataType(dataType);
            prediction.setDataValue(String.valueOf(realtimeValue));
            prediction.setHistoryAvg(historyAvg);
            prediction.setHistoryMin(historyMin);
            prediction.setHistoryMax(historyMax);
            prediction.setPredictionStatus(status);
            prediction.setPredictionDesc(desc);
            prediction.setPredictionTime(LocalDateTime.now());
            predictionList.add(prediction);
        }

        // 批量持久化预判结果
        if (CollUtil.isNotEmpty(predictionList)) {
            persistPredictionResult(predictionList);
        }
    }

    // 预判结果持久化
    @Transactional(rollbackFor = Exception.class)
    public void persistPredictionResult(List<DataPrediction> predictionList) {
        saveBatch(predictionList);
        log.info("预判结果持久化完成，共{}条", predictionList.size());
    }
}
