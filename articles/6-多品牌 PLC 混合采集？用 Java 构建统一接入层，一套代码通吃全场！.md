# 6-多品牌 PLC 混合采集？用 Java 构建统一接入层，一套代码通吃全场！

> **西门子、三菱、Modbus 老设备共存？别写四套轮子了，抽象+配置化才是正解！**
>
> 🧭 本文属于专栏《Java × 工业智能》第 6 篇 | GitHub 源码：[github.com/iweidujiang/java-industrial-smart](https://github.com/iweidujiang/java-industrial-smart)

------

## 一、真实痛点：一个车间，四种协议

在很多正在智能化改造的工厂里，你经常会遇到这样的场景：

- 新产线用的是 **西门子 S7-1200**，走 S7 协议；  
- 中期扩产买了 **三菱 FX5U**，用 MC 协议；  
- 老锅炉控制柜只留了个 **RS485 接口**，跑 Modbus RTU；  
- 还有一台进口包装机，文档都找不到，只知道它支持 **欧姆龙 Host Link**……

结果就是：**想把全厂数据采上来，得写四套通信逻辑**。不仅开发累，后期维护更是噩梦——改个寄存器地址，要改四个地方；加一台新设备，又要复制粘贴一堆代码。

有没有办法，**用一套架构，统一处理所有 PLC 类型**？

答案是：**抽象协议接口 + 配置驱动 + 连接池管理**。

下面我们来用 Java 打造一个**可扩展、易维护、高可用的 PLC 数据采集统一接入层**。



## 二、设计目标：我们要解决什么？

1. **协议无关**：上层业务不关心底层是 Modbus 还是 S7；  
2. **配置驱动**：新增设备或修改采集点，只需改 YAML，不用动代码；  
3. **插件扩展**：支持新品牌 PLC？实现一个适配器即可；  
4. **稳定可靠**：连接断了能自动重连，主备链路可切换；  
5. **轻量高效**：资源占用低，适合部署在边缘网关（后面准备专门文章讲解边缘计算，又挖坑！）。



## 三、整体架构：分层解耦，各司其职

![PLC混采统一架构](E:\文章\Java x 工业智能\6-多品牌 PLC 混合采集？用 Java 构建统一接入层，一套代码通吃全场！.assets\PLC混采统一架构.png)

这个架构的核心思想是：**让变化的部分（协议）被封装，让不变的部分（调度、配置、连接管理）复用**。



## 四、核心设计 1：定义统一协议接口

我们定义一个统一的接口，所有协议适配器都必须实现它：

```java
// io.github.iweidujiang.industry.plc.adapter.PlcProtocolAdapter
public interface PlcProtocolAdapter {

    /**
     * 读取一批数据点（寄存器、线圈、DB块等）
     * @param points 采集点列表，包含地址、数据类型等信息
     * @return Map<点名, 值>，例如 {"温度": 25.5, "电机状态": true}
     */
    Map<String, Object> readDataPoints(List<DataPoint> points) throws PlcException;

    /**
     * 建立物理连接（TCP/串口）
     */
    void connect() throws PlcException;

    /**
     * 断开连接并释放资源
     */
    void disconnect();
}
```

这样，无论底层是 TCP 还是串口，是字节序大端还是小端，上层都看到一样的方法。



## 五、核心设计 2：YAML 配置驱动一切

告别硬编码，用 `plc-config.yml` 定义所有设备和采集点：

```yaml
# src/main/resources/plc-config.yml
devices:
  - name: "注塑机-西门子"
    protocol: "siemens-s7"
    host: "192.168.10.50"
    port: 102
    points:
      - name: "料筒温度"
        address: "DB10.DBW20"
        dataType: "REAL"
      - name: "循环次数"
        address: "DB10.DBD24"
        dataType: "DINT"

  - name: "空压站-三菱"
    protocol: "mitsubishi-mc"
    host: "192.168.10.51"
    port: 5001
    points:
      - name: "压力值"
        address: "D200"
        dataType: "FLOAT"

  - name: "热水锅炉-Modbus"
    protocol: "modbus"
    serialPort: "/dev/ttyUSB0"
    baudRate: 9600
    deviceId: 1
    points:
      - name: "水位百分比"
        address: 100
        dataType: "UINT16"
```

> ✅ **好处显而易见**：
>
> - 运维人员可直接修改配置，无需程序员介入；
> - 测试环境 vs 生产环境，只需换配置文件；
> - 新增设备？复制一段 YAML，填上参数就行。



## 六、核心设计 3：连接池 + 故障自动恢复

PLC 网络不稳定是常态。我们通过连接池管理连接生命周期，并支持主备 IP 自动切换：

```java
@Slf4j
@Service
public class PlcConnectionManager {
    private final ConcurrentHashMap<String, PlcConnection> connections = new ConcurrentHashMap<>();

    private final PlcConnectionFactory plcConnectionFactory;

    public PlcConnectionManager(PlcConnectionFactory plcConnectionFactory) {
        this.plcConnectionFactory = plcConnectionFactory;
    }

    public PlcProtocolAdapter getConnection(DeviceConfig config) {
        String key = config.getName();
        PlcConnection conn = connections.get(key);

        if (conn == null || !conn.isHealthy()) {
            try {
                PlcProtocolAdapter adapter = plcConnectionFactory.createAdapter(config);
                adapter.connect();
                conn = new PlcConnection(adapter, config);
                connections.put(key, conn);
            } catch (Exception e) {
                throw new RuntimeException("创建连接失败: " + e.getMessage(), e);
            }
        }

        return conn.getAdapter();
    }

    private static class PlcConnection {
        @Getter
        private final PlcProtocolAdapter adapter;
        private final long createTime = System.currentTimeMillis();

        public PlcConnection(PlcProtocolAdapter adapter, DeviceConfig config) {
            this.adapter = adapter;
        }

        public boolean isHealthy() {
            return System.currentTimeMillis() - createTime < 300_000; // 5分钟超时
        }
    }
}
```

配合定时健康检查（例如每 30 秒 ping 一次），确保采集服务始终在线。



## 七、如何扩展新协议？三步走

假设后期要接入 **施耐德 Modbus TCP**（虽然也是 Modbus，但走以太网）：

1. **新建适配器类**  

   ```java
   public class ModbusTcpAdapter implements PlcProtocolAdapter {
       // 使用 jlibmodbus 实现 readDataPoints()
   }
   ```

2. **注册到适配器工厂**  

   ```java
   adapterRegistry.register("modbus-tcp", new ModbusTcpAdapter());
   ```

3. **在 YAML 中配置设备**  

   ```yaml
   - name: "配电柜-施耐德"
     protocol: "modbus-tcp"
     host: "192.168.10.60"
     port: 502
     points:
       - name: "电流"
         address: 40001
         dataType: "FLOAT"
   ```

整个过程**零侵入核心调度逻辑**，真正实现“开箱即用”。



## 八、GitHub 项目结构

```
java-industrial-smart/
└── code/
    ├── 05-modbus-rest-control
    └── 06-plc-unified-adapter      # ✅ 本文模块
        ├── pom.xml
        ├── src/main/resources/plc-config.yml
        └── src/main/java/io/github/iweidujiang/industry/plc/
            ├── adapter/            # 协议适配器
            ├── factory/            # 适配器工厂
            ├── model/              # DeviceConfig, DataPoint
            ├── connection/         # 连接池、健康检查
            └── service/            # PlcDataCollector（调度器）
```



## 九、小结

**抽象不是炫技，而是为了少加班。**

在工业现场，设备异构是常态。与其为每个品牌写一套采集代码，不如花一点时间做**合理的抽象和配置化设计**。

这套统一接入层的价值在于：

- **降低耦合**：业务逻辑与通信协议解耦；
- **提升效率**：新增设备从“几天”缩短到“几分钟”；
- **增强健壮性**：连接管理、重试、切换机制保障数据连续性。

> 好的工业软件，不是功能最多，而是**让复杂变得简单，让变化变得容易**。



------

**完整源码已开源**：
📁 模块路径：`code/06-plc-unified-adapter`

🔗 仓库地址：[github.com/iweidujiang/java-industrial-smart](https://github.com/iweidujiang/java-industrial-smart)

欢迎 Star & 提 Issue！

---

*本文属于专栏 《Java × 工业智能》第 5 篇*

*如果你对这个系列感兴趣，记得关注我哦！*