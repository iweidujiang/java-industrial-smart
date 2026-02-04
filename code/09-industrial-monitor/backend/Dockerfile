# 使用官方 OpenJDK 21 JRE（轻量）
FROM eclipse-temurin:21-jre-alpine

# 设置中国时区（工业系统必备）
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 创建工作目录
WORKDIR /app

# 复制 JAR 文件
COPY target/industrial-monitor-1.0.0.jar ./app.jar

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]