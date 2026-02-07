# 10-手摸手教你部署工业监控大屏项目——Docker 与 Kubernetes 双方案实战

> 🧭 本文属于专栏《Java × 工业智能》第 10 篇 | GitHub 源码：[github.com/iweidujiang/java-industrial-smart](https://github.com/iweidujiang/java-industrial-smart)



本篇来实操一下通过 Docker 和 k8s 来部署以下三个组件：

| 组件               | 技术栈               | 功能                           |
| ------------------ | -------------------- | ------------------------------ |
| `redis`            | 官方镜像             | 缓存模拟设备数据（温度、压力） |
| `monitor-backend`  | Spring Boot (JDK 21) | 提供 `/api/data/latest` 接口   |
| `monitor-frontend` | Vue 3 + Nginx        | 实时曲线 + 告警弹窗            |

> 所有操作均在 **本地完成** 。
>
> 我在 [Windows 下 Docker 安装与使用全攻略](https://mp.weixin.qq.com/s/wKJrVvm6njPN9p0ge_CUtw) 这篇文章中已经提供了详细的在Windows11上安装 **Docker** 的方法，本文就在此基础上带大家体验一下在Windows上玩所谓的“云原生”。
>
> 上车！

![0071nci4gy1hnqu263mi6g308c08cwf7](E:\文章\Java x 工业智能\10-手摸手教你部署工业监控大屏项目——Docker 与 Kubernetes 双方案实战.assets\0071nci4gy1hnqu263mi6g308c08cwf7.gif)



## 二、准备工作：构建本地镜像

### 步骤 1：进入后端目录，构建 JAR 包

```bash
cd code/09-industrial-monitor/backend
mvn clean package -DskipTests
```

> 建议在 `pom.xml` 中添加 `<finalName>monitor-backend</finalName>`，使 JAR 名固定为 `monitor-backend.jar`，方便创建镜像时 jar 包名称保持一致。



### 步骤 2：前端项目创建 `Dockerfile`

在 `backend/` 目录下新建 `Dockerfile`：

```dockerfile
# 使用官方 OpenJDK 21 JRE（轻量）
FROM eclipse-temurin:21-jre-alpine

# 设置中国时区（工业系统必备）
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 创建工作目录
WORKDIR /app

# 复制 JAR 文件
COPY target/monitor-backend.jar ./app.jar

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
```

> **这里的 JDK 版本必须和 pom.xml 中的编译版本一致。**，否则会报 `UnsupportedClassVersionError`。

构建镜像：

```bash
docker build -t monitor-backend:local .
```

![后端项目镜像构建成功](E:\文章\Java x 工业智能\10-手摸手教你部署工业监控大屏项目——Docker 与 Kubernetes 双方案实战.assets\后端项目镜像构建成功.png)

### 步骤 3：为前端创建 Nginx 配置

在 `frontend/` 目录下新建 `nginx.conf`：

```nginx
server {
    listen 80;
    server_name localhost;

    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 关键：反向代理 API 请求到 backend 服务
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

> **为什么末尾不加 `/`？**
> 因为 Spring Boot 接口路径是 `/api/...`，保留原路径转发即可。若加 `/`，会变成 `/...`，导致 404。



### 步骤 4：前端项目创建 `Dockerfile`

```dockerfile
FROM nginx:alpine

# 删除默认 index.html
RUN rm /etc/nginx/conf.d/default.conf

# 复制自定义配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制前端构建产物
COPY dist/ /usr/share/nginx/html/

EXPOSE 80
```

构建前端镜像：

```bash
cd ../frontend
npm run build
docker build -t monitor-frontend:local .
```

![前端build](E:\文章\Java x 工业智能\10-手摸手教你部署工业监控大屏项目——Docker 与 Kubernetes 双方案实战.assets\前端build.png)

![前端Docker镜像构建成功](E:\文章\Java x 工业智能\10-手摸手教你部署工业监控大屏项目——Docker 与 Kubernetes 双方案实战.assets\前端Docker镜像构建成功.png)

### 步骤 5：验证镜像

```bash
docker images
# 应看到 monitor-backend:local 和 monitor-frontend:local
```

![查看镜像](E:\文章\Java x 工业智能\10-手摸手教你部署工业监控大屏项目——Docker 与 Kubernetes 双方案实战.assets\查看镜像.png)

## 三、方案一：Docker Compose —— 一键启动

### 1. 创建 `docker-compose.yml`

```yaml
version: '3.8'

services:
  redis:
    image: redis:7-alpine
    container_name: industrial-redis
    ports:
      - "6379:6379"
    volumes:
      - E:/docker_service_data/java_industrial_redis:/data
    command:
      - "redis-server"
      - "--requirepass"
      - "devRed1s"
      - "--appendonly"
      - "yes"
    restart: unless-stopped

  backend:
    image: monitor-backend:local
    container_name: monitor-backend
    depends_on:
      - redis
    environment:
      - REDIS_HOST=redis
    restart: always

  frontend:
    image: monitor-frontend:local
    container_name: monitor-frontend
    depends_on:
      - backend
    ports:
      - "80:80"
    restart: always

```

> **关键说明**：
>
> - `REDIS_HOST=redis`：利用 Compose 内置 DNS，指向 Redis 容器；
> - `command` 为 Redis 设置密码，必须与后端配置一致；
> - 前端通过 Nginx 代理 `/api/` 到 `backend:8080`，**无需暴露 8080 端口到宿主机**。

### 2. 启动 & 访问

```bash
cd code/10-deploy-dashboard
docker-compose up -d
```

打开浏览器 ，访问： `http://localhost` ：

![image-20260204174042120](E:\文章\Java x 工业智能\10-手摸手教你部署工业监控大屏项目——Docker 与 Kubernetes 双方案实战.assets\image-20260204174042120.png)



## 四、方案二：Kubernetes 原生部署

以下文件的根目录是 **10-deploy-dashboard** 。

### 1. `k8s/redis.yaml`

```yaml
apiVersion: v1
kind: Service
metadata:
  name: redis
spec:
  ports:
    - port: 6379
      targetPort: 6379
  selector:
    app: redis
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
        - name: redis
          image: redis:7-alpine
          command:
            - "redis-server"
            - "--requirepass"
            - "devRed1s"
            - "--appendonly"
            - "yes"
          ports:
            - containerPort: 6379
```

### 2. `k8s/backend.yaml`

```yaml
apiVersion: v1
kind: Service
metadata:
  name: backend
spec:
  type: ClusterIP
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: backend
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
spec:
  replicas: 1
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
    spec:
      containers:
        - name: backend
          image: monitor-backend:local
          imagePullPolicy: IfNotPresent
          env:
            - name: REDIS_HOST
              value: "redis"
            - name: REDIS_PASSWORD
              value: "devRed1s"
```

### 3. `k8s/frontend.yaml`

```yaml
apiVersion: v1
kind: Service
metadata:
  name: frontend
spec:
  type: LoadBalancer
  ports:
    - port: 80
      targetPort: 80
  selector:
    app: frontend
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
spec:
  replicas: 1
  selector:
    matchLabels:
      app: frontend
  template:
    metadata:
      labels:
        app: frontend
    spec:
      containers:
        - name: frontend
          image: monitor-frontend:local
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 80
```

### 4. 部署 & 访问

```bash
kubectl apply -f k8s/
kubectl get services  # 等待 EXTERNAL-IP 变为 localhost
```

![k8s启动](E:\文章\Java x 工业智能\10-手摸手教你部署工业监控大屏项目——Docker 与 Kubernetes 双方案实战.assets\k8s启动.png)

访问 `http://localhost` 即可。



## 五、常见问题排查

| 问题           | 原因                   | 解决                                                      |
| -------------- | ---------------------- | --------------------------------------------------------- |
| 后端启动失败   | JDK 版本不匹配         | 使用 `eclipse-temurin:21-jre-alpine`                      |
| Redis 连接拒绝 | 密码未设置或 host 错误 | `command: ["--requirepass", "..."]` + `REDIS_HOST=redis`  |
| 前端 API 404   | Nginx 未代理 `/api/`   | 配置 `location /api/ { proxy_pass http://backend:8080; }` |
| 页面白屏       | `dist/` 未生成         | 先运行 `npm run build`                                    |



## 六、关于部署方案的几点说明

在本篇中，我刻意 **没有使用 Helm Chart** ，也 **没有引入复杂的 Ingress 或 Service Mesh** ，原因如下：

1. **规避网络依赖，确保本地可运行**

   Helm 仓库（如 Artifact Hub）通常需要访问外网，而在工业现场或内网开发环境中，网络受限是常态。

   采用原生 YAML 或 Docker Compose，所有资源均可本地构建、离线部署，彻底摆脱对远程仓库的依赖。

2. **项目体量小，Docker Compose 完全够用**

   本系统仅包含三个服务（Redis + 后端 + 前端），无复杂调度、自动扩缩容或跨节点通信需求。

   Docker Compose 以极简的配置即可实现服务编排、网络互通与数据持久化，学习成本低、调试直观，非常适合中小型监控场景。

3. **Kubernetes 原生 YAML 已足够表达意图**

   即便选择 K8s，我也仅使用了 `Deployment` + `Service` 这两个最基础的资源对象。

   这既保留了向真实集群（如 K3s、RKE2）迁移的可能性，又避免了 Helm 模板抽象带来的理解负担——**看得见的 YAML，才是可控的部署**。

4. **前端反向代理由 Nginx 承担，职责清晰**

   将 API 路由逻辑放在前端容器的 Nginx 中，而非依赖外部网关，使得整个大屏系统成为一个自包含的交付单元。

   无论部署在工控机、边缘服务器还是云虚拟机，只需暴露一个 80 端口，即可完整运行。

> **用最简单的工具，解决最确定的问题**。



------

## 附录：完整目录结构

```
code/
├── 09-industrial-monitor/
│   ├── backend/
│   │   ├── pom.xml
│   │   ├── src/
│   │   ├── target/monitor-backend.jar
│   │   ├── Dockerfile
│   └── frontend/
│       ├── dist/
│       ├── nginx.conf
│       ├── Dockerfile
│       └── ...
└── 10-deploy-dashboard/
    ├── docker-compose.yml
    └── k8s/
        ├── redis.yaml
        ├── backend.yaml
        └── frontend.yaml
```

 所有配置均已在 **Docker Desktop（Windows + WSL2）** 上实测通过。

