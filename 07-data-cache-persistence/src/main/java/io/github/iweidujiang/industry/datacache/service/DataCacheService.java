package io.github.iweidujiang.industry.datacache.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.iweidujiang.industry.datacache.model.IndustrialData;

/**
 * 时序数据缓存与持久化业务接口
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/2
 */
public interface DataCacheService extends IService<IndustrialData> {
    /**
     * 采集数据写入Redis缓存（高频采集入口）
     * @param industrialData 时序数据实体
     */
    void cacheIndustrialData(IndustrialData industrialData);

    /**
     * 异步批量将Redis中的数据写入MySQL（持久化入口）
     */
    void batchPersistData();

    /**
     * 重试失败的持久化数据（兜底保障，避免数据丢失）
     */
    void retryFailedPersistData();
}
