package io.github.iweidujiang.industry.twin.websocket;

import io.github.iweidujiang.industry.twin.model.EnvironmentData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 环境数据广播服务
 * <p>
 * 负责通过 WebSocket 向所有前端客户端推送最新温湿度数据。
 * 使用 Spring Messaging 的 SimpMessagingTemplate 实现。
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/4
 */
@Slf4j
@Service
public class EnvironmentBroadcaster {
    private final SimpMessagingTemplate messagingTemplate;

    public EnvironmentBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 广播环境数据到指定主题
     *
     * @param data 要广播的环境数据
     */
    public void broadcast(EnvironmentData data) {
        try {
            messagingTemplate.convertAndSend("/topic/environment", data);
            log.debug("已成功推送环境数据 - 温度: {}℃, 湿度: {}%",
                    data.getTemperature(), data.getHumidity());
        } catch (Exception e) {
            log.error("推送环境数据到 WebSocket 时发生异常", e);
        }
    }
}
