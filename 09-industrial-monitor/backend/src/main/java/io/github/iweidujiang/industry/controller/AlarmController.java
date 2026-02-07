package io.github.iweidujiang.industry.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.iweidujiang.industry.model.AlarmRecord;
import io.github.iweidujiang.industry.service.AlarmRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 告警管理 REST 控制器
 * 提供告警历史查询、确认等接口
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/3
 */
@RestController
@RequestMapping("/api/alarm")
public class AlarmController {
    private final AlarmRecordService alarmRecordService;

    public AlarmController(AlarmRecordService alarmRecordService) {
        this.alarmRecordService = alarmRecordService;
    }

    @GetMapping("/list")
    public Page<AlarmRecord> listAlarms(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return alarmRecordService.getRecentAlarms(page, size);
    }
}
