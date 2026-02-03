package io.github.iweidujiang.industry.websocket;

import lombok.Data;

/**
 * 消息载体类
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/3
 */
@Data
public class WebSocketMessage {
    private String type;      // 消息类型
    private Object data;      // 消息数据
    private String timestamp; // 时间戳
    private String message;   // 附加信息
}
