package io.github.iweidujiang.industry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 安全配置
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/3
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable() // 关闭 CSRF（前后端分离必需）
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/**").permitAll()   // 放行 API
                        .requestMatchers("/ws/**").permitAll()    // 放行 WebSocket 握手
                        .anyRequest().permitAll()                 // 其他也放行（开发阶段）
                )
                .httpBasic().disable()                        // 禁用 Basic Auth
                .formLogin().disable();                       // 禁用表单登录

        return http.build();
    }
}
