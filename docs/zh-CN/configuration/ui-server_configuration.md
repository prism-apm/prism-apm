---
created: 2026-07-30 16:41:14
modified: 2026-08-03 14:51:11
---

# UI Server 配置

本文档说明 `prism-apm-ui-server` 的运行时配置。UI Server 负责提供查询、拓扑、管理、权限和登录鉴权接口。

如果需要理解 UI Server 的职责、接口分组和数据访问约定，请参考：

- [UI Server](../UI%20Server.md)

# 最小必需配置

启动 UI Server 时，用户通常需要提供读写数据库连接信息和 JWT 签名密钥。不要直接编写完整的 JDBC URL，项目会根据 `DB_WRITE_*` 和 `DB_READ_*` 环境变量自动组装数据源配置。

## 环境变量

| 环境变量 | 示例 | 说明 |
| --- | --- | --- |
| `DB_WRITE_ENDPOINT` | `postgres:5432` | 写库地址，格式为 `host:port`。 |
| `DB_WRITE_DB_NAME` | `apm` | 写库数据库名。 |
| `DB_WRITE_USERNAME` | `postgres` | 写库用户名。 |
| `DB_WRITE_PASSWORD` | `your-db-password` | 写库密码。 |
| `DB_READ_ENDPOINT` | `postgres:5432` | 读库地址，格式为 `host:port`。 |
| `DB_READ_DB_NAME` | `apm` | 读库数据库名。 |
| `DB_READ_USERNAME` | `postgres` | 读库用户名。 |
| `DB_READ_PASSWORD` | `your-db-password` | 读库密码。 |
| `JWT_SECRET` | `replace-with-a-strong-random-secret` | JWT HMAC 签名密钥。 |

单数据库部署时，读库和写库可以指向同一个 PostgreSQL/TimescaleDB 实例，但 `DB_WRITE_*` 和 `DB_READ_*` 两组变量都需要设置。

`JWT_SECRET` 必须使用足够长的随机值。UI Server 使用 HS256 签名 JWT，密钥至少需要 32 字节。不要使用示例值或仓库中的占位值。

## 示例

```bash
export DB_WRITE_ENDPOINT=localhost:5432
export DB_WRITE_DB_NAME=apm
export DB_WRITE_USERNAME=postgres
export DB_WRITE_PASSWORD=your-db-password

export DB_READ_ENDPOINT=localhost:5432
export DB_READ_DB_NAME=apm
export DB_READ_USERNAME=postgres
export DB_READ_PASSWORD=your-db-password

export JWT_SECRET="$(openssl rand -base64 32)"
```

## 内部映射

应用内部会使用以下配置模板生成最终的数据源和 JWT 配置：

```properties
spring.datasource.dynamic.datasource.master.url=jdbc:postgresql://${DB_WRITE_ENDPOINT}/${DB_WRITE_DB_NAME}?useUnicode=true&characterEncoding=UTF-8&rewriteBatchedStatements=true&preferQueryMode=simple
spring.datasource.dynamic.datasource.master.username=${DB_WRITE_USERNAME}
spring.datasource.dynamic.datasource.master.password=${DB_WRITE_PASSWORD}

spring.datasource.dynamic.datasource.slave.url=jdbc:postgresql://${DB_READ_ENDPOINT}/${DB_READ_DB_NAME}?useUnicode=true&characterEncoding=UTF-8&rewriteBatchedStatements=true&preferQueryMode=simple
spring.datasource.dynamic.datasource.slave.username=${DB_READ_USERNAME}
spring.datasource.dynamic.datasource.slave.password=${DB_READ_PASSWORD}

jwt.secret=${JWT_SECRET:change-me}
jwt.expiration=${JWT_EXPIRATION:86400000}
```

## 默认配置

`server.port` 未在配置文件中覆盖，使用 Spring Boot 默认端口 `8080`。只有端口冲突，或部署平台要求使用其他端口时，才需要覆盖 `server.port`。

`JWT_EXPIRATION` 有默认值 `86400000`，单位为毫秒，即 24 小时。只有需要调整登录 token 有效期时，才需要覆盖它。

# 数据库配置

UI Server 使用动态数据源：

| 数据源 | 内部名称 | 用途 |
| --- | --- | --- |
| 写库 | `master` | 管理、权限、项目、拓扑规则等写操作。 |
| 读库 | `slave` | 普通查询默认数据源。 |

默认主数据源是 `slave`。写操作通过代码中的 `@DS("master")` 切换到写库。

## 单库部署

如果没有独立只读库，可以把读库和写库配置为同一个数据库：

```bash
export DB_WRITE_ENDPOINT=localhost:5432
export DB_WRITE_DB_NAME=apm
export DB_WRITE_USERNAME=postgres
export DB_WRITE_PASSWORD=your-db-password

export DB_READ_ENDPOINT=localhost:5432
export DB_READ_DB_NAME=apm
export DB_READ_USERNAME=postgres
export DB_READ_PASSWORD=your-db-password
```

## 读写分离部署

如果使用主从或读写分离，需要确保：

- `DB_WRITE_*` 指向可写数据库。
- `DB_READ_*` 指向只读库或读副本。

## 数据库初始化

UI Server 依赖仓库根目录 `sql/` 下的数据库对象和初始化数据。首次运行前需要完成数据库初始化，否则登录、权限、服务查询和拓扑查询可能无法正常工作。

# JWT 配置

UI Server 使用 JWT 做登录态认证。

## 环境变量

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `JWT_SECRET` | `change-me` | JWT HMAC 签名密钥。实际部署必须覆盖。 |
| `JWT_EXPIRATION` | `86400000` | JWT 有效期，单位为毫秒。 |

虽然 `JWT_SECRET` 在配置文件中有占位默认值，但该值不适合实际部署。HS256 需要至少 32 字节密钥，过短的密钥可能导致应用启动失败。

## 多实例部署

如果 UI Server 部署多个实例，所有实例必须使用同一个 `JWT_SECRET`。否则一个实例签发的 token 可能无法被另一个实例验证。

如果更换 `JWT_SECRET`，旧 token 会立即失效，用户需要重新登录。

## 有效期设置

`JWT_EXPIRATION` 单位为毫秒：

| 示例值 | 含义 |
| --- | --- |
| `3600000` | 1 小时。 |
| `86400000` | 24 小时。 |
| `604800000` | 7 天。 |

有效期越短，泄露 token 的风险窗口越小，但用户需要更频繁登录。有效期越长，用户体验更平滑，但安全风险更高。

# 预热任务配置

UI Server 包含一个可选的 PostgreSQL/TimescaleDB 预热任务，用于定期调用数据库函数，降低冷查询延迟。该任务默认关闭。

## 配置项

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `apm.schedule.prewarm-enabled` | `false` | 是否启用预热任务。 |
| `apm.schedule.prewarm-function` | `prewarm_data` | 要调用的数据库函数名，不需要带括号。 |
| `apm.schedule.prewarm-cron` | `* 0/10 * * * ?` | 任务 Cron 表达式。 |

## 启用条件

只有在以下条件都满足时，才建议开启预热任务：

- 数据库中已经创建对应的预热函数。
- UI 查询在一段时间不访问后存在明显冷启动变慢。
- 数据库可以承受定时预热带来的额外查询负载。

启用示例：

```bash
java -jar prism-apm-ui-server/target/prism-apm-ui-server-*.jar \
  --apm.schedule.prewarm-enabled=true \
  --apm.schedule.prewarm-function=prewarm_data \
  --apm.schedule.prewarm-cron="0 0/10 * * * ?"
```

如果 Cron 表达式无效，代码会回退到默认 Cron 表达式并记录错误日志。

# 日志配置

日志配置位于：

```text
prism-apm-ui-server/src/main/resources/logback-spring.xml
```

## 默认行为

默认不指定 Spring profile 时，日志输出到控制台。

启用 `prod` 或 `test` profile 时，根日志写入文件：

```text
logs/ui-server-yyyy-MM-dd.i.log
```

## 日志滚动

当前日志滚动策略写在 `logback-spring.xml` 中，不是通过环境变量配置：

| 日志 | 单文件大小 | 保留天数 | 总大小上限 |
| --- | --- | --- | --- |
| UI Server 主日志 | `100MB` | `10` | `10GB` |
| 数据库排查日志 | `200MB` | `3` | `5GB` |

如果需要修改日志文件大小、保留天数或日志目录，需要调整 `logback-spring.xml`。

## 数据库排查日志

`logback-spring.xml` 中单独配置了数据库排查日志：

```text
logs/db-trace-yyyy-MM-dd.i.log
```

该日志用于 HikariCP、PostgreSQL driver、动态数据源和 Mapper 层排查。数据库相关日志可能增长很快，排查完成后应恢复正常日志级别并清理不需要的日志文件。

# OpenTelemetry Java Agent 配置

`prism-apm-ui-server` 的 Docker 镜像默认携带 OpenTelemetry Java Agent。直接运行 jar 时不会自动启用该 agent，除非手动添加 `-javaagent` 参数。

## Docker 默认配置

Dockerfile 中默认设置：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `OTEL_SERVICE_NAME` | `prism-apm-ui-server` | 上报到 OpenTelemetry 的服务名。 |
| `OTEL_TRACES_EXPORTER` | `otlp` | Trace exporter。 |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://host.docker.internal:4318` | OTLP endpoint。 |
| `OTEL_METRICS_EXPORTER` | `none` | 不上报 metrics。 |
| `OTEL_LOGS_EXPORTER` | `none` | 不上报 logs。 |
| `OTEL_SEMCONV_STABILITY_OPT_IN` | `http,database,messaging,rpc` | OpenTelemetry 语义约定选择。 |

## 什么时候需要覆盖

如果 OpenTelemetry Collector 不在 `host.docker.internal:4318`，需要覆盖 `OTEL_EXPORTER_OTLP_ENDPOINT`。

如果不希望 UI Server 上报 Trace，可以设置：

```bash
export OTEL_TRACES_EXPORTER=none
```

