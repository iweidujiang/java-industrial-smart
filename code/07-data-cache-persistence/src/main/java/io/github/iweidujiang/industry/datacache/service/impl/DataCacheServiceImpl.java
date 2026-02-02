package io.github.iweidujiang.industry.datacache.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.iweidujiang.industry.datacache.mapper.IndustrialDataMapper;
import io.github.iweidujiang.industry.datacache.model.IndustrialData;
import io.github.iweidujiang.industry.datacache.service.DataCacheService;
import io.github.iweidujiang.industry.datacache.util.RedisUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 业务层实现类（核心逻辑：缓存+持久化）
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/2
 */
@Slf4j
@Service
public class DataCacheServiceImpl extends ServiceImpl<IndustrialDataMapper, IndustrialData> implements DataCacheService {
    @Resource
    private RedisUtil redisUtil;

    @Resource
    private IndustrialDataMapper industrialDataMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // Redis的key前缀（统一规范，方便后续清理，比如「industrial:data:device1_temperature」）
    private static final String REDIS_KEY_PREFIX = "industrial:data:";
    // 失败重试的Redis key（存放写入MySQL失败的数据）
    private static final String REDIS_FAILED_KEY = "industrial:data:failed";

    /**
     * 集数据写入Redis缓存（高频采集入口，同步执行，速度极快）
     */
    @Override
    public void cacheIndustrialData(IndustrialData industrialData) {
        try {
            // 1. 构建Redis的key：前缀 + 设备ID + 数据类型（区分不同设备、不同数据）
            String redisKey = REDIS_KEY_PREFIX + industrialData.getDeviceId() + "_" + industrialData.getDataType();

            // 2. 设置采集时间、创建时间（不用手动传，这里统一设置）
            LocalDateTime collectTime = LocalDateTime.now();
            industrialData.setCollectTime(collectTime);
            industrialData.setCreateTime(collectTime);
            industrialData.setDataStatus(0); // 默认数据正常

            // 3. 将实体类转为JSON字符串（Redis中存JSON，方便后续读取解析）
            String dataJson = JSONUtil.toJsonStr(industrialData);

            // 4. 写入Redis（SortedSet类型，score=时间戳（秒），按时间排序）
            double score = collectTime.toEpochSecond(ZoneOffset.of("+8"));
            redisUtil.zAdd(redisKey, dataJson, score);

            log.info("数据写入Redis成功：key={}, 数据={}", redisKey, dataJson);
        } catch (Exception e) {
            log.error("数据写入Redis失败，数据：{}，异常信息：{}", JSONUtil.toJsonStr(industrialData), e.getMessage());
            // 极端情况：Redis写入失败，直接暂存到失败队列，后续重试
            stringRedisTemplate.opsForList().leftPush(REDIS_FAILED_KEY, JSONUtil.toJsonStr(industrialData));
        }
    }

    /**
     * 异步批量将Redis中的数据写入MySQL（异步执行，不影响高频采集）
     * Async：Spring异步注解，开启独立线程执行，不用等执行完成
     */
    @Override
    @Async
    @Transactional // 事务注解，保证批量写入要么全成功，要么全失败，避免数据错乱
    public void batchPersistData() {
        try {
            log.info("开始执行异步持久化：从Redis批量读取数据，写入MySQL");

            // 1. 获取Redis中所有时序数据的key（所有设备、所有数据类型）
            Set<String> redisKeys = stringRedisTemplate.keys(REDIS_KEY_PREFIX + "*");
            if (redisKeys.isEmpty()) {
                log.info("Redis中无待持久化数据，结束本次持久化");
                return;
            }

            // 2. 遍历每个key，批量读取数据、写入MySQL
            for (String redisKey : redisKeys) {
                // 2.1 读取Redis中最近30分钟的数据（可自定义时间，比如10分钟、1小时）
                // 最小时间戳：当前时间 - 30分钟（秒）
                double minScore = LocalDateTime.now().minusMinutes(30).toEpochSecond(ZoneOffset.of("+8"));
                // 最大时间戳：当前时间（秒）
                double maxScore = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8"));
                List<String> dataJsonList = redisUtil.zRangeByScore(redisKey, minScore, maxScore);

                if (dataJsonList.isEmpty()) {
                    continue;
                }

                // 2.2 将JSON字符串转为实体类列表（批量插入MySQL）
                List<IndustrialData> dataList = new ArrayList<>();
                for (String dataJson : dataJsonList) {
                    IndustrialData data = JSONUtil.toBean(dataJson, IndustrialData.class);
                    dataList.add(data);
                }

                // 2.3 批量写入MySQL（MyBatis-Plus的批量插入方法，高效）
                this.saveBatch(dataList);
                log.info("批量写入MySQL成功：key={}，数据条数={}", redisKey, dataList.size());

                // 2.4 写入成功后，删除Redis中已持久化的数据（避免重复写入，节省内存）
                redisUtil.zRemoveByScore(redisKey, minScore, maxScore);
                log.info("删除Redis中已持久化数据：key={}，数据条数={}", redisKey, dataJsonList.size());
            }

        } catch (Exception e) {
            log.error("异步持久化失败，异常信息：{}", e.getMessage());
            // 这里可以做更细致的失败处理，比如将失败的key记录下来，后续重试
        }
    }

    /**
     * 重试失败的持久化数据（兜底保障，避免数据丢失）
     * 可配合定时任务执行，比如每分钟重试一次
     */
    @Override
    @Async
    @Transactional
    public void retryFailedPersistData() {
        try {
            log.info("开始重试失败的持久化数据");

            // 1. 读取Redis中失败的数据（列表类型，leftPop批量读取）
            List<String> failedJsonList = stringRedisTemplate.opsForList().range(REDIS_FAILED_KEY, 0, -1);
            if (failedJsonList == null || failedJsonList.isEmpty()) {
                log.info("无失败数据需要重试");
                return;
            }

            // 2. 批量写入MySQL
            List<IndustrialData> failedDataList = new ArrayList<>();
            for (String failedJson : failedJsonList) {
                IndustrialData data = JSONUtil.toBean(failedJson, IndustrialData.class);
                failedDataList.add(data);
            }
            this.saveBatch(failedDataList);
            log.info("重试失败数据写入MySQL成功，条数：{}", failedDataList.size());

            // 3. 重试成功后，删除Redis中的失败数据
            stringRedisTemplate.opsForList().trim(REDIS_FAILED_KEY, failedJsonList.size(), -1);

        } catch (Exception e) {
            log.error("重试失败数据持久化仍失败，异常信息：{}", e.getMessage());
            // 极端情况：多次重试失败，可发送告警通知（后续文章会讲告警机制）
        }
    }
}
