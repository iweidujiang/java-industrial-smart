# 轻量化AI落地：Java + Spring Boot 实现设备异常预判

大家好，我是苏渡苇～ 继续《Java x 工业智能》合集第8篇实操！

聚焦轻量化AI落地，纯 **Java** 实现设备异常预判，无需复杂 AI 框架，全程实操带图、核心代码可直接复用，新手也能快速上手。



## 一、核心定位：预判 vs 告警（事前vs事后）

工业场景中，设备异常防控分为“事前预判”和“事后告警”，两者相辅相成、各司其职，核心区别如下：

- 告警：数据已异常 → 事后补救，避免故障扩大

- 预判：数据未异常、有异常趋势 → 事前预防，从源头规避故障



## 二、预判核心流程

全程贴合工业数据流转逻辑，无复杂架构，核心流程示意图如下：

![image-20260202203956136](E:\文章\Java x 工业智能\8-轻量化AI落地：Java + SpringBoot 实现设备异常预判.assets\image-20260202203956136.png)

核心 4 步：

1. 统计：从 MySQL 读取设备历史正常数据，计算正常范围+波动阈值

2. 采集：复用前序实时数据采集逻辑，从Redis获取实时数据

3. 预判：实时数据对比历史阈值，判断是否存在异常趋势

4. 处理：预判结果持久化，异常趋势触发提前提醒，联动后续告警

## 三、前置准备

快速上手秩序准备如下环境和数据：

1. 环境准备：JDK 17+、Redis 6.2+、MySQL 8.0+

2. 数据准备：MySQL 的 t_industrial_data 表，需提前准备至少50条设备正常时序数据（用于统计历史阈值，确保预判准确性）


## 四、核心落地实操

模块代码结构如下：

```plain text

└─code
    └─08-data-prediction
        └─src
            └─main
                ├─java
                │  └─io
                │      └─github
                │          └─iweidujiang
                │              └─industry
                │                  └─prediction
                │                      ├─config （配置类+定时任务）
                │                      ├─model （预判实体+阈值配置）
                │                      ├─mapper （数据访问层）
                │                      ├─service （预判核心业务）
                │                      └─PredictionApplication.java （启动类）
                └─resources （application.yml配置）
```

### 1. 核心依赖（pom.xml）

仅引入必要依赖，无冗余，适配Spring Boot 3.5.x + JDK 17+，无需新增任何AI框架。

> 其实大部分依赖在前几篇文章中已经提供了源码，但为了功能的独立性和阅读的便利，我在每篇文章中都明确给出依赖。

```xml
<groupId>io.github.iweidujiang.industry</groupId>
<artifactId>data-prediction</artifactId>
<version>1.0.0</version>

<name>data-prediction</name>
<description>设备数据异常预判</description>

<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.10</version>
    <relativePath/>
</parent>

<properties>
    <java.version>21</java.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <!-- MyBatis Plus -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.15</version>
    </dependency>
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>8.0.33</version>
        <scope>runtime</scope>
    </dependency>
    <!-- 工具类依赖（简化日期、字符串操作） -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.26</version>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Modbus RTU -->
    <dependency>
        <groupId>com.fazecast</groupId>
        <artifactId>jSerialComm</artifactId>
        <version>2.10.4</version>
    </dependency>
    <!-- 西门子 S7 协议 -->
    <dependency>
        <groupId>com.github.s7connector</groupId>
        <artifactId>s7connector</artifactId>
        <version>2.1</version>
        <scope>compile</scope>
    </dependency>
    <!-- YAML 配置解析 -->
    <dependency>
        <groupId>org.yaml</groupId>
        <artifactId>snakeyaml</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

</dependencies>
```

### 2. 核心配置（application.yml）

新增数据预判相关配置：

```yaml
# 异常预判核心配置（可根据设备调整）
prediction:
  history:
    days: 30    # 统计最近30天正常数据
    count: 50   # 最小历史数据量（避免统计不准）
  fluctuation:
    threshold: 0.1 # 波动阈值10%（平均值±10%为正常）
  realtime:
    interval: 5 # 每5分钟预判一次
  update:
    interval: 1440 # 每天更新一次历史阈值
  status:
    normal: 0   # 正常
    warning: 1  # 预警（有趋势）
    abnormal: 2 # 异常（已超标）
```

### 3. 核心实体

**预判结果实体（DataPrediction.java）：**

```java
@Data
@TableName("t_data_prediction")
public class DataPrediction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deviceId;    // 设备ID
    private String dataType;    // 数据类型（温度/压力/转速）
    private String dataValue;   // 实时数据值
    private Double historyAvg;  // 历史平均值
    private Double historyMin;  // 历史最小值
    private Double historyMax;  // 历史最大值
    private Integer predictionStatus; // 预判状态（0/1/2）
    private String predictionDesc;    // 预判描述
    private LocalDateTime predictionTime; // 预判时间
}
```

### 4. 配置类

阈值配置类：

```java
@Data
@Configuration
@ConfigurationProperties(prefix = "prediction")
public class PredictionThresholdConfig {
    private History history;
    private Fluctuation fluctuation;
    private Realtime realtime;
    private Update update;
    private Status status;

    // 内部静态类，对应application.yml中预判配置层级
    @Data
    public static class History {
        private Integer days;    // 统计历史数据天数
        private Integer count;   // 最小历史数据量
    }
    @Data
    public static class Fluctuation {
        private Double threshold; // 数据波动阈值
    }
    @Data
    public static class Realtime {
        private Integer interval; // 实时预判间隔（分钟）
    }
    @Data
    public static class Update {
        private Integer interval; // 阈值更新间隔（分钟）
    }
    @Data
    public static class Status {
        private Integer normal;   // 正常状态码
        private Integer warning;  // 预警状态码
        private Integer abnormal; // 异常状态码
    }
}
```

### 5. 预判核心逻辑

```java
@Slf4j
@Service
public class PredictionService extends ServiceImpl<DataPredictionMapper, DataPrediction> {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private PredictionThresholdConfig predictionConfig;

    // Redis key 规范
    private static final String REDIS_HISTORY_THRESHOLD = "industrial:prediction:history:";
    private static final String REDIS_REALTIME_DATA = "industrial:data:realtime:";

    // 统计历史数据，计算阈值
    public void calculateHistoryThreshold(String deviceId, String dataType) {
        // 实际逻辑：从MySQL查询该设备、该类型最近30天正常数据，计算平均/最大/最小
        double avg = 27.5; // 模拟历史平均值（实际从MySQL查询计算）
        double min = 25.0;
        double max = 30.0;

        // 存入Redis，供后续预判使用
        String key = REDIS_HISTORY_THRESHOLD + deviceId + ":" + dataType;
        stringRedisTemplate.opsForHash().put(key, "avg", String.valueOf(avg));
        stringRedisTemplate.opsForHash().put(key, "min", String.valueOf(min));
        stringRedisTemplate.opsForHash().put(key, "max", String.valueOf(max));
    }

    // 实时预判（核心逻辑：对比历史阈值+趋势判断）
    @Transactional(rollbackFor = Exception.class)
    public void realtimePrediction() {
        List<DataPrediction> predictionList = new ArrayList<>();
        // 获取Redis中所有实时数据（复用前序采集的实时数据）
        Map<Object, Object> realtimeDataMap = stringRedisTemplate.opsForHash().entries(REDIS_REALTIME_DATA);
        if (CollUtil.isEmpty(realtimeDataMap)) {
            log.info("无实时数据，跳过本次预判");
            return;
        }

        // 遍历实时数据，逐一生成预判结果
        for (Map.Entry<Object, Object> entry : realtimeDataMap.entrySet()) {
            String key = entry.getKey().toString(); // 格式：deviceId:dataType
            String dataJson = entry.getValue().toString();
            // 解析设备ID、数据类型、实时值
            String[] keyArr = key.split(":");
            String deviceId = keyArr[0];
            String dataType = keyArr[1];
            double realtimeValue = Double.parseDouble(JSONUtil.parseObj(dataJson).getStr("dataValue"));

            // 获取历史阈值，无阈值则先统计
            String historyKey = REDIS_HISTORY_THRESHOLD + deviceId + ":" + dataType;
            Map<Object, Object> historyMap = stringRedisTemplate.opsForHash().entries(historyKey);
            if (CollUtil.isEmpty(historyMap)) {
                calculateHistoryThreshold(deviceId, dataType);
                continue;
            }

            // 核心预判判断（纯Java实现，无AI框架）
            double historyAvg = Double.parseDouble(historyMap.get("avg").toString());
            double historyMin = Double.parseDouble(historyMap.get("min").toString());
            double historyMax = Double.parseDouble(historyMap.get("max").toString());
            double fluctuation = historyAvg * predictionConfig.getFluctuation().getThreshold();
            Integer status = predictionConfig.getStatus().getNormal();
            String desc = "数据正常，无异常趋势";

            // 预警判断（有异常趋势，未超标）
            if (realtimeValue > historyAvg + fluctuation || realtimeValue < historyAvg - fluctuation) {
                status = predictionConfig.getStatus().getWarning();
                desc = "数据超出正常波动，有异常趋势（当前：" + realtimeValue + "）";
            }
            // 异常判断（已超标，联动后续告警）
            if (realtimeValue > historyMax || realtimeValue < historyMin) {
                status = predictionConfig.getStatus().getAbnormal();
                desc = "数据已超标，需立即检查（当前：" + realtimeValue + "）";
            }

            // 构建预判结果，加入列表
            DataPrediction prediction = new DataPrediction();
            prediction.setDeviceId(deviceId);
            prediction.setDataType(dataType);
            prediction.setDataValue(String.valueOf(realtimeValue));
            prediction.setHistoryAvg(historyAvg);
            prediction.setHistoryMin(historyMin);
            prediction.setHistoryMax(historyMax);
            prediction.setPredictionStatus(status);
            prediction.setPredictionDesc(desc);
            prediction.setPredictionTime(LocalDateTime.now());
            predictionList.add(prediction);
        }

        // 批量持久化预判结果
        if (CollUtil.isNotEmpty(predictionList)) {
            persistPredictionResult(predictionList);
        }
    }

    // 预判结果持久化
    @Transactional(rollbackFor = Exception.class)
    public void persistPredictionResult(List<DataPrediction> predictionList) {
        saveBatch(predictionList);
        log.info("预判结果持久化完成，共{}条", predictionList.size());
    }
}
```

### 5. 启动类

独立启动，无需依赖其他模块，配置简单：

```java
@SpringBootApplication
@EnableScheduling
@MapperScan("io.github.iweidujiang.industry.prediction.mapper")
public class DataPredictionApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataPredictionApplication.class, args);
        System.out.println("轻量化 AI 异常预判服务启动成功");
    }
}
```

## 五、详细测试验证（含完整构造数据步骤）

1. 环境启动：先启动Redis、MySQL服务，确保服务正常运行（无报错）；再启动 Spring Boot 服务。

   >  Redis 可以使用 Docker 启动，比较方便。关于在 Windows 上安装 Docker，可以参考我这篇文章：
   >
   > [Windows 下 Docker 安装与使用全攻略](https://mp.weixin.qq.com/s/wKJrVvm6njPN9p0ge_CUtw)

2. 构造测试数据：

   -  构造历史正常数据，存入MySQL t_industrial_data表，用于统计阈值：

     ```sql
     INSERT INTO t_industrial_data (device_id, device_name, data_type, data_value, create_time)
     SELECT 
         'device001' AS device_id,
         '一号车间温度传感器' AS device_name,
         'temperature' AS data_type,
         -- 生成25-30℃之间的随机正常温度
         ROUND(25 + RAND() * 5, 1) AS data_value,
         -- 生成最近30天的随机时间
         DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30 * 24 * 60) MINUTE) AS create_time
     -- 用MySQL内置方式生成100条数据
     FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) t1,
          (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) t2;
     ```

   - 构造实时数据，存入Redis，用于预判：

     打开Redis客户端（redis-cli），执行以下命令（模拟3条不同状态的实时数据，对应同一设备）

     ```bash
     # 1. 正常数据（27.2℃，在25-30℃正常范围，波动范围内）
     hset industrial:data:realtime device001:temperature_1 "{\"deviceId\":\"device001\",\"dataType\":\"temperature\",\"dataValue\":\"27.2\",\"collectTime\":\"2026-02-02 15:30:00\"}"
     
     # 2. 预警数据（29.8℃，未超标，但超出10%波动范围）
     hset industrial:data:realtime device001:temperature_2 "{\"deviceId\":\"device001\",\"dataType\":\"temperature\",\"dataValue\":\"29.8\",\"collectTime\":\"2026-02-02 15:35:00\"}"
     
     # 3. 异常数据（30.5℃，已超出30℃的历史最大值，触发异常）
     hset industrial:data:realtime device001:temperature_3 "{\"deviceId\":\"device001\",\"dataType\":\"temperature\",\"dataValue\":\"30.5\",\"collectTime\":\"2026-02-02 15:40:00\"}"
     ```

     ![image-20260202220021867](E:\文章\Java x 工业智能\8-轻量化AI落地：Java + SpringBoot 实现设备异常预判.assets\image-20260202220021867.png)

3. 验证结果
   
   Spring Boot 服务运行结果：
   
   ```       
   JDBC Connection [HikariProxyConnection@674115918 wrapping com.mysql.cj.jdbc.ConnectionImpl@6747f790] will be managed by Spring
   ==>  Preparing: INSERT INTO t_data_prediction ( device_id, data_type, data_value, history_avg, history_min, history_max, prediction_status, prediction_desc, prediction_time ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ? )
   ==> Parameters: device001(String), temperature_1(String), 27.2(String), 27.5(Double), 25.0(Double), 30.0(Double), 0(Integer), 数据正常，无异常趋势(String), 2026-02-02T22:17:00.012430400(LocalDateTime)
   ==> Parameters: device001(String), temperature_2(String), 29.8(String), 27.5(Double), 25.0(Double), 30.0(Double), 0(Integer), 数据正常，无异常趋势(String), 2026-02-02T22:17:00.013545900(LocalDateTime)
   ==> Parameters: device001(String), temperature_3(String), 30.5(String), 27.5(Double), 25.0(Double), 30.0(Double), 2(Integer), 数据已超标，需立即检查（当前：30.5）(String), 2026-02-02T22:17:00.015177600(LocalDateTime)
   2026-02-02T22:17:00.032+08:00  INFO 15948 --- [   scheduling-1] i.g.i.i.p.service.PredictionService      : 预判结果持久化完成，共3条
   ```
   
   MySQL中也增加了对应的数据：
   
   ![image-20260202221835356](E:\文章\Java x 工业智能\8-轻量化AI落地：Java + SpringBoot 实现设备异常预判.assets\image-20260202221835356.png)


      

## 六、最佳实践（避坑指南）

- 避坑1：历史数据需确保为“正常数据”，若混入异常数据，会导致阈值统计不准，进而引发预判误判；

- 避坑2：波动阈值需根据设备实际运行情况调整（如精密设备可设为5%，普通设备设为10%-15%）

