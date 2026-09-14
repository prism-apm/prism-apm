---
created: 2026-05-18 16:04:05
modified: 2026-08-07 17:51:54
---
# 角色

- Parser: 接收并根据规则解析 trace，保存到数据库中
- UI Server: 提供不同维度的数据查询能力。


# 架构

系统从 Kafka 接收 trace，并进行解析保存到数据库中。暂时不支持直接从 OpenTelemetry Collector 直接接收数据。

![](../../assets/PrismAPM-architecture.svg)



Prism APM 当前由以下主要模块组成：

| 模块                    | 说明                                          |
| --------------------- | ------------------------------------------- |
| `prism-apm-parser`    | 从 Kafka 消费 OpenTelemetry trace 数据，解析后写入数据库。 |
| `prism-apm-ui-server` | 提供数据查询、权限配置等相关功能的后端服务。                      |
| `prism-apm-ui`        | 前端页面，开发环境由 Vite 启动，生产环境构建为静态资源。             |
| `prism-apm-common`    | 后端公共实体、Mapper、工具和测试辅助模块。                    |
| `sql/`                | 数据库 schema、权限和路由规则初始化等脚本。                   |

# 存储

数据库使用的是 TimescaleDB。选择 TimescaleDB 的主要原因如下：

- 功能强大：有很多数据聚合相关功能和函数。
- 性能强：单机性能强劲，足以应付很多公司的业务场景。
- 容易上手：因为是基于 Postgres 开发的，所以上手比较容易。
- 主备功能：基于 Postgres。



