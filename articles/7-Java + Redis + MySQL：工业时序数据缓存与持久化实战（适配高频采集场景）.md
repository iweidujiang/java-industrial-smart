# Java + Redis + MySQL：工业时序数据缓存与持久化实战（适配高频采集场景）

> 本文属于专栏《Java × 工业智能》第 7 篇 | GitHub 源码：[github.com/iweidujiang/java-industrial-smart](https://github.com/iweidujiang/java-industrial-smart)

大家好，我是苏渡苇～ 继续《Java x 工业智能》合集更新，今天带来第 **7** 篇实操干货！

前面已经搞定了 Modbus 通信、多PLC接入、SpringBoot 控制设备，不知道大家有没有遇到一个问题：工厂里的设备（比如PLC、传感器）都是高频采集数据的——可能每秒采集1次，甚至每秒几次，要是直接把这些数据往MySQL里写，不仅会拖慢数据库，还会导致Java程序采集卡顿，严重的甚至会丢数据。

这篇文章就解决这个核心痛点，用我们最熟悉、最常用的 **Java + Redis + MySQL** 组合，实现工业时序数据的“缓存+持久化”双重保障：Redis 扛住高频采集的压力，MySQL 负责数据长期存储，全程实操带代码，贴合真实工厂场景。



## 一、先搞懂：为什么需要 Redis + MySQL 组合？

在工业场景里，时序数据（就是设备按时间顺序采集的温度、压力、转速等数据）有两个核心特点：**高频产生、需要长期留存**。

单独用MySQL或者Redis，都有问题：

- 只用MySQL：MySQL是关系型数据库，写入速度相对慢，高频写入会造成“写阻塞”，Java程序要等数据库写入完成才能继续采集下一批数据，久而久之就会卡顿、丢数据；

- 只用Redis：Redis是内存数据库，写入速度极快，能完美扛住高频采集，但Redis默认以内存存储为主——即便它支持RDB、AOF两种持久化方式，也更适合临时缓存高频数据，难以满足工业场景中时序数据长期留存、可追溯（如故障追溯）的核心需求，毕竟工业数据一旦丢失，可能影响故障排查、生产复盘，这是绝对不能接受的。

所以最优解是：Redis 做“临时缓存”，先快速接住所有高频采集的数据，既发挥它写入速度快的优势，也依托其自身RDB、AOF持久化能力做临时兜底；再由Java程序异步把Redis中的数据批量写入MySQL，实现“高频采集不卡顿、数据长期留存可追溯”的双重保障——这也是工厂里处理时序数据最常用、最稳妥的方案之一。

## 二、核心流程：数据怎么流转？

整个流程特别简单，不用复杂架构：

1.  采集端：Java程序从PLC、传感器采集数据（比如温度25℃、压力1.2MPa），同时带上采集时间戳（时序数据的核心，必须有）；

2.  缓存写入：Java程序先把采集到的数据，快速写入Redis（用Redis的SortedSet类型，按时间戳排序，方便后续批量读取和去重）；

3.  异步持久化：Java程序开启一个异步任务（不用等任务完成，不影响后续采集），定期从Redis中读取批量数据，批量写入MySQL（减少MySQL写入次数，提升效率）；

4.  数据清理：MySQL写入完成后，删除Redis中已经持久化的数据，避免Redis内存溢出——这里删除缓存不影响数据安全，因为数据已同步到MySQL，且Redis自身的临时持久化仅用于缓存层兜底，无需长期保留缓存数据；

5.  兜底保障：万一MySQL写入失败，程序会把失败的数据重新放回Redis，等待下一次异步任务重试，避免数据丢失。

简单总结：

![image-20260202105916467](E:\文章\Java x 工业智能\7-Java + Redis + MySQL：工业时序数据缓存与持久化实战（适配高频采集场景）.assets\image-20260202105916467.png)

整个流程闭环，既保证速度，又保证数据安全。

## 三、前置准备（3分钟搞定）

技术都是大家熟悉的，提前准备好这3个东西，不用额外装复杂工具：

1. Java环境（JDK 17+，推荐JDK 17或JDK 21，与Spring Boot 3.5.x 完美兼容）；

2. Redis；

3. MySQL




## 四、代码实操

> **完整源码已开源**：
> 📁 模块路径：`code/07-data-cache-persistence`
>
> 🔗 仓库地址：[github.com/iweidujiang/java-industrial-smart](https://github.com/iweidujiang/java-industrial-smart)
>
> 欢迎 Star & 提 Issue！

代码仓库结构如下：

```plain text
├─articles
└─code
    ├─03-modbus-over-serial
    ├─04-modbus-mqtt
    ├─05-modbus-rest-control
    ├─06-plc-unified-adapter
    └─07-data-cache-persistence
        └─src
            └─main
                ├─java
                │  └─io
                │      └─github
                │          └─iweidujiang
                │              └─industry
                │                  └─datacache
                │                      ├─config （配置类：Redis、MySQL、定时任务配置）
                │                      ├─model （实体类：时序数据实体）
                │                      ├─repository （数据访问层：MySQL DAO接口）
                │                      ├─service （业务层：缓存、持久化核心逻辑）
                │                      │  └─impl （业务层实现类）
                │                      └─util （工具类：Redis操作工具、时间工具）
                └─resources （配置文件：application.yml，配置Redis、MySQL连接）
```

下面开始上代码！

<img src="https://i0.hdslb.com/bfs/article/a8afed42e3c3e80bb0a4a1790340ca6bfa99ddad.jpg@1142w_1242h.webp" alt="img" style="zoom:45%;" />

### 第一步：配置pom.xml（依赖引入）

pom.xml中引入核心依赖（Spring Boot、Redis、MySQL、MyBatis-Plus，都是常用依赖）：

```xml
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
```

### 第二步：配置application.yml（连接Redis、MySQL）

在 application.yml 文件，配置Redis和MySQL的连接信息：

```yaml
server:
  port: 8087

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/industrial_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 111111
  data:
    redis:
      host: localhost
      port: 6379

mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: io.github.iweidujiang.industry.datacache.model
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      table-prefix: t_
      id-type: auto
```

提醒：先在MySQL中创建数据库「industrial_db」（名字可以自定义，和yml中一致即可），表会由MyBatis-Plus自动创建，不用手动建表。

### 第三步：编写核心实体类（时序数据模型）

```java
@Data
@TableName("t_industrial_data")
public class IndustrialData {

    /** 主键（自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备ID（比如PLC的ID，区分不同设备的数据） */
    private String deviceId;

    /** 设备名称（比如「一号车间温度传感器」） */
    private String deviceName;

    /** 数据类型（比如temperature=温度、pressure=压力、speed=转速） */
    private String dataType;

    /** 数据值（比如25.5，存字符串兼容各种数据格式，也可以用Double） */
    private String dataValue;

    /** 采集时间戳（时序数据核心，必须有，精确到秒/毫秒） */
    private LocalDateTime collectTime;

    /** 数据状态（0=正常，1=异常，后续对接告警机制可用） */
    private Integer dataStatus;

    /** 创建时间 */
    private LocalDateTime createTime;

    // 自动填充创建时间（不用手动设置，后续用配置实现）
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = LocalDateTime.now();
    }
}
```

### 第四步：编写Redis操作工具类

封装Redis的常用操作（写入、批量读取、删除），后续业务层直接调用，不用重复写Redis代码：

```java
@Component
public class RedisUtil {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 写入Redis（SortedSet类型，按时间戳排序，key=设备ID+数据类型，比如「device1_temperature」）
     * @param key Redis的key（区分不同设备、不同类型的数据）
     * @param value 数据值（这里存JSON字符串，包含完整的时序数据信息）
     * @param score 时间戳（用于排序，方便后续批量读取）
     */
    public void zAdd(String key, String value, double score) {
        stringRedisTemplate.opsForZSet().add(key, value, score);
        // 设置过期时间（防止Redis内存溢出，比如设置7天过期，兜底）
        stringRedisTemplate.expire(key, 7, TimeUnit.DAYS);
    }

    /**
     * 批量读取Redis中的数据（按时间戳范围读取，比如读取最近10分钟的数据）
     * @param key Redis的key
     * @param minScore 最小时间戳（开始时间）
     * @param maxScore 最大时间戳（结束时间）
     * @return 批量数据列表
     */
    public List<String> zRangeByScore(String key, double minScore, double maxScore) {
        Set<String> set = stringRedisTemplate.opsForZSet().rangeByScore(key, minScore, maxScore);
        return CollUtil.isNotEmpty(set) ? CollUtil.newArrayList(set) : CollUtil.newArrayList();
    }

    /**
     * 批量删除Redis中的数据（删除已经持久化到MySQL的数据）
     * @param key Redis的key
     * @param minScore 最小时间戳
     * @param maxScore 最大时间戳
     */
    public void zRemoveByScore(String key, double minScore, double maxScore) {
        stringRedisTemplate.opsForZSet().removeRangeByScore(key, minScore, maxScore);
    }

    /**
     * 判断Redis中是否存在某个key（用于判断设备是否有缓存数据）
     */
    public boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    /**
     * 批量删除Redis中的key（用于清理过期设备的数据）
     */
    public void deleteKeys(Collection<String> keys) {
        stringRedisTemplate.delete(keys);
    }
}
```

### 第五步：编写数据访问层（MySQL DAO接口）

数据访问层接口 IndustrialDataMapper，继承MyBatis-Plus的BaseMapper，不用写SQL，就能实现批量插入、查询等操作（可以使用Myybatis Plus自动生成）：

```java
@Mapper
public interface IndustrialDataMapper extends BaseMapper<IndustrialData> {
    // 这里暂时先不用写任何方法，BaseMapper已经提供了目前需要的方法
}
```

### 第六步：编写核心业务层（缓存+持久化逻辑）

这是本篇文章的核心，分为接口和实现类，封装“数据写入Redis、异步批量写入MySQL”的逻辑。

#### 6.1 业务层接口（DataCacheService.java）

```java
public interface DataCacheService {
    /**
     * 采集数据写入Redis缓存（高频采集入口）
     * @param industrialData 时序数据实体
     */
    void cacheIndustrialData(IndustrialData industrialData);

    /**
     * 异步批量将Redis中的数据写入MySQL（持久化入口）
     */
    void batchPersistData();

    /**
     * 重试失败的持久化数据（兜底保障，避免数据丢失）
     */
    void retryFailedPersistData();
}
```

#### 6.2 业务层实现类（DataCacheServiceImpl.java）

```java
@Slf4j
@Service
public class DataCacheServiceImpl extends ServiceImpl<IndustrialDataMapper, IndustrialData> implements DataCacheService {
    @Resource
    private RedisUtil redisUtil;

    @Resource
    private IndustrialDataMapper industrialDataMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // Redis的key前缀（统一规范，方便后续清理，比如「industrial:data:device1_temperature」）
    private static final String REDIS_KEY_PREFIX = "industrial:data:";
    // 失败重试的Redis key（存放写入MySQL失败的数据）
    private static final String REDIS_FAILED_KEY = "industrial:data:failed";

    /**
     * 集数据写入Redis缓存（高频采集入口，同步执行，速度极快）
     */
    @Override
    public void cacheIndustrialData(IndustrialData industrialData) {
        try {
            // 1. 构建Redis的key：前缀 + 设备ID + 数据类型（区分不同设备、不同数据）
            String redisKey = REDIS_KEY_PREFIX + industrialData.getDeviceId() + "_" + industrialData.getDataType();

            // 2. 设置采集时间、创建时间（不用手动传，这里统一设置）
            LocalDateTime collectTime = LocalDateTime.now();
            industrialData.setCollectTime(collectTime);
            industrialData.setCreateTime(collectTime);
            industrialData.setDataStatus(0); // 默认数据正常

            // 3. 将实体类转为JSON字符串（Redis中存JSON，方便后续读取解析）
            String dataJson = JSONUtil.toJsonStr(industrialData);

            // 4. 写入Redis（SortedSet类型，score=时间戳（秒），按时间排序）
            double score = collectTime.toEpochSecond(ZoneOffset.of("+8"));
            redisUtil.zAdd(redisKey, dataJson, score);

            log.info("数据写入Redis成功：key={}, 数据={}", redisKey, dataJson);
        } catch (Exception e) {
            log.error("数据写入Redis失败，数据：{}，异常信息：{}", JSONUtil.toJsonStr(industrialData), e.getMessage());
            // 极端情况：Redis写入失败，直接暂存到失败队列，后续重试
            stringRedisTemplate.opsForList().leftPush(REDIS_FAILED_KEY, JSONUtil.toJsonStr(industrialData));
        }
    }

    /**
     * 异步批量将Redis中的数据写入MySQL（异步执行，不影响高频采集）
     * Async：Spring异步注解，开启独立线程执行，不用等执行完成
     */
    @Override
    @Async
    @Transactional // 事务注解，保证批量写入要么全成功，要么全失败，避免数据错乱
    public void batchPersistData() {
        try {
            log.info("开始执行异步持久化：从Redis批量读取数据，写入MySQL");

            // 1. 获取Redis中所有时序数据的key（所有设备、所有数据类型）
            Set<String> redisKeys = stringRedisTemplate.keys(REDIS_KEY_PREFIX + "*");
            if (redisKeys.isEmpty()) {
                log.info("Redis中无待持久化数据，结束本次持久化");
                return;
            }

            // 2. 遍历每个key，批量读取数据、写入MySQL
            for (String redisKey : redisKeys) {
                // 2.1 读取Redis中最近30分钟的数据（可自定义时间，比如10分钟、1小时）
                // 最小时间戳：当前时间 - 30分钟（秒）
                double minScore = LocalDateTime.now().minusMinutes(30).toEpochSecond(ZoneOffset.of("+8"));
                // 最大时间戳：当前时间（秒）
                double maxScore = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8"));
                List<String> dataJsonList = redisUtil.zRangeByScore(redisKey, minScore, maxScore);

                if (dataJsonList.isEmpty()) {
                    continue;
                }

                // 2.2 将JSON字符串转为实体类列表（批量插入MySQL）
                List<IndustrialData> dataList = new ArrayList<>();
                for (String dataJson : dataJsonList) {
                    IndustrialData data = JSONUtil.toBean(dataJson, IndustrialData.class);
                    dataList.add(data);
                }

                // 2.3 批量写入MySQL（MyBatis-Plus的批量插入方法，高效）
                this.saveBatch(dataList);
                log.info("批量写入MySQL成功：key={}，数据条数={}", redisKey, dataList.size());

                // 2.4 写入成功后，删除Redis中已持久化的数据（避免重复写入，节省内存）
                redisUtil.zRemoveByScore(redisKey, minScore, maxScore);
                log.info("删除Redis中已持久化数据：key={}，数据条数={}", redisKey, dataJsonList.size());
            }

        } catch (Exception e) {
            log.error("异步持久化失败，异常信息：{}", e.getMessage());
            // 这里可以做更细致的失败处理，比如将失败的key记录下来，后续重试
        }
    }

    /**
     * 重试失败的持久化数据（兜底保障，避免数据丢失）
     * 可配合定时任务执行，比如每分钟重试一次
     */
    @Override
    @Async
    @Transactional
    public void retryFailedPersistData() {
        try {
            log.info("开始重试失败的持久化数据");

            // 1. 读取Redis中失败的数据（列表类型，leftPop批量读取）
            List<String> failedJsonList = stringRedisTemplate.opsForList().range(REDIS_FAILED_KEY, 0, -1);
            if (failedJsonList == null || failedJsonList.isEmpty()) {
                log.info("无失败数据需要重试");
                return;
            }

            // 2. 批量写入MySQL
            List<IndustrialData> failedDataList = new ArrayList<>();
            for (String failedJson : failedJsonList) {
                IndustrialData data = JSONUtil.toBean(failedJson, IndustrialData.class);
                failedDataList.add(data);
            }
            this.saveBatch(failedDataList);
            log.info("重试失败数据写入MySQL成功，条数：{}", failedDataList.size());

            // 3. 重试成功后，删除Redis中的失败数据
            stringRedisTemplate.opsForList().trim(REDIS_FAILED_KEY, failedJsonList.size(), -1);

        } catch (Exception e) {
            log.error("重试失败数据持久化仍失败，异常信息：{}", e.getMessage());
            // 极端情况：多次重试失败，可发送告警通知（后续文章会讲告警机制）
        }
    }
}

```

### 第七步：配置类（Redis、定时任务、异步任务）

新建3个配置类，分别配置Redis序列化、定时任务、异步任务，确保程序正常运行：

#### 7.1 Redis配置（RedisConfig.java）

```java
@Configuration
public class RedisConfig {
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate(redisConnectionFactory);
        // 配置key和value的序列化方式（String序列化，避免中文乱码）
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        stringRedisTemplate.setKeySerializer(stringRedisSerializer);
        stringRedisTemplate.setValueSerializer(stringRedisSerializer);
        stringRedisTemplate.setHashKeySerializer(stringRedisSerializer);
        stringRedisTemplate.setHashValueSerializer(stringRedisSerializer);
        stringRedisTemplate.afterPropertiesSet();
        return stringRedisTemplate;
    }
}
```

#### 7.2 定时任务执行类（ScheduledTask.java）

```java
@Component
public class ScheduledTask {

    @Resource
    private DataCacheService dataCacheService;

    /**
     * 定时执行异步持久化（每30分钟执行一次，可自定义 cron 表达式）
     * cron表达式说明：0 0/30 * * * ? 表示每30分钟执行一次
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    public void scheduledBatchPersist() {
        dataCacheService.batchPersistData();
    }

    /**
     * 定时执行失败重试（每1分钟执行一次，确保失败数据及时重试）
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void scheduledRetryFailedData() {
        dataCacheService.retryFailedPersistData();
    }
}
```

#### 7.3 异步任务配置（AsyncConfig.java）

配置异步任务的线程池，避免异步任务占用主线程，确保高频采集不卡顿：

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数（根据服务器配置调整，比如8）
        executor.setCorePoolSize(8);
        // 最大线程数
        executor.setMaxPoolSize(16);
        // 队列容量（缓存异步任务）
        executor.setQueueCapacity(100);
        // 线程空闲时间（超过这个时间，空闲线程会被销毁）
        executor.setKeepAliveSeconds(60);
        // 线程名称前缀（方便日志调试）
        executor.setThreadNamePrefix("industrial-async-");
        // 初始化线程池
        executor.initialize();
        return executor;
    }
}
```

### 第八步：启动类（启动项目）

```java
@SpringBootApplication
@EnableScheduling
public class DataCachePersistenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataCachePersistenceApplication.class, args);
        System.out.println("Java + Redis + MySQL 时序数据缓存与持久化项目启动成功！");
    }
}
```

## 五、测试验证

新建测试类：

```java
@SpringBootTest
public class DataCacheTest {
    @Resource
    private DataCacheService dataCacheService;

    // 模拟高频采集：循环100次，每秒采集1次，模拟设备高频产生数据
    @Test
    public void testCacheIndustrialData() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            IndustrialData data = new IndustrialData();
            data.setDeviceId("device001"); // 设备ID，自定义
            data.setDeviceName("一号车间温度传感器");
            data.setDataType("temperature"); // 数据类型：温度
            data.setDataValue(String.valueOf(25.0 + i % 5)); // 模拟温度数据（25-30℃）

            // 调用方法，写入Redis
            dataCacheService.cacheIndustrialData(data);

            // 暂停1秒，模拟每秒采集1次
            Thread.sleep(1000);
        }
    }
}
```

运行效果：

```
2026-02-02T12:04:13.511+08:00  INFO 40704 --- [           main] i.g.i.i.d.s.impl.DataCacheServiceImpl    : 数据写入Redis成功：key=industrial:data:device001_temperature, 数据={"deviceId":"device001","deviceName":"一号车间温度传感器","dataType":"temperature","dataValue":"25.0","collectTime":1770005053188,"dataStatus":0,"createTime":1770005053188}
2026-02-02T12:04:14.516+08:00  INFO 40704 --- [           main] i.g.i.i.d.s.impl.DataCacheServiceImpl    : 数据写入Redis成功：key=industrial:data:device001_temperature, 数据={"deviceId":"device001","deviceName":"一号车间温度传感器","dataType":"temperature","dataValue":"26.0","collectTime":1770005054512,"dataStatus":0,"createTime":1770005054512}
2026-02-02T12:04:15.521+08:00  INFO 40704 --- [           main] i.g.i.i.d.s.impl.DataCacheServiceImpl    : 数据写入Redis成功：key=industrial:data:device001_temperature, 数据={"deviceId":"device001","deviceName":"一号车间温度传感器","dataType":"temperature","dataValue":"27.0","collectTime":1770005055517,"dataStatus":0,"createTime":1770005055517}
...
```



## 六、最佳实践（避坑指南）

结合真实工厂场景，给大家提3个关键注意事项，避免后续落地时踩坑：

- 1.  Redis的key设计：必须区分设备ID和数据类型（比如「industrial:data:device001_temperature」），否则不同设备、不同类型的数据会混在一起，后续无法批量处理；

- 2.  定时任务时间：持久化的时间间隔（比如30分钟），要根据设备采集频率调整——采集频率高，间隔可以短一点（比如10分钟），避免Redis中缓存的数据过多，占用内存；

- 3.  异常处理：代码中已经做了Redis写入失败、MySQL写入失败的兜底处理——即便Redis自身有持久化能力，也需额外做好异常重试，避免极端情况下（如Redis持久化失败、服务器宕机）的数据丢失，后续可以对接告警机制（下一篇文章会讲），一旦出现失败，及时通知开发者处理；

- 4.  数据去重：Redis的SortedSet类型本身不会去重，如果设备采集到重复数据（比如同一时间戳的同一数据），可以在写入Redis前，先判断该时间戳的数据是否已存在，避免重复缓存、重复持久化。

## 七、小结

这篇文章搞定了工业高频时序数据的“缓存+持久化”核心问题，全程用的都是Java、Redis、MySQL、SpringBoot这些大家熟悉的技术栈。

核心收获：明确Redis不仅能扛高频采集，自身也具备临时持久化能力，结合MySQL实现“缓存+长期持久化”的组合方案，理解工业时序数据的流转流程，解决工厂数据采集卡顿、丢数据、无法长期追溯的痛点，同时学会异步任务、定时任务的实操用法。

---

最后，按惯例提醒：

**完整源码已开源**：
📁 模块路径：`code/07-data-cache-persistence`

🔗 仓库地址：[github.com/iweidujiang/java-industrial-smart](https://github.com/iweidujiang/java-industrial-smart)

欢迎 Star & 提 Issue！