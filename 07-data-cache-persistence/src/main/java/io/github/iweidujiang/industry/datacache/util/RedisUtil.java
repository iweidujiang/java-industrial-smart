package io.github.iweidujiang.industry.datacache.util;

import cn.hutool.core.collection.CollUtil;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis操作工具类
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/2
 */
@Component
public class RedisUtil {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 写入Redis（SortedSet类型，按时间戳排序，key=设备ID+数据类型，比如「device1_temperature」）
     * @param key Redis的key（区分不同设备、不同类型的数据）
     * @param value 数据值（这里存JSON字符串，包含完整的时序数据信息）
     * @param score 时间戳（用于排序，方便后续批量读取）
     */
    public void zAdd(String key, String value, double score) {
        stringRedisTemplate.opsForZSet().add(key, value, score);
        // 设置过期时间（防止Redis内存溢出，比如设置7天过期，兜底）
        stringRedisTemplate.expire(key, 7, TimeUnit.DAYS);
    }

    /**
     * 批量读取Redis中的数据（按时间戳范围读取，比如读取最近10分钟的数据）
     * @param key Redis的key
     * @param minScore 最小时间戳（开始时间）
     * @param maxScore 最大时间戳（结束时间）
     * @return 批量数据列表
     */
    public List<String> zRangeByScore(String key, double minScore, double maxScore) {
        Set<String> set = stringRedisTemplate.opsForZSet().rangeByScore(key, minScore, maxScore);
        return CollUtil.isNotEmpty(set) ? CollUtil.newArrayList(set) : CollUtil.newArrayList();
    }

    /**
     * 批量删除Redis中的数据（删除已经持久化到MySQL的数据）
     * @param key Redis的key
     * @param minScore 最小时间戳
     * @param maxScore 最大时间戳
     */
    public void zRemoveByScore(String key, double minScore, double maxScore) {
        stringRedisTemplate.opsForZSet().removeRangeByScore(key, minScore, maxScore);
    }

    /**
     * 判断Redis中是否存在某个key（用于判断设备是否有缓存数据）
     */
    public boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    /**
     * 批量删除Redis中的key（用于清理过期设备的数据）
     */
    public void deleteKeys(Collection<String> keys) {
        stringRedisTemplate.delete(keys);
    }
}
