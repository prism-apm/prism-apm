---
created: 2026-05-28 14:29:02
modified: 2026-08-04 16:48:05
---


本文档说明如何在公开环境中部署 Prism APM。内容以当前仓库状态为准，优先覆盖本地开发和单节点服务器部署。

各个服务的详细配置，请参看：
- [parser configuration](configuration/parser_configuration.md)
- [ui-server configuration](configuration/ui-server_configuration.md)


# 前期准备


## 部署架构

一个最小可用环境需要：

- `prism-apm-parser`：负责数据接入和解析写入。
- `prism-apm-ui-server`：提供 UI 查询接口和登录鉴权接口。
- `prism-apm-ui`：提供浏览器访问页面。
- `TimescaleDB`：保存 trace 解析结果、聚合结果、权限数据和查询配置。
- `Kafka`：接收上游 trace 数据，供 `prism-apm-parser` 消费。

## 前置条件

建议先准备以下环境：

| 依赖                  | 建议版本                        | 用途                   |
| ------------------- | --------------------------- | -------------------- |
| JDK                 | 21                          | 后端编译和运行。             |
| Maven               | 3.9+                        | 构建 Java 模块。          |
| Node. Js            | `>=22.12.0`                 | 构建和运行前端开发服务。         |
| npm                 | 随 Node. Js 安装               | 安装前端依赖。              |
| TimescaleDB         | 支持 `timescaledb` 扩展         | 创建 hypertable 和连续聚合。 |
| TimescaleDB Toolkit | 支持 `timescaledb_toolkit` 扩展 | SQL 脚本中会创建该扩展。       |
| Kafka               | 兼容 Spring Kafka 客户端         | trace 数据输入。          |
| `psql`              | 与 PostgreSQL 匹配             | 执行数据库初始化脚本。          |

如果使用容器运行，请确保容器网络可以访问 TimescaleDB 和 Kafka。容器中的 `localhost` 指向容器自身，不是宿主机。

## 端口和默认配置

| 组件                    | 默认端口                              | 来源                                                           |
| --------------------- | --------------------------------- | ------------------------------------------------------------ |
| `prism-apm-parser`    | `8090`                            | `prism-apm-parser/src/main/resources/application.properties` |
| `prism-apm-ui-server` | `8080`                            | Spring Boot 默认端口，当前配置文件未覆盖。                                  |
| `prism-apm-ui`        | `8066`                            | `prism-apm-ui/vite.config.ts`                                |
| 前端开发代理                | `/api` -> `http://127.0.0.1:8080` | `prism-apm-ui/vite.config.ts`                                |

默认 Kafka topic：

| 配置项                               | 默认值                                |
| --------------------------------- | ---------------------------------- |
| `parser.kafka_topic`              | `opentelemetry-trace`              |
| `parser.reprocessing_kafka_topic` | `reprocessing-opentelemetry-trace` |

## 数据库初始化

### 创建数据库

以下示例使用本地数据库和数据库名 `apm`：

```bash
CREATE DATABASE apm;
```


### 执行 SQL 脚本

从仓库根目录按顺序执行：

```bash
psql -d apm -f sql/10_create_service_call_objects.sql
psql -d apm -f sql/11_create_app_config_objects.sql
psql -d apm -f sql/20_init-common-permission.sql
psql -d apm -f sql/21_init-common-replacer-rule.sql
psql -d apm -f sql/22_init-common-table-routing-rules.sql
psql -d apm -f sql/30_warmup_data.sql
```

说明：

- `10_create_service_call_objects.sql` 会创建 TimescaleDB 扩展、核心 trace 表、hypertable 和连续聚合。
- `11_create_app_config_objects.sql` 会创建权限、项目、路由规则和解析配置等应用表。
- `20_*`、`21_*`、`22_*` 脚本会写入公共初始化数据。
- `30_warmup_data.sql` 包含预热相关函数，适合在需要预热 TimescaleDB 元数据时使用。

### 数据库权限注意事项

`10_create_service_call_objects.sql` 当前包含如下行为：

- 创建 `timescaledb` 扩展。
- 创建 `timescaledb_toolkit` 扩展。
- 创建用于 TimescaleDB job 的数据库用户 `ts_job_runner`。

执行该脚本的数据库用户需要有创建扩展、建表、建视图、建用户和授权的权限。生产环境不要直接使用示例密码；上线前应替换为安全密码，或按实际 DBA 流程预先创建用户并调整脚本。

### 验证数据库

执行：

```bash
psql -d apm -c "select extname from pg_extension where extname in ('timescaledb', 'timescaledb_toolkit');"
psql -d apm -c "select count(*) from information_schema.tables where table_schema = 'public';"
psql -d apm -c "select count(*) from service_call;"
```

如果 `service_call` 表不存在，说明核心 SQL 未成功执行。


# 本地启动项目

1，首先下载项目。

2，启动方式：

```shell
cd docker-compose
docker compose up -d
```

`docker-compose` 包含了 jaeger collector 相关组件。启动完后，可以直接尝试发送 trace 到 `4318` 端口。可以使用下面地址查看 trace 接口和处理情况。
- `http://localhost:16686`：查看 trace 是否已经被正常接收到。

3，先通过 `http://localhost:8066/#/admin` 配置项目。通过 `http://localhost:8066` 查看项目调用情况。默认用户名和密码如下：
- User Name: `admin`
- Password：`apm-admin`

# 生产或单节点部署

单节点部署可以采用以下方式：

1. 在服务器上部署 TimescaleDB。
2. 部署 Kafka，或连接已有 Kafka 集群。
3. 执行 `sql/` 下初始化脚本。
4. 构建并启动 `prism-apm-parser`。
5. 构建并启动 `prism-apm-ui-server`。
6. 构建 `prism-apm-ui`，将 `dist/` 交给 Nginx 或其他静态服务器托管。
7. 在 Nginx 中将 `/api` 反向代理到 `prism-apm-ui-server`。


## 启动 Parser

下面是运行 Parser 容器示例。启动前，请修改相关配置。

```bash
docker run --rm \
  -p 8090:8090 \
  -e DB_ENDPOINT=host.docker.internal:5432 \
  -e DB_NAME=apm \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=your-db-password \
  -e KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9094 \
  -e KAFKA_USERNAME=admin \
  -e KAFKA_PASSWORD=your-kafka-password \
  -e PARSER_KAFKA_TOPIC=opentelemetry-trace \
  prism-apm-parser \
  --spring.profiles.active=kafka-sasl
```

如果连接 Kafka 不需要 SASL 验证的话，示例如下：

```bash
docker run --rm \
  -p 8090:8090 \
  -e DB_ENDPOINT=host.docker.internal:5432 \
  -e DB_NAME=apm \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=your-db-password \
  -e KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9094 \
  -e PARSER_KAFKA_TOPIC=opentelemetry-trace \
  prism-apm-parser 
```


## 启动 UI Server

下面是运行 UI Server 容器示例。启动前，请修改相关配置。


```bash
docker run --rm \
  -p 8080:8080 \
  -e JWT_SECRET=replace-with-a-strong-secret \
  -e JWT_EXPIRATION=86400000 \
  -e DB_WRITE_ENDPOINT=host.docker.internal:5432 \
  -e DB_WRITE_DB_NAME=apm \
  -e DB_WRITE_USERNAME=postgres \
  -e DB_WRITE_PASSWORD=your-db-password \
  -e DB_READ_ENDPOINT=host.docker.internal:5432 \
  -e DB_READ_DB_NAME=apm \
  -e DB_READ_USERNAME=postgres \
  -e DB_READ_PASSWORD=your-db-password \
  prism-apm-ui-server --spring.profiles.active=local
```

## 启动 UI

构建 UI：

```bash
cd prism-apm-ui
npm install
npm run build
```

构建产物默认输出到：

```text
prism-apm-ui/dist/
```

Nginx 配置示例：

```nginx
server {
    listen 80;
    server_name prism-apm.example.com;

    root /opt/prism-apm-ui/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```


# 验证

## 验证 Parser

1，查看 `PARSER_KAFKA_TOPIC` Topic 中的是否有数据。

2，查看 `log/server.log` 是否正常输出。

3，查看数据库的 `service_call` 表中是否有数据。

4，查看数据库的 `service_call_minute_entry ` 表中是否有数据。

## 验证 UI Server

登录接口允许匿名访问，可以用来验证 HTTP 服务是否可达。注意替换 IP 地址。

```bash
curl -i -X POST "http://localhost:8080/api/apm/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data "username=admin&password=replace-with-real-password"
```

如果返回 `401` 或登录失败，但 HTTP 响应正常，说明服务已启动，接下来应检查初始化用户和密码。

## 验证前端

访问下面的地址验证是否可达。注意替换 IP 地址。

```text
http://localhost:8066
```

生产环境访问部署后的静态站点地址。打开浏览器开发者工具，确认 `/api` 请求被正确代理到 `prism-apm-ui-server`。



# 扩容

`prism-apm-ui-server` 都是无状态的，支持横向扩容。`prism-apm-parser` 是有状态的，重启或扩容会损失一些处理中的数据，具体请看 [parser](architecture/parser_architecture.md) 的说明。
