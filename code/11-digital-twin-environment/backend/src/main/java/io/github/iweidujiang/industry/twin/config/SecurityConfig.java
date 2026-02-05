package io.github.iweidujiang.industry.twin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置类
 * <p>
 * 配置安全规则，允许WebSocket端点的匿名访问
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/4
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF保护，因为WebSocket连接不需要
            .csrf().disable()
            // 配置授权规则
            .authorizeRequests()
            // 允许对WebSocket端点的匿名访问
            .requestMatchers("/ws/**").permitAll()
            // 允许对静态资源的匿名访问
            .requestMatchers("/").permitAll()
            // 其他所有请求都需要认证
            .anyRequest().authenticated();
        return http.build();
    }
}
