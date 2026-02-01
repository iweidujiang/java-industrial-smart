package io.github.iweidujiang.industry.plc.model;

/**
 * 数据采集点定义
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 *
 * @date 2026/2/1
 */

import com.github.s7connector.api.DaveArea;
import lombok.Data;

@Data
public class DataPoint {
    private String name;        // 点位名称，如 "温度"
    private String address;     // 地址，如 "DB1.DBW0" 或 "100"
    private String dataType;    // 数据类型，如 "INT", "FLOAT", "BOOL"

    // 用于 s7connector 的解析字段
    private DaveArea daveArea;
    private int dbNumber;       // DB编号或区域偏移
    private int byteOffset;     // 字节偏移
    private int bitOffset;      // 位偏移（仅 BOOL 用）
    private int size;           // 要读取的字节数
    private boolean isBool;     // 是否为布尔量

    public void parseSiemensAddress() {
        if (address == null || address.isEmpty()) {
            throw new IllegalArgumentException("地址不能为空");
        }

        String addr = address.trim().toUpperCase();

        // 处理 I1.0, Q1.0, M1.0 这类布尔地址
        if (addr.matches("[IMQ]\\d+\\.\\d+")) {
            char areaChar = addr.charAt(0);
            String[] parts = addr.split("\\.");
            int byteOff = Integer.parseInt(parts[0].substring(1));
            int bitOff = Integer.parseInt(parts[1]);

            switch (areaChar) {
                case 'I':
                    daveArea = DaveArea.DI;
                    break;
                case 'Q':
                    daveArea = DaveArea.OUTPUTS;
                    break;
                case 'M':
                    daveArea = DaveArea.FLAGS;
                    break;
                default:
                    throw new IllegalArgumentException("不支持的区域：" + areaChar);
            }
            this.dbNumber = 0; // I/Q/M 不需要 DB 编号
            this.byteOffset = byteOff;
            this.bitOffset = bitOff;
            this.size = 1;
            this.isBool = true;
            return;
        }

        // 处理 DB1.DBW0, DB1.DBD4 等
        if (addr.startsWith("DB")) {
            daveArea = DaveArea.DB;
            String[] parts = addr.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("DB 地址格式错误：" + addr);
            }

            try {
                this.dbNumber = Integer.parseInt(parts[0].substring(2)); // DB1 → 1
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("无效的 DB 编号：" + parts[0]);
            }

            String dataPart = parts[1];
            if (dataPart.startsWith("DB")) {
                dataPart = dataPart.substring(2); // DBW0 → W0
            }

            if (dataPart.startsWith("B")) {
                this.byteOffset = Integer.parseInt(dataPart.substring(1));
                this.size = 1;
            } else if (dataPart.startsWith("W")) {
                this.byteOffset = Integer.parseInt(dataPart.substring(1));
                this.size = 2;
            } else if (dataPart.startsWith("D")) {
                this.byteOffset = Integer.parseInt(dataPart.substring(1));
                this.size = 4;
            } else {
                throw new IllegalArgumentException("不支持的数据标识：" + dataPart);
            }
        } else {
            throw new IllegalArgumentException("不支持的地址格式：" + addr);
        }
    }
}
