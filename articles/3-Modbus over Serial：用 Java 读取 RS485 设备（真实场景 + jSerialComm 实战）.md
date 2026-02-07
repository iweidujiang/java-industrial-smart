# Modbus over Serial：用 Java 读取 RS485 设备（真实场景 + jSerialComm 实战）

**本文关键词**：#Modbus #RTU #RS485 #Java #jSerialComm #ModbusPoll #ModbusSlave #工业串口

在工厂车间、配电房、暖通机房里，**90% 的老旧设备只支持串口通信**——没有网口，只有 DB9 或端子排，协议是 **Modbus RTU**。
我们作为 Java 开发者，如何与这些“沉默的设备”对话？

今天，我们就用 **真实调试流程** 演示：

1. 用 **Modbus Slave** 模拟一台温控器（通过虚拟串口）；
2. 用 **Modbus Poll** 验证通信是否正常；
3. 最后，用 **Java + jSerialComm + j2mod** 编写自己的主站程序，读取温度数据。



## 一、为什么必须学 Modbus RTU？

| 对比项     | Modbus TCP         | Modbus RTU                 |
| ---------- | ------------------ | -------------------------- |
| 物理接口   | 网线（RJ45）       | 串口（RS232/RS485）        |
| 传输介质   | 以太网             | 双绞线（最长 1200 米）     |
| 多设备支持 | 需交换机           | 一条总线挂 32 台设备       |
| 成本       | 较高               | 极低（芯片几块钱）         |
| 典型设备   | 新型 PLC、智能电表 | 老式温控器、变频器、传感器 |

> **现实**：大量存量设备只有 RS485 接口。想集成？必须走串口。



## 二、真实场景设定

> **任务**：读取一台 Modbus RTU 温控器的当前温度（寄存器 40001）。

### 设备参数（来自设备手册）：

- 通信方式：**Modbus RTU**
- 波特率：**9600**
- 数据位：**8**
- 停止位：**1**
- 校验位：**无（None）**
- 从站地址：**1**
- 温度地址：**40001**（保持寄存器，值 = 实际温度 × 10）

### 所需工具：

- **Modbus Slave**（模拟温控器，作为从站）

  ![Modbus Slave](E:\文章\Java x 工业智能\Modbus over Serial：用 Java 读取 RS485 设备（真实场景 + jSerialComm 实战）.assets\image-20260121224049581.png)

- **Modbus Poll**（验证通信，作为主站）

  ![Modbus Poll](E:\文章\Java x 工业智能\Modbus over Serial：用 Java 读取 RS485 设备（真实场景 + jSerialComm 实战）.assets\image-20260121224144803.png)

- **Virtual Serial Port Driver (VSPD)**（创建虚拟串口对，如 COM3 ↔ COM4）

  ![VSPD](E:\文章\Java x 工业智能\Modbus over Serial：用 Java 读取 RS485 设备（真实场景 + jSerialComm 实战）.assets\image-20260121224217457.png)

- **Java 程序**（最终替代 Modbus Poll）

> 如果你有真实 USB-RS485 模块（如 FT232、CH340），可跳过虚拟串口，直连硬件。



## 三、Step 1：用 Modbus Slave 模拟从站（通过串口）

在 VSPD 中创建一对虚拟串口，例如：**COM3 ↔ COM4**

![image-20260121224338525](E:\文章\Java x 工业智能\Modbus over Serial：用 Java 读取 RS485 设备（真实场景 + jSerialComm 实战）.assets\image-20260121224338525.png)

启动 **Modbus Slave**：

- Connection → Connect
- Serial Port: **COM3**
- Baud Rate: **9600**
- Data Bits: **8**
- Parity: **None**
- Stop Bits: **1**
- Slave ID: **1**

![启动slave](E:\文章\Java x 工业智能\Modbus over Serial：用 Java 读取 RS485 设备（真实场景 + jSerialComm 实战）.assets\image-20260121224628786.png)

因为我们要模拟温控器，因此需要定义一下slave，如图所示：

![image-20260121225512080](E:\文章\Java x 工业智能\Modbus over Serial：用 Java 读取 RS485 设备（真实场景 + jSerialComm 实战）.assets\image-20260121225512080.png)

在 Holding Registers 表格中，设置 **Address 0（即 40001） = 256**（代表 25.6℃）

✅ 此时，Modbus Slave 已作为“温控器”在 COM3 上运行。



## 四、Step 2：用 Modbus Poll 验证通信

1. 启动 **Modbus Poll**
2. Connection → Connect
   - Serial Port: **COM4**（与 Slave 配对的另一端）
   - 其他参数同上（9600, 8, N, 1）
   - Slave ID: **1**
3. 设置显示格式：
   - Setup → Read/Write Definition
     - Function: **3 (Read Holding Registers)**
     - Address: **0**
     - Quantity: **1**
     - Scan Rate: **1000 ms**

✅ 你会看到窗口中持续显示 **256** —— 通信成功！

![image-20260121225816100](E:\文章\Java x 工业智能\Modbus over Serial：用 Java 读取 RS485 设备（真实场景 + jSerialComm 实战）.assets\image-20260121225816100.png)

> 📌 这一步至关重要：**先确保物理层和协议层通了，再写代码**。



## 五、Step 3：用 Java 读取串口 Modbus RTU

在前两步中，我们已经用 **Modbus Slave** 模拟了一台温控器（监听 COM3），并用 **Modbus Poll** 通过 COM4 成功读取到温度值 `256`（即 25.6℃）。
现在，我们要用 **Java 程序替代 Modbus Poll**，实现同样的功能——但**不依赖任何 native 库（如 RXTX）**。

#### 为什么不用 Jamod/j2mod？

虽然 Jamod 是经典选择，但它底层依赖 **RXTX**，而 RXTX 在现代系统上常因 `UnsatisfiedLinkError` 导致部署失败。
为追求**稳定性、跨平台性与零 native 依赖**，我们采用：

- **jSerialComm**：纯 Java 串口库，JAR 内置各平台 native 驱动；
- **手动构造 Modbus RTU 帧**：协议简单，仅需 CRC16 校验，代码透明可控。

#### 第一步：添加 Maven 依赖

```xml
<dependency>
    <groupId>com.fazecast</groupId>
    <artifactId>jSerialComm</artifactId>
    <version>2.10.4</version>
</dependency>
```

> 💡 jSerialComm 是目前最活跃、最易用的 Java 串口库，支持 Windows/Linux/macOS，无需额外安装驱动。

#### 第二步：编写 Modbus RTU 工具类

```java
package io.github.iweidujiang.industry.modbusoverserial.util;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Modbus RTU 工具类（含 CRC16 计算）
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/1/21
 */
public class ModbusRTU {

    // CRC16-MODBUS 校验（标准）
    public static int calculateCRC(byte[] data, int offset, int length) {
        int crc = 0xFFFF;
        for (int i = offset; i < offset + length; i++) {
            crc ^= (data[i] & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc >>= 1;
                    crc ^= 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        return crc;
    }

    // 构造读保持寄存器请求帧（功能码 03）
    public static byte[] buildReadHoldingRegistersFrame(int slaveId, int startAddress, int quantity) {
        byte[] frame = new byte[6];
        frame[0] = (byte) slaveId;
        frame[1] = 0x03; // 功能码
        frame[2] = (byte) (startAddress >> 8);
        frame[3] = (byte) (startAddress & 0xFF);
        frame[4] = (byte) (quantity >> 8);
        frame[5] = (byte) (quantity & 0xFF);

        int crc = calculateCRC(frame, 0, 6);
        byte[] fullFrame = new byte[8];
        System.arraycopy(frame, 0, fullFrame, 0, 6);
        fullFrame[6] = (byte) (crc & 0xFF);
        fullFrame[7] = (byte) (crc >> 8);
        return fullFrame;
    }

    // 从响应帧中提取寄存器值（假设只读1个寄存器）
    public static int extractRegisterValue(byte[] response) {
        if (response.length < 7 || response[1] != 0x03) {
            throw new RuntimeException("Invalid response");
        }
        return ((response[3] & 0xFF) << 8) | (response[4] & 0xFF);
    }

    // 发送请求并读取响应（带超时）
    public static byte[] sendAndReceive(SerialPort serialPort, byte[] request) throws IOException, InterruptedException {
        OutputStream out = serialPort.getOutputStream();
        InputStream in = serialPort.getInputStream();

        // 清空输入缓冲区（防止残留数据干扰）
        while (in.available() > 0) in.read();

        // 发送请求
        out.write(request);
        out.flush();

        // 等待响应（Modbus RTU 至少 3.5 字符时间，9600 波特率下约 4ms/字节）
        Thread.sleep(100); // 简单等待，工业场景可优化为字节到达检测

        // 读取响应
        byte[] buffer = new byte[256];
        int len = in.read(buffer);
        if (len <= 0) {
            throw new RuntimeException("No response from device");
        }
        return java.util.Arrays.copyOf(buffer, len);
    }
}

```

#### 第三步：主程序 —— 读取温度

```java
package io.github.iweidujiang.industry.modbusoverserial;

import com.fazecast.jSerialComm.SerialPort;
import io.github.iweidujiang.industry.modbusoverserial.util.ModbusRTUUtils;

import java.util.Arrays;

/**
 * Modbus RTU 读取
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/1/21
 */
public class ModbusRtuReader {
    public static void main(String[] args) throws Exception {
        // 配置串口（必须与 Modbus Slave 完全一致！）
        String portName = "COM4"; // （Linux: "/dev/ttyUSB0"）
        SerialPort serialPort = SerialPort.getCommPort(portName);
        serialPort.setComPortParameters(9600, 8, 1, 0); // 9600,8,N,1
        serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);

        if (!serialPort.openPort()) {
            System.err.println("❌ 无法打开串口: " + portName);
            return;
        }

        try {
            // 构造请求：从站ID=1，读地址0（即40001），读1个寄存器
            byte[] request = ModbusRTUUtils.buildReadHoldingRegistersFrame(1, 0, 1);
            System.out.println("📤 发送请求: " + bytesToHex(request));

            // 发送并等待响应（简单延时，工业场景可优化）
            serialPort.getOutputStream().write(request);
            serialPort.getOutputStream().flush();

            Thread.sleep(100); // 等待设备响应（Modbus RTU 响应通常 < 50ms）

            // 读取响应
            byte[] buffer = new byte[256];
            int len = serialPort.getInputStream().read(buffer);
            if (len <= 0) {
                throw new RuntimeException("未收到响应");
            }
            byte[] response = Arrays.copyOf(buffer, len);
            System.out.println("📥 收到响应: " + bytesToHex(response));

            // 🔍 解析温度值
            int rawValue = ModbusRTUUtils.extractRegisterValue(response);
            double temperature = rawValue / 10.0; // 缩放因子：×10 存储
            System.out.printf("✅ 当前温度: %.1f ℃\n", temperature);

        } catch (Exception e) {
            System.err.println("💥 通信失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            serialPort.closePort();
        }
    }

    // 辅助方法：字节数组转十六进制字符串（用于调试）
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim();
    }
}

```

运行效果：

```
📤 发送请求: 01 03 00 00 00 01 84 0A
📥 收到响应: 01 03 02 01 00 B9 D4
✅ 当前温度: 25.6 ℃
```



## 六、常见问题排查

| 现象            | 可能原因                                   | 解决方案                       |
| --------------- | ------------------------------------------ | ------------------------------ |
| 无响应          | 串口号错 / 波特率不匹配                    | 用 Modbus Poll 先调通          |
| 返回异常码 0x02 | 寄存器地址越界                             | 确认设备支持该地址             |
| CRC 错误        | 校验位设置错误（如设备用 Even，你设 None） | 仔细核对手册                   |
| 读到乱码        | 停止位或数据位错误                         | 通常为 8-N-1，但老设备可能不同 |

> **建议**：在开发机上始终保留 Modbus Poll，用于快速验证。



## 七、扩展：连接真实硬件

如果你有 **USB 转 RS485 模块**（如 CH340、FT232RL）：

1. 将模块 A/B 线接到温控器的 485+ / 485-；
2. 在设备管理器中查看分配的 COM 号（如 COM7）；
3. 在 Java 代码中替换 `"COM4"` 为 `"COM7"`；
4. 确保温控器供电且地址设为 1。

> ⚠️ 注意：RS485 是**半双工**，确保总线终端电阻已加（120Ω），否则长距离通信易出错。



## 八、小结

- ✅ **Modbus RTU 是工业现场的“普通话”**，必须掌握；
- ✅ 调试流程：**Modbus Slave（模拟）→ Modbus Poll（验证）→ Java（集成）**；
- ✅ 串口参数（波特率、校验等）必须与设备**严格一致**；
- ✅ 先用专业工具调通，再写代码，事半功倍。



## 结语

现在，你的 Java 程序不仅能连网络设备，还能深入车间底层，与 RS485 设备对话。
这才是真正的 **“软硬协同”** 能力。



------

欢迎点赞、留言、转发，让更多 Java 开发者精准对接工业设备！