

# Modbus 地址总搞混？别再死记 40001=0！设计 Java 工具类彻底理清



上一篇文章整了用 Java 读写 Modbus 寄存器，但你是否还在为这些地址头疼：

- 设备 A 的温度在 **40001**，设备 B 却在 **400001**？
- 按 `40001 - 40001 = 0` 计算，结果设备返回“非法地址”？
- 同样是保持寄存器，有的设备要减 40001，有的却要减 40000？

**真相是：Modbus 地址没有“唯一标准”，一切以设备手册为准！**

今天，我们就揭开 Modbus 地址的“多面性”，并提供一个**可配置、可扩展、防错的 Java 工具类**，让你轻松应对各种设备。



## 一、Modbus 地址：两种表示，多种变体

Modbus 协议本身只定义了 **从 0 开始的协议地址（Protocol Address）**。
但为了便于攻城狮识别数据类型，行业形成了**用户地址（User Address）** 的习惯表示法。

### 用户地址的两种常见格式

| 数据类型                       | 5位格式（常见于老设备/PLC） | 6位格式（常见于新设备/电表） |
| ------------------------------ | --------------------------- | ---------------------------- |
| 线圈（Coil）                   | `00001 – 09999`             | `000001 – 065536`            |
| 离散输入（Discrete Input）     | `10001 – 19999`             | `100001 – 165536`            |
| 输入寄存器（Input Register）   | `30001 – 39999`             | `300001 – 365536`            |
| 保持寄存器（Holding Register） | `40001 – 49999`             | `400001 – 465536`            |

> **关键点**：  
>
> - 两种格式都广泛存在；  
> - **转换公式不同**！

### 转换公式（必须查手册！）

| 数据类型        | 5位格式转换                   | 6位格式转换                    |
| --------------- | ----------------------------- | ------------------------------ |
| 线圈 / 离散输入 | `协议地址 = 用户地址 - 1`     | `协议地址 = 用户地址 - 1`      |
| 输入寄存器      | `协议地址 = 用户地址 - 30001` | `协议地址 = 用户地址 - 300001` |
| 保持寄存器      | `协议地址 = 用户地址 - 40001` | `协议地址 = 用户地址 - 400001` |

### 地址映射示例

| 数据类型   | 用户地址示例 | 协议地址（十六进制） | 协议地址（十进制） |
| :--------- | :----------- | :------------------- | :----------------- |
| 线圈       | 000001       | 0x0000               | 0                  |
| 线圈       | 000002       | 0x0001               | 1                  |
| 离散输入   | 100001       | 0x0000               | 0                  |
| 离散输入   | 100002       | 0x0001               | 1                  |
| 输入寄存器 | 300001       | 0x0000               | 0                  |
| 输入寄存器 | 300002       | 0x0001               | 1                  |
| 保持寄存器 | 400001       | 0x0000               | 0                  |
| 保持寄存器 | 400002       | 0x0001               | 1                  |

> ⚠️ **重要提醒**：
> 某些厂商还会引入**自定义偏移**（如 +1、+100），例如：
>
> - 西门子 PLC：保持寄存器 `400001` 对应协议地址 `0`（即减 400001）
> - 某国产电表：保持寄存器 `40001` 对应协议地址 `1`（即减 40000）

**结论：永远以设备通信手册为准！**



## 二、Modbus 功能码



### 常用功能码（必知）

| 功能码        | 名称         | 作用                             | 地址类型 |
| :------------ | :----------- | :------------------------------- | :------- |
| **01**        | 读线圈       | 读取一个或多个线圈状态（ON/OFF） | 0xxxx    |
| **02**        | 读离散输入   | 读取一个或多个离散输入状态       | 1xxxx    |
| **03**        | 读保持寄存器 | 读取一个或多个保持寄存器值       | 4xxxx    |
| **04**        | 读输入寄存器 | 读取一个或多个输入寄存器值       | 3xxxx    |
| **05**        | 写单个线圈   | 写入单个线圈状态（ON/OFF）       | 0xxxx    |
| **06**        | 写单个寄存器 | 写入单个保持寄存器值             | 4xxxx    |
| **15** (0x0F) | 写多个线圈   | 写入多个线圈状态                 | 0xxxx    |
| **16** (0x10) | 写多个寄存器 | 写入多个保持寄存器值             | 4xxxx    |

### 扩展功能码（常见于高级设备）

| 功能码 | 名称            | 作用                     |
| :----- | :-------------- | :----------------------- |
| 07     | 读异常状态      | 读取设备的异常状态       |
| 08     | 诊断            | 串行链路诊断             |
| 11     | 获取事件计数器  | 读取事件计数器和状态     |
| 12     | 获取事件记录    | 读取事件日志             |
| 17     | 报告从站ID      | 获取从站设备描述信息     |
| 22     | 屏蔽写寄存器    | 写寄存器时使用AND/OR掩码 |
| 23     | 读/写多个寄存器 | 同时读写多个寄存器       |





## 三、解决方案：可配置的 `ModbusAddressResolver`

我们来设计一个**支持多种地址格式、可自定义偏移**的解析器。

### Step 1：定义地址格式枚举

```java
/**
 * 用户地址格式类型
 */
public enum AddressFormat {
    /**
     * 5位格式：40001, 30001 等
     */
    FIVE_DIGIT,
    
    /**
     * 6位格式：400001, 300001 等
     */
    SIX_DIGIT,
    
    /**
     * 自定义偏移（由用户指定起始值）
     */
    CUSTOM
}
```

### Step 2：数据类型枚举

```java
public enum ModbusDataType {
    // 线圈
    COIL,

    // 离散输入
    DISCRETE_INPUT,

    // 输入寄存器
    INPUT_REGISTER,

    // 保持寄存器
    HOLDING_REGISTER
}
```



### Step 3：地址解析器

```java
package io.github.iweidujiang.modbusexample.resolver;

import io.github.iweidujiang.modbusexample.enums.AddressFormat;
import io.github.iweidujiang.modbusexample.enums.ModbusDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * ┌───────────────────────────────────────────────
 * │ 📦 Modbus 地址解析器，支持 5位、6位、自定义偏移格式
 * │
 * │ 👤 作者：苏渡苇
 * │ 🔗 公众号：苏渡苇
 * │ 💻 GitHub：https://github.com/iweidujiang
 * │ 📅 @date 2026/1/20
 * └───────────────────────────────────────────────
 */
public class ModbusAddressResolver {
    // 默认偏移配置（按标准）
    private static final Map<ModbusDataType, Integer> DEFAULT_OFFSETS_5D = new HashMap<>();
    private static final Map<ModbusDataType, Integer> DEFAULT_OFFSETS_6D = new HashMap<>();

    static {
        // 5位格式偏移
        DEFAULT_OFFSETS_5D.put(ModbusDataType.COIL, 1);
        DEFAULT_OFFSETS_5D.put(ModbusDataType.DISCRETE_INPUT, 10001);
        DEFAULT_OFFSETS_5D.put(ModbusDataType.INPUT_REGISTER, 30001);
        DEFAULT_OFFSETS_5D.put(ModbusDataType.HOLDING_REGISTER, 40001);

        // 6位格式偏移
        DEFAULT_OFFSETS_6D.put(ModbusDataType.COIL, 1);
        DEFAULT_OFFSETS_6D.put(ModbusDataType.DISCRETE_INPUT, 100001);
        DEFAULT_OFFSETS_6D.put(ModbusDataType.INPUT_REGISTER, 300001);
        DEFAULT_OFFSETS_6D.put(ModbusDataType.HOLDING_REGISTER, 400001);
    }

    private final AddressFormat format;
    private final Map<ModbusDataType, Integer> customOffsets;

    /**
     * 构造标准格式解析器
     */
    public ModbusAddressResolver(AddressFormat format) {
        this.format = format;
        this.customOffsets = null;
    }

    /**
     * 构造自定义偏移解析器
     */
    public ModbusAddressResolver(Map<ModbusDataType, Integer> customOffsets) {
        if (customOffsets == null || customOffsets.isEmpty()) {
            throw new IllegalArgumentException("自定义偏移表不能为空");
        }
        this.format = AddressFormat.CUSTOM;
        this.customOffsets = new HashMap<>(customOffsets); // 防御性拷贝
    }

    /**
     * 将用户地址转换为协议地址
     */
    public int toProtocolAddress(int userAddress, ModbusDataType dataType) {
        if (dataType == null) {
            throw new IllegalArgumentException("数据类型不能为 null");
        }

        int offset;
        if (format == AddressFormat.CUSTOM) {
            // customOffsets 不应为 null（构造时保证）
            Integer customOffset = customOffsets.get(dataType);
            if (customOffset == null) {
                throw new IllegalArgumentException("自定义偏移未配置数据类型: " + dataType);
            }
            offset = customOffset;
        } else {
            // 使用标准偏移表
            Map<ModbusDataType, Integer> offsets =
                    (format == AddressFormat.FIVE_DIGIT) ? DEFAULT_OFFSETS_5D : DEFAULT_OFFSETS_6D;

            Integer standardOffset = offsets.get(dataType);
            if (standardOffset == null) {
                throw new IllegalArgumentException("不支持的数据类型: " + dataType);
            }
            offset = standardOffset;
        }

        // 可选：校验结果非负（防止用户地址小于偏移量）
        if (userAddress < offset) {
            throw new IllegalArgumentException(
                    String.format("用户地址 %d 小于偏移量 %d，计算结果为负", userAddress, offset)
            );
        }

        return userAddress - offset;
    }

    /**
     * 获取读操作对应的功能码
     */
    public int getReadFunctionCode(ModbusDataType dataType) {
        switch (dataType) {
            case COIL: return 1;
            case DISCRETE_INPUT: return 2;
            case INPUT_REGISTER: return 4;
            case HOLDING_REGISTER: return 3;
            default: throw new IllegalArgumentException("未知数据类型: " + dataType);
        }
    }
}

```





## 四、使用示例：适配不同设备

### 场景1：标准 5 位地址设备（温控器）

```java
ModbusAddressResolver resolver = new ModbusAddressResolver(AddressFormat.FIVE_DIGIT);

int protocolAddr = resolver.toProtocolAddress(40001, ModbusDataType.HOLDING_REGISTER); // → 0
int fc = resolver.getReadFunctionCode(ModbusDataType.HOLDING_REGISTER); // → 3

ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(protocolAddr, 1);
```

### 场景2：6 位地址设备（智能电表）

```java
ModbusAddressResolver resolver = new ModbusAddressResolver(AddressFormat.SIX_DIGIT);
int addr = resolver.toProtocolAddress(400001, ModbusDataType.HOLDING_REGISTER); // → 0
```

### 场景3：自定义偏移设备（某国产 PLC：40001 → 协议地址 1）

```java
Map<ModbusDataType, Integer> offsets = new HashMap<>();
offsets.put(ModbusDataType.HOLDING_REGISTER, 40000); // 40001 - 40000 = 1

ModbusAddressResolver resolver = new ModbusAddressResolver(offsets);
int addr = resolver.toProtocolAddress(40001, ModbusDataType.HOLDING_REGISTER); // → 1
```



## 五、最佳实践建议

1. **永远先读设备手册**：确认地址格式、偏移、支持的功能码；
2. **不要硬编码地址**：用常量类管理点表；
3. **封装解析逻辑**：避免散落在各处的 `addr - 40001`；
4. **日志记录转换过程**：便于调试“地址非法”问题；
5. **对异常地址做校验**：防止负数或超范围地址。



## 六、小结

- Modbus 用户地址有 **5位和6位**两种主流格式，根据设备手册进行实际开发；
- **协议地址 = 用户地址 - 偏移量**，偏移量因设备而异，还是读设备手册！；
- 必须熟悉常见功能码，**01~06 是基础**；
- 使用咱的 **可配置解析器**，一套代码适配所有设备；
- **设备手册是唯一真理**，切勿假设！



## 结语

现在，你不再需要死记“40001=0”。
面对任何 Modbus 设备，你都能快速解析其地址规则，并写出健壮的 Java 代码。

---

欢迎收藏、转发，让更多 Java 开发者精准对接工业设备！