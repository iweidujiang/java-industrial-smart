package io.github.iweidujiang.industry.datacache.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.iweidujiang.industry.datacache.model.IndustrialData;
import org.apache.ibatis.annotations.Mapper;

/**
 * 时序数据访问层
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/2
 */
@Mapper
public interface IndustrialDataMapper extends BaseMapper<IndustrialData> {
    // 这里不用写任何方法，BaseMapper已经提供了所有需要的方法
}
