package io.github.iweidujiang.industry.twin.config;

import io.github.iweidujiang.industry.twin.model.EnvironmentData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/4
 */
@Configuration
public class RedisConfig {
    /**
     * 创建专用于 EnvironmentData 的 RedisTemplate
     * Key: String, Value: EnvironmentData (JSON 序列化)
     */
    @Bean
    public RedisTemplate<String, EnvironmentData> environmentRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, EnvironmentData> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 使用字符串序列化
        template.setKeySerializer(new StringRedisSerializer());

        // Value 使用 JSON 序列化（支持自定义对象）
        Jackson2JsonRedisSerializer<EnvironmentData> valueSerializer =
                new Jackson2JsonRedisSerializer<>(EnvironmentData.class);
        template.setValueSerializer(valueSerializer);

        // Hash 结构也使用相同序列化器
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
