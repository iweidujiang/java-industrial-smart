package io.github.iweidujiang.industry.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import io.github.iweidujiang.industry.model.AlarmRecord;

/**
 * 告警记录服务接口
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/3
 */
public interface AlarmRecordService extends IService<AlarmRecord> {

    Page<AlarmRecord> getRecentAlarms(int page, int size);
}
