# 从 Modbus 到 MQTT：用 Spring Boot 构建工业数据网关

在本系列前几篇中，主要介绍了：

- 用 Java 读取 Modbus TCP/RTU 设备；
- 模拟从站、解析寄存器、处理串口通信。

但这些数据还“困”在本地。**如何让 Web 应用、手机 App 或云平台获取这些数据？**

答案是：**通过 MQTT 协议，将工业数据接入消息中间件**。

本文我们就构建一个**轻量级工业网关**：

1. 用 Java 定时读取 Modbus RTU 温控器（如前文 COM4）；
2. 将温度数据封装为 JSON；
3. 通过 MQTT 发布到本地 EMQX Broker；
4. 用任意 MQTT 客户端（如 MQTT Explorer）**即时验证数据是否到达**。

全程使用 **Spring Boot + Spring Integration + jSerialComm**，非常适合使用 Java 的童鞋。

## 一、为什么选 MQTT？

| 协议      | 适用场景           | 工业适用性                       |
| --------- | ------------------ | -------------------------------- |
| HTTP      | Web API            | ❌ 请求/响应，不适合设备主动上报  |
| WebSocket | 实时 Web           | ⚠️ 需长连接，资源消耗大           |
| **MQTT**  | 低带宽、不稳定网络 | ✅ 轻量、发布/订阅、支持离线、QoS |

> MQTT 是 IIoT（工业物联网）的事实标准，阿里云 IoT、华为 OceanConnect、AWS IoT Core 均原生支持。



## 二、整体架构示意图

![image-20260129175726056](E:\文章\Java x 工业智能\4-从 Modbus 到 MQTT：用 Java 搭建工业数据桥梁.assets\image-20260129175726056.png)

>  **核心思想**：Java 网关作为“协议转换器”，将**封闭的 Modbus 协议**转化为**开放的 MQTT 消息**，供上层应用消费。



## 三、实施步骤

### 3.1 启动本地 MQTT Broker（EMQX）

如果你不玩虚拟机、没有云服务器、也没有可使用的Linux服务器，可以在Windows上安装 Docker 来启动服务，非常爽。可以参考这篇文章：

[Windows 下 Docker 安装与使用全攻略](https://mp.weixin.qq.com/s/wKJrVvm6njPN9p0ge_CUtw)

#### 使用 Docker 启动

```bash
# 拉取镜像
docker pull docker.1ms.run/emqx
# 给镜像打标签
docker tag docker.1ms.run/emqx:latest emqx:stable

# 启动步骤
# 1.先不挂载数据卷，直接启动
docker run -d --name emqx -p 1883:1883 -p 18083:18083 emqx:stable

# 2.将配置文件和数据拷贝到我们的主机上进行持久化
docker cp emqx:/opt/emqx/etc/. E:\docker_service_data\emqx\etc\
docker cp emqx:/opt/emqx/data/. E:\docker_service_data\emqx\data\

# 3.挂载数据卷启动
docker run -d --name emqx -p 1883:1883 -p 18083:18083 -v E:/docker_service_data/emqx/etc:/opt/emqx/etc -v E:/docker_service_data/emqx/data:/opt/emqx/data -v E:/docker_service_data/emqx/log:/opt/emqx/log emqx:stable
```

- `1883`：MQTT 默认端口；
- `18083`：EMQX Dashboard（浏览器访问 `http://localhost:18083`，账号 `admin` / 密码 `public`）。

这里做一点说明：**为什么要先不挂载数据卷启动，再拷贝配置？**

**核心原因**：EMQX 容器在首次运行时，会根据镜像内置的默认配置自动初始化 `/opt/emqx/etc/` 目录中的 `emqx.conf` 文件。如果你直接挂载一个空的或不完整的 `etc/` 目录，EMQX 会认为“用户想自定义配置”，于是跳过默认配置生成流程，尝试加载一个不存在的 `emqx.conf`，最终导致启动失败（如报错 `emqx.conf is not found` 或 `illegal characters`）。

因此，需要使用我上面给出的启动步骤可以解决问题，即以下三步法：

1. **先不挂载任何数据卷**，直接启动容器，让 EMQX 自动创建并写入完整配置；
2. **通过 `docker cp` 将容器内的 `/opt/emqx/etc/` 和 `/opt/emqx/data/` 拷贝到主机** ， 实现配置和数据的持久化；
3. **再次启动容器，并挂载这三个目录**，确保每次重启都能加载正确配置。

> 这种方式保证了：
>
> - 配置文件格式完全兼容当前镜像版本；
> - 不会因手动编辑错误导致启动失败；
> - 数据和配置真正实现“持久化”，避免容器重建后丢失。

**验证**：打开浏览器访问 `http://localhost:18083`，登录后看到控制台即成功。

![image-20260129195855073](E:\文章\Java x 工业智能\4-从 Modbus 到 MQTT：用 Java 搭建工业数据桥梁.assets\image-20260129195855073.png)



### 3.2 创建 Spring Boot 项目

#### Maven 依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>

    <!-- jSerialComm: 串口通信 -->
    <dependency>
        <groupId>com.fazecast</groupId>
        <artifactId>jSerialComm</artifactId>
        <version>2.10.4</version>
    </dependency>

    <!-- Spring Integration MQTT -->
    <dependency>
        <groupId>org.springframework.integration</groupId>
        <artifactId>spring-integration-mqtt</artifactId>
    </dependency>

    <!-- JSON 处理 -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```



#### 实现 Modbus RTU 读取逻辑

```java
public class ModbusRTUUtils {

    /**
     * 模拟读取温度
     */
    public static double readTemperature(String portName, int baudRate) throws Exception {
        SerialPort serialPort = SerialPort.getCommPort(portName);
        serialPort.setComPortParameters(baudRate, 8, 1, 0);
        serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        serialPort.openPort();

        try {
            // 构造 Modbus RTU 请求：设备ID=1, 功能码=3 (读保持寄存器), 地址=0, 数量=1
            byte[] request = {0x01, 0x03, 0x00, 0x00, 0x00, 0x01, (byte) 0xCD, (byte) 0xCA};
            serialPort.writeBytes(request, request.length);

            Thread.sleep(100); // 等待响应

            byte[] response = new byte[9];
            int read = serialPort.readBytes(response, response.length);
            if (read < 7) throw new RuntimeException("响应过短");

            // 解析温度：寄存器值 / 10.0
            ByteBuffer buffer = ByteBuffer.wrap(response);
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.position(3); // 跳过设备ID、功能码、字节数
            int rawValue = buffer.getShort() & 0xFFFF;
            return rawValue / 10.0;
        } finally {
            serialPort.closePort();
        }
    }
}
```

**测试步骤**：

- 使用 **Modbus Slave**  在 COM3 模拟设备；
- Java 程序连接 COM4（虚拟串口对）；
- 设置寄存器 0 = 256 ： 返回 25.6℃。

#### 配置 Spring Integration MQTT

```java
@Configuration
@EnableIntegration
public class MqttConfig {

    @Value("${mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${mqtt.client-id:modbus-gateway}")
    private String clientId;

    // 创建 MQTT 出站通道适配器
    @Bean
    public MessageChannel mqttOutboundChannel() {
        return MessageChannels.direct().get();
    }

    // 创建 MQTT 连接选项（EMQX 支持匿名连接）
    private MqttConnectOptions getMqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setAutomaticReconnect(true);
        options.setCleanSession(false); // 保留会话，避免重复订阅
        return options;
    }

    // 创建 MQTT 出站处理器
    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler =
                new MqttPahoMessageHandler(clientId, (MqttPahoClientFactory) getMqttConnectOptions());
        messageHandler.setDefaultTopic("devices/thermostat/telemetry");
        messageHandler.setAsync(true); // 异步发送，不阻塞主线程
        return messageHandler;
    }
}
```



#### 定时采集并发布数据

```java
@Component
public class DataCollector {

    @Autowired
    private ModbusService modbusService;

    @Autowired
    private MqttService mqttService;

    @Scheduled(fixedRate = 10_000)
    public void collectAndPublish() {
        try {
            double temperature = modbusService.readTemperature();
            String payload = String.format(
                "{\"temperature\":%.1f,\"timestamp\":%d}",
                temperature, System.currentTimeMillis()
            );
            mqttService.publish("devices/thermostat/telemetry", payload);
        } catch (Exception e) {
            System.err.println("采集失败: " + e.getMessage());
        }
    }
}
```

#### 配置文件

```yaml
# src/main/resources/application.yml
mqtt:
  broker-url: tcp://localhost:1883
  client-id: modbus-gateway-${random.uuid}
```



## 四、查看 MQTT 数据

用开源工具 **MQTT Explorer**（https://mqtt-explorer.com/）即可：

1. 下载安装 MQTT Explorer；
2. 启动后，点击 **+ New Connection**；
3. 填写：
   - **Name**: `Local EMQX`
   - **Host**: `127.0.0.1`
   - **Port**: `1883`
   - **Client ID**: 任意（如 `test-client`）
4. 点击 **Save** → **Connect**；
5. 订阅主题：`devices/thermostat/telemetry`；
6. 启动 Java 程序，**立即看到 JSON 消息滚动出现**！

![image-20260130002846568](E:\文章\Java x 工业智能\4-从 Modbus 到 MQTT：用 Java 搭建工业数据桥梁.assets\image-20260130002846568.png)

![image-20260130002800750](E:\文章\Java x 工业智能\4-从 Modbus 到 MQTT：用 Java 搭建工业数据桥梁.assets\image-20260130002800750.png)



 这证明：**工业数据已成功进入消息总线**，任何订阅该主题的服务都能收到！



## 五、小结

- **Modbus 是“腿”**，负责走到设备；  
- **MQTT 是“嘴”**，负责把数据广播出去；  
- **Spring Boot + jSerialComm + spring-integration-mqtt** 即可完成轻量工业网关核心；  
- **EMQX + MQTT Explorer** 提供零配置验证环境。



## 结语

从串口字节到 MQTT 消息，只用了不到 200 行 Java 代码。
这正是工业智能化的第一步：**打通 OT 与 IT 的数据管道**。



> 所有代码已在 GitHub 整理为完整项目：
>
> [https://github.com/iweidujiang/java-industrial-smart](https://github.com/iweidujiang/java-industrial-smart)

------

*本文属于专栏 《Java × 工业智能》第 4 篇*

*如果你对这个系列感兴趣，记得关注我哦！*

