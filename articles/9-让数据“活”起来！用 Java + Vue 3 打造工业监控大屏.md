# 让数据“活”起来！用 Java + Vue 3 打造工业监控大屏

> 🧭 本文属于专栏《Java × 工业智能》第 9 篇 | GitHub 源码：[github.com/iweidujiang/java-industrial-smart](https://github.com/iweidujiang/java-industrial-smart)

------

## 一、为什么需要可视化？——从“有数据”到“看得懂”

在前面几篇文章中，我们已经打通了工业数据的“任督二脉”：

- 通过统一接入层采集西门子、三菱、Modbus 设备数据；
- 利用 Redis 高效缓存最新值；
- 甚至实现了基于滑动窗口的异常趋势预判。

但这些成果对用户而言仍是“黑盒”——工程师看到的是日志和 JSON，而车间主任关心的是：“锅炉温度正常吗？空压机有没有报警？”

**可视化，就是把机器语言翻译成人话的过程**。

本篇目标很明确：**用最短路径，构建一个能跑、能看、能告警的工业监控大屏**，为后续系统集成打下基础。



## 二、技术选型：轮询 vs WebSocket？看场景说话

实现“实时更新”，常见有两种思路：

| 方案          | 延迟       | 资源消耗     | 开发复杂度 | 适用场景                       |
| ------------- | ---------- | ------------ | ---------- | ------------------------------ |
| **HTTP 轮询** | 500ms ~ 2s | 低           | ⭐☆☆        | 中小规模、内网环境、演示验证   |
| **WebSocket** | <100ms     | 高（长连接） | ⭐⭐⭐        | 高频控制、远程操作、大规模并发 |

> 🔍 **本篇采用 HTTP 轮询**：
>
> - **快速验证**：无需处理连接保活、断线重连、集群广播等复杂问题；
> - **调试友好**：浏览器 DevTools 可直接查看每次请求/响应；
> - **资源节省**：在边缘设备（如 2GB 内存工控机）上更稳定；

> 💡 **生产建议**：
> 若未来需支持 **50+ 设备并发** 或 **<500ms 响应要求**，再平滑升级为 WebSocket；
> 否则，**简单即可靠**。



## 三、整体结构

![image-20260203204320284](E:\文章\Java x 工业智能\9-让数据“活”起来！用 Java + Vue 3 打造工业监控大屏.assets\image-20260203204320284.png)

> ✅ **关键设计原则**：
>
> 1. **无状态前端**：所有数据来自 API，刷新即最新；
> 2. **模拟驱动**：无真实设备时，后端自动生成模拟数据



## 四、后端实现：提供“新鲜”数据

### 1. 数据接口设计

先设计 Redis 缓存结构：  

- Key: `device:{deviceId}:latest`  
- Value: JSON 字符串，如 `{"温度":62.3,"压力":0.82}`

新增一个 REST 接口，供前端拉取：

```java
@GetMapping("/latest/{deviceId}")
public ResponseEntity<Map<String, Object>> getLatestData(@PathVariable String deviceId) {
    String key = "device:" + deviceId + ":latest";
    String json = redisTemplate.opsForValue().get(key);

    if (json == null) {
        log.warn("⚠️ 设备 {} 无缓存数据", deviceId);
        return ResponseEntity.notFound().build();
    }

    try {
        Map<String, Object> data = objectMapper.readValue(json, Map.class);
        log.debug("📡 返回设备 {} 最新数据: {}", deviceId, data);
        return ResponseEntity.ok(data);
    } catch (JsonProcessingException e) {
        log.error("解析设备 {} 数据失败", deviceId, e);
        return ResponseEntity.status(500).build();
    }
}
```

> ✅ 返回标准 HTTP 状态码：404 表示无数据，500 表示解析错误



### 2. 模拟数据生成器（无硬件也能跑）

为方便演示，我们内置一个模拟设备 `mock-boiler`：

```java
@Scheduled(fixedRate = 1000)
public void generateMockData() {
    double temperature = 60 + Math.sin(System.currentTimeMillis() / 2000.0) * 5;
    double pressure = 0.8 + Math.random() * 0.05;

    Map<String, Double> values = Map.of("温度", temperature, "压力", pressure);

    try {
        String json = objectMapper.writeValueAsString(values);
        redisTemplate.opsForValue().set(
                "device:mock-boiler:latest",
                json,
                Duration.ofSeconds(10)
        );

        if (temperature > 68) {
            alertService.triggerAlert("mock-boiler", "温度过高", String.format("%.1f℃", temperature));
        }

        log.debug("💾 写入模拟数据: {}", values);
    } catch (JsonProcessingException e) {
        log.error("生成模拟数据失败", e);
    }
}
```

> **技巧**：用 `sin()` 生成平滑波动曲线，比纯随机更像真实设备



### 3. 告警服务

为简化，告警暂存于内存（生产环境应写入数据库）：

```java
@Service
public class AlertService {
    private final Queue<Alert> recentAlerts = new ConcurrentLinkedQueue<>();
    
    public void triggerAlert(String deviceId, String message, String value) {
        Alert alert = new Alert(deviceId, message, value, Instant.now());
        recentAlerts.offer(alert);
        // 保留最近5条
        while (recentAlerts.size() > 5) recentAlerts.poll();
    }
    
    public List<Alert> getRecentAlerts() {
        return new ArrayList<>(recentAlerts);
    }
}
```

前端可通过 `/api/alerts` 获取最新告警列表。



## 五、前端实现：专注两个核心功能

前端使用 **Vue 3 + TypeScript + Pinia + ECharts**，仅包含两个页面组件：

### 1. 实时曲线（ECharts 动态更新）

- X 轴：时间（HH:mm:ss）
- Y 轴左：温度（℃）
- Y 轴右：压力（MPa）
- 每秒追加新点，最多保留 60 个点（1 分钟）

关键逻辑：

```ts
// 每秒拉取数据
const timer = setInterval(async () => {
  const res = await fetch('/api/data/latest/mock-boiler')
  if (res.ok) {
    const data = await res.json()
    updateChart(data.温度, data.压力)
  }
}, 1000)

// 更新图表
function updateChart(temp: number, press: number) {
  const now = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  temperatureSeries.data.push([now, temp])
  pressureSeries.data.push([now, press])
  
  if (temperatureSeries.data.length > 60) {
    temperatureSeries.data.shift()
    pressureSerie.data.shift()
  }
  chart.setOption({ series: [temperatureSeries, pressureSeries] })
}
```

> ✅ **性能优化**：ECharts 的 `setOption` 支持增量更新，避免整图重绘



### 2. 告警弹窗（自动弹出 + 自动消失）

- 当收到新告警时，屏幕右上角弹出红色通知框；
- 5 秒后自动消失，或点击“×”手动关闭；
- 同时播放轻微提示音（可配置开关）。

实现要点：

```vue
<!-- AlertToast.vue -->
<script setup>
const props = defineProps<{ message: string; value: string }>()
const emit = defineEmits(['close'])

onMounted(() => {
  const timer = setTimeout(() => emit('close'), 5000)
  onUnmounted(() => clearTimeout(timer))
  
  // 播放提示音（静音模式可跳过）
  const audio = new Audio('/alert.mp3')
  audio.play().catch(e => console.warn('音频播放被阻止', e))
})
</script>
```

> 🔕 **用户体验细节**：  
>
> - 音频使用 `.mp3` 格式，兼容性好；  
> - 捕获 `play()` 异常，避免浏览器静音策略报错。



## 六、效果预览

![监控大屏数据模拟](E:\文章\Java x 工业智能\9-让数据“活”起来！用 Java + Vue 3 打造工业监控大屏.assets\screenshots.gif)

![告警弹窗](E:\文章\Java x 工业智能\9-让数据“活”起来！用 Java + Vue 3 打造工业监控大屏.assets\image-20260203212946369.png)

## 七、如何运行？三步启动

### 1. 启动后端

```bash
# 确保本地运行 Redis（默认 localhost:6379）
cd code/09-industrial-monitor/backend
mvn spring-boot:run
```

### 2. 启动前端

```bash
cd code/09-industrial-monitor/frontend
npm install
npm run dev
```

### 3. 访问

打开浏览器：`http://localhost:5173`
即可看到动态曲线，约 10 秒后触发首次告警（温度 > 68℃，参数可调）。



## 八、小结

本篇通过极简设计，实现了工业监控的核心诉求：

- **实时可见**：数据每秒更新，趋势一目了然；
- **异常可知**：告警主动触达，避免漏看

------

