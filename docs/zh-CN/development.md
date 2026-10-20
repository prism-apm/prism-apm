---
created: 2026-06-24 15:11:30
modified: 2026-10-20 17:26:33
---

# 开发指南

本文档面向 Prism APM 的开发者，说明本地开发时需要理解和维护的内容。部署流程和运行时参数已经分别放在部署文档和配置文档中，本页重点记录项目结构、模块边界、启动方式、常见改动入口和验证方法。

# 项目概览

## 项目定位

Prism APM 是一个面向 OpenTelemetry Trace 数据的应用性能监控项目。系统从 Kafka 消费 Trace 数据，由 Parser 解析并写入 PostgreSQL/TimescaleDB，再由 UI Server 提供查询、权限和管理接口，前端负责服务概览、接口详情、数据库访问、拓扑和后台管理页面展示。

当前项目主要支持 OpenTelemetry Trace JSON 数据接入。Parser 不直接接收 OTLP HTTP 或 OTLP gRPC，也不保存完整原始 Trace。

## 模块职责

| 模块 | 职责 | 开发时重点 |
| --- | --- | --- |
| `prism-apm-common` | 公共实体、MyBatis Mapper 和测试辅助代码。 | 数据库公共表、解析配置、字段替换规则、Testcontainers 测试基类。 |
| `prism-apm-parser` | 从 Kafka 消费 OpenTelemetry Trace JSON，解析 Span，关联父子 Span，写入 TimescaleDB。 | 解析规则、字段抽取、缓存和 DelayQueue、批量写入、重处理接口。 |
| `prism-apm-ui-server` | 提供前端查询 API、登录鉴权、权限管理、项目管理、拓扑规则和表路由。 | Controller、Service、Mapper XML、动态数据源、JWT 权限、查询性能。 |
| `prism-apm-ui` | Vue 2 + Vite 前端。 | 页面模块、API 封装、路由、Pinia 状态、国际化、图表和拓扑展示。 |
| `prism-explorer` | 本地演示和模拟数据工具。 | 一键体验环境、初始化数据库、生成模拟数据、文档截图数据准备。 |
| `sql/` | 生产/本地通用数据库初始化脚本。 | TimescaleDB 扩展、核心表、连续聚合、权限和路由规则初始化数据。 |
| `docker-compose/` | 完整本地依赖和服务编排。 | TimescaleDB、Kafka、Jaeger、Parser、UI Server、UI 的联调环境。 |
| `scripts/` | 文档截图和模拟数据辅助脚本。 | Playwright 截图、快速开始截图、功能文档截图、模拟数据生成。 |

## 推荐阅读顺序

第一次参与开发时，建议按以下顺序理解项目：

1. 阅读 [项目介绍](introduction.md)，了解 Prism APM 的目标。
2. 阅读 [快速开始](getting-started.md)，先跑通 `prism-explorer` 演示环境。
3. 阅读 [整体架构](architecture/architecture.md) 和 [Parser 架构](architecture/parser_architecture.md)，理解 Trace 从 Kafka 到页面的链路。
4. 阅读 [Parser 配置](configuration/parser_configuration.md) 和 [UI Server 配置](configuration/ui-server_configuration.md)，理解关键环境变量。
5. 再回到本文档，根据要改的模块选择启动和验证方式。

# 本地开发

## 开发环境

建议准备以下工具：

| 工具             | 建议版本            | 用途                                               |
| -------------- | --------------- | ------------------------------------------------ |
| JDK            | 21              | 后端编译、测试和运行。                                      |
| Maven Wrapper  | 由 `./mvnw` 管理的 Maven 3.9.9 | 构建 Java 模块，不要求本机预先安装 Maven。              |
| Node.js        | `>=22.12.0`     | 前端开发服务和构建，版本要求来自 `prism-apm-ui/package.json`。    |
| npm            | 随 Node.js 安装    | 安装前端依赖。                                          |
| Docker Desktop | 4.0+            | 运行 TimescaleDB、Kafka、Jaeger 和 Testcontainers 测试。 |
| `psql`         | 与 PostgreSQL 匹配 | 手动执行和验证数据库脚本。                                    |

后端模块使用 Spring Boot 3.2.5 和 Java 21。前端使用 Vue 2.7、Vue Router 3、Pinia、iView、ECharts 和 AntV G6。

Maven 命令请在仓库根目录使用 `./mvnw` 执行。Windows 环境请使用 `mvnw.cmd`。

## 本地依赖

完整联调通常需要：

| 组件 | 默认地址 | 说明 |
| --- | --- | --- |
| TimescaleDB | `localhost:5432` | 主 Docker Compose 默认映射。 |
| Kafka | `localhost:9094` | 主 Docker Compose 暴露给宿主机的地址。 |
| Jaeger Collector OTLP HTTP | `localhost:4318` | 用于接收 OTLP HTTP Trace。 |
| Jaeger UI | `http://localhost:16686` | 查看 Jaeger 收到的 Trace。 |
| Parser | `http://localhost:8090` | Parser 服务和重处理接口。 |
| UI Server | `http://localhost:8080` | 前端 API。 |
| UI 开发服务 | `http://127.0.0.1:8066` | Vite dev server。 |


## 构建


### 服务打包

构建后端时，先进入到 `prism-apm` 目录下，然后执行下面的命令。

```bash
./mvnw -pl prism-apm-common clean install -DskipTests
./mvnw -pl prism-apm-parser clean package -DskipTests
./mvnw -pl prism-apm-ui-server clean package -DskipTests
```

或者

```bash
./mvnw -pl prism-apm-parser -am clean package -DskipTests
./mvnw -pl prism-apm-ui-server -am clean package -DskipTests
```


构建前端：

```bash
cd prism-apm-ui
npm install
npm run build
```

### 构建镜像

构建镜像时，先进入到 `prism-apm` 目录下，然后执行下面的命令。

```bash
docker build -f prism-apm-parser/Dockerfile -t prism-apm-parser .
docker build -f prism-apm-ui-server/Dockerfile -t prism-apm-ui-server .
docker build -f prism-apm-ui/Dockerfile -t prism-apm-ui .
```

`prism-apm-ui-server/Dockerfile` 会下载 OpenTelemetry Java Agent，构建该镜像时需要可以访问 Maven Central。

如果不需要验证本地 Dockerfile 改动，也可以直接拉取已发布的镜像：

```bash
docker pull prismapm/prism-apm-parser:latest
docker pull prismapm/prism-apm-ui-server:latest
docker pull prismapm/prism-apm-ui:latest
```

如需可复现的测试环境，请将 `latest` 替换为 `0.1.0` 等明确的发布版本。


## Docker compose 启动完整环境

从仓库根目录启动主 Docker Compose：

```bash
cd docker-compose
docker compose up -d
```

这套编排包含 TimescaleDB、Kafka、Jaeger、Parser、UI Server 和 UI，默认使用 Docker Hub 上 `prismapm` 命名空间中的 `latest` 镜像。

如需使用本地构建的 `prism-apm-parser`、`prism-apm-ui-server` 和 `prism-apm-ui` 镜像，请显式将镜像前缀设置为空：

```bash
PRISM_IMAGE_PREFIX= docker compose up -d
```

如需测试固定版本或改用阿里云 ACR，可以覆盖镜像标签或仓库前缀：

```bash
PRISM_IMAGE_TAG=0.1.0 docker compose up -d

PRISM_IMAGE_PREFIX=crpi-16i4tdv4pz11ple8.cn-beijing.personal.cr.aliyuncs.com/prism-apm/ \
PRISM_IMAGE_TAG=0.1.0 \
docker compose up -d
```


## 单独启动服务

### 启动 Parser

Parser 的入口类是 `prism-apm-parser/src/main/java/org/prism/apm/parser/Application.java`，默认端口是 `8090`。

最小本地配置：

```bash
export DB_ENDPOINT=localhost:5432
export DB_NAME=apm
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export KAFKA_BOOTSTRAP_SERVERS=localhost:9094
```

启动：

```bash
./mvnw -pl prism-apm-parser spring-boot:run
```

如果 Kafka 开启 SASL，启动时启用 `kafka-sasl` profile，并设置 `KAFKA_USERNAME`、`KAFKA_PASSWORD` 等变量。具体请参看 [parser configuration](configuration/parser_configuration.md)

### 启动 UI Server

UI Server 的入口类是 `prism-apm-ui-server/src/main/java/org/prism/apm/ui/server/Application.java`，默认端口是 Spring Boot 默认值 `8080`。

最小本地配置：

```bash
export DB_WRITE_ENDPOINT=localhost:5432
export DB_WRITE_DB_NAME=apm
export DB_WRITE_USERNAME=postgres
export DB_WRITE_PASSWORD=postgres
export DB_READ_ENDPOINT=localhost:5432
export DB_READ_DB_NAME=apm
export DB_READ_USERNAME=postgres
export DB_READ_PASSWORD=postgres
export JWT_SECRET=change-me-change-me-change-me-change-me
```

启动：

```bash
./mvnw -pl prism-apm-ui-server spring-boot:run
```

UI Server 会给所有 `@RestController` 自动添加 `/api` 前缀。登录接口是 `POST /api/apm/login`，前端会把登录后的 token 放入 `localStorage.apm_token`，并在后续请求中发送 `Authorization: Bearer <token>`。

### 启动前端

前端目录是 `prism-apm-ui`。开发服务默认监听 `127.0.0.1:8066`，并把 `/api` 代理到 `http://127.0.0.1:8080`。

```bash
cd prism-apm-ui
npm install
npm run dev
```

浏览器打开：

```text
http://127.0.0.1:8066/
```

默认登录账号来自初始化 SQL：

| 用户名     | 密码          |
| ------- | ----------- |
| `admin` | `apm-admin` |

# 数据库开发

数据库初始化脚本位于仓库根目录 `sql/`，执行顺序如下：

```bash
psql -d apm -f sql/10_create_service_call_objects.sql
psql -d apm -f sql/11_create_app_config_objects.sql
psql -d apm -f sql/20_init-common-permission.sql
psql -d apm -f sql/21_init-common-replacer-rule.sql
psql -d apm -f sql/22_init-common-table-routing-rules.sql
psql -d apm -f sql/30_warmup_data.sql
```

开发数据库相关功能时，需要特别关注：

| 主题             | 说明                                            |
| -------------- | --------------------------------------------- |
| TimescaleDB 扩展 | `timescaledb` 和 `timescaledb_toolkit` 都需要安装。  |
| Hypertable     | Trace 明细表按时间组织，写入和查询都应保留时间条件。                 |
| 连续聚合           | UI 查询主要依赖分钟级聚合数据，迟到 Span 和重处理会影响聚合窗口。         |
| 表路由            | UI Server 通过表路由规则选择查询对象，相关代码在 `route` 包。      |
| 读写分离           | UI Server 默认主数据源是 `slave`，写操作需要显式使用 `master`。 |

修改 SQL 后，至少需要验证脚本可以在空数据库按顺序执行，并确认核心表、扩展、初始化账号和路由规则存在。

# 验证与排查

## 测试

后端测试使用 JUnit 5、Spring Boot Test 和 Testcontainers。运行 Testcontainers 测试前需要确保 Docker 可用。

常用测试命令：

```bash
./mvnw -pl prism-apm-common test
./mvnw -pl prism-apm-parser -am test
./mvnw -pl prism-apm-ui-server -am test
```

按改动选择验证范围：

| 改动                         | 推荐验证                                                                                                              |
| -------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| SQL 初始化、TimescaleDB 表或连续聚合 | `TimescaleDdlInitializationTest`、`ApmTimescaleSchemaDdlTest`、`ApmTimescaleCaggCalculationTest`                    |
| Parser 字段解析或组件规则           | `OpenTelemetrySpanParserTest`、`OpenTelemetrySpanInfoBuilderTest`、`ParameterizedProcessingPipelineTest`            |
| Kafka 配置                   | `KafkaNoAuthConnectionTest`、`KafkaSaslConnectionTest`                                                             |
| UI Server 表路由              | `TableRoutingServiceTest`、`TableRoutingStoreTest`                                                                 |
| UI Server 查询 SQL           | `ServiceCallMinuteDOMapper4ServiceTest`、`ServiceCallMinuteDOMapper4TargetTest`、`ServiceCallMinuteDOMapper4DBTest` |
| 权限和后台管理                    | `AdminUserServiceTest`、`AdminProjectServiceTest`、`AdminPermissionServiceTest`、`AdminAssignServiceTest`            |
| 前端构建                       | `npm run build`                                                                                                   |


## 日志和排查

Parser 主要日志：

| 文件 | 说明 |
| --- | --- |
| `logs/server.log` | 主服务日志。 |
| `logs/reprocessing.log` | 重处理日志。 |
| `logs/unparsed_span.log` | Span 解析失败或字段缺失。 |
| `logs/stats.log` | 缓存、队列和 Span 处理统计。 |
| `logs/cache.log` | Ehcache 过期和淘汰事件。 |

UI Server 在默认 profile 下主要输出到控制台；启用 `prod` 或 `test` profile 后会写入 `logs/ui-server-yyyy-MM-dd.i.log` 和 `logs/db-trace-yyyy-MM-dd.i.log`。

常见排查顺序：

1. 页面无数据时，先确认 UI Server 登录和查询接口是否正常。
2. 再确认 `service_call` 和分钟级聚合表是否有数据。
3. 如果数据库无数据，检查 Parser 是否消费到 Kafka 消息。
4. 如果 Parser 消费到了消息但没有写入，检查 `unparsed_span.log`、`stats.log` 和解析规则。
5. 如果拓扑或上下游关系不完整，检查 Kafka 是否按 `traceId` 分区，以及 `cache.timeout_s` 和 `dependency.timeoutThreshold` 的关系。

# 文档维护

## 常见改动需要补充的文档

开发过程中，如果修改了以下内容，需要同步更新文档：

| 改动                            | 需要更新的文档                                       |
| ----------------------------- | --------------------------------------------- |
| 新增或修改部署方式                     | `deployment.md`                               |
| 新增 Parser 配置项                 | `configuration/parser_configuration.md`       |
| 新增 UI Server 配置项              | `configuration/ui-server_configuration.md`    |
| 修改数据库初始化脚本                    | 部署文档、配置文档、相关架构文档                              |
| 修改 Trace 解析规则或支持新组件           | `architecture/parser_architecture.md`、`features/parser_feature.md` |
| 新增前端功能页面                      | 对应 `features/**` 文档                           |
| 修改登录、权限或默认账号                  | `features/admin_feature.md` 和 UI Server 配置文档          |
| 修改 Quickstart 或 Explorer 体验流程 | `getting-started.md`                          |
| 更新文档截图                        | `docs/assets/**` 和 `scripts/doc-snapshot/README.md` |


## 文档截图

文档截图脚本位于 `scripts/`，运行前需要先启动 `prism-explorer` 环境。

安装 Playwright：

```bash
mkdir -p .doc-tools
npm install --prefix .doc-tools playwright
npx --prefix .doc-tools playwright install chromium
```

常用命令：

```bash
node scripts/doc-snapshot/capture-quickstart-screenshots.mjs
node scripts/doc-snapshot/capture-service-metrics-screenshots.mjs
node scripts/doc-snapshot/capture-topology-screenshots.mjs
```

截图脚本默认会通过 Prism Explorer 清理并生成最近 7 天模拟数据，再登录 Prism APM UI 截图。已有合适数据时可以使用 `--skip-data`。
