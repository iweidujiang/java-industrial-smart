package io.github.iweidujiang.industry.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.iweidujiang.industry.mapper.AlarmRecordMapper;
import io.github.iweidujiang.industry.model.AlarmRecord;
import io.github.iweidujiang.industry.service.AlarmRecordService;
import org.springframework.stereotype.Service;

/**
 * 告警记录服务实现类
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/3
 */
@Service
public class AlarmRecordServiceImpl extends ServiceImpl<AlarmRecordMapper, AlarmRecord> implements AlarmRecordService {

    @Override
    public Page<AlarmRecord> getRecentAlarms(int page, int size) {
        return this.page(new Page<>(page, size));
    }
}
