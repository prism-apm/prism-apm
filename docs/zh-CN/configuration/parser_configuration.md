---
created: 2026-07-29 16:46:48
modified: 2026-08-03 17:12:16
---

# Parser 配置

本文档说明 `prism-apm-parser` 的运行时配置。Parser 负责从 Kafka 消费 OpenTelemetry Trace JSON，解析后写入 PostgreSQL/TimescaleDB。

如果需要理解 Parser 的整体处理流程，请参考：

- [Parser 架构](../architecture/parser_architecture.md)

# 最小必需配置

## 环境变量

| 环境变量 | 示例 | 说明 |
| --- | --- | --- |
| `DB_ENDPOINT` | `postgres:5432` | PostgreSQL/TimescaleDB 地址，格式为 `host:port`。 |
| `DB_NAME` | `apm` | 数据库名。 |
| `DB_USERNAME` | `postgres` | 数据库用户名。 |
| `DB_PASSWORD` | `your-db-password` | 数据库密码。 |
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | Kafka broker 地址。 |

## 示例

```bash
export DB_ENDPOINT=localhost:5432
export DB_NAME=apm
export DB_USERNAME=postgres
export DB_PASSWORD=your-db-password
export KAFKA_BOOTSTRAP_SERVERS=localhost:9094
```

## 内部映射

应用内部会使用以下配置模板生成最终连接配置：

```properties
spring.datasource.url=jdbc:postgresql://${DB_ENDPOINT:localhost:5432}/${DB_NAME:apm}?useUnicode=true&characterEncoding=UTF-8&rewriteBatchedStatements=true
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9094}
```

## 默认配置

`server.port`、`parser.kafka_topic` 和 `parser.reprocessing_kafka_topic` 都有默认值。只有端口冲突、上游 Trace topic 名称不是默认值，或需要使用非默认重处理 topic 时，才需要覆盖这些配置。


# Kafka 配置

Parser 通过 Spring Kafka 自动配置创建 consumer 和 producer。正常情况下只需要设置 `KAFKA_BOOTSTRAP_SERVERS`，项目会映射为 `spring.kafka.bootstrap-servers`。

## 内置行为

| 行为            | 默认值               | 说明                   |
| ------------- | ----------------- | -------------------- |
| 消费组           | `apm-span-parser` | Parser 默认消费组。        |
| offset 提交     | 手动提交              | Parser 批量处理完成后确认消息。  |
| 消费方式          | 批量消费              | 每次 poll 一批 Trace 消息。 |
| 无 offset 时的起点 | `latest`          | 新消费组从最新消息开始消费。       |

## 可选调优参数

以下参数通常不需要配置，只有在吞吐量、消息大小或资源占用不符合预期时才建议覆盖：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `spring.kafka.consumer.max-poll-records` | `50` | 单次 poll 最大消息数。 |
| `spring.kafka.listener.concurrency` | `4` | Kafka listener 并发数。 |
| `spring.kafka.producer.properties.max.request.size` | `10485760` | Producer 单次请求最大大小，默认 10 MB。 |
| `spring.kafka.producer.buffer-memory` | `67108864` | Producer buffer memory，默认 64 MB。 |

## Topic 配置

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `parser.kafka_topic` | `opentelemetry-trace` | Parser 正常消费的 Trace topic。 |
| `parser.reprocessing_kafka_topic` | `reprocessing-opentelemetry-trace` | Parser 重处理消费的 Trace topic。 |

`parser.reprocessing_kafka_topic` 不需要为了正常接入 Trace 显式设置；未配置时会使用默认值。只有需要使用重处理能力，或重处理 topic 名称不是默认值时，才需要覆盖它。

## 注意事项

- 要使用 `traceId` 作为 Kafka message key。如果使用 OpenTelemetry Collector 的话，针对 Kafka 这个 exporter 需要设置 `partition_traces_by_id: true`。
- Topic 分区数不小于 Parser 的消费并发数。

# Kafka SASL 配置

此配置主要用于是否使用认证访问 Kafka。如果 Kafka 开启 SASL，可以启用 `application-kafka-sasl.properties`：

## 启用方式

```bash
java -jar prism-apm-parser/target/prism-apm-parser-*.jar \
  --spring.profiles.active=kafka-sasl
```

## 环境变量

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `KAFKA_SECURITY_PROTOCOL` | `SASL_PLAINTEXT` | Kafka security protocol。 |
| `KAFKA_SASL_MECHANISM` | `PLAIN` | Kafka SASL mechanism。 |
| `KAFKA_USERNAME` | `admin` | Kafka 用户名。 |
| `KAFKA_PASSWORD` | `change-me` | Kafka 密码。生产环境必须替换。 |

启用 `kafka-sasl` profile 后，项目会把这些环境变量映射为 Spring Kafka 的安全配置，并自动组装 JAAS 配置。

如果 Kafka 使用 PLAINTEXT，不要启用 `kafka-sasl` profile。

# 缓存配置

Parser 使用本地 Ehcache 暂存 Span，用于等待父 Span 到达并生成调用关系。Kafka 中同一 Trace 的 Span 可能乱序到达，Parser 会先把解析出的 Span 写入缓存，再由延迟队列按 `spanId` 取出并尝试查找父 Span。

## 常用配置

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `cache.timeout_s` | `600` | Span 在缓存中的保留时间，单位为秒。 |
| `cache.heap_percent` | `25` | `spanCache` 可使用的 JVM 最大堆内存比例。 |
| `cache.entry_size_bytes` | `1000` | 单条 Span 缓存项的估算大小，单位为字节。 |
| `resource.hash.cache.size_entries` | `100000` | 目标资源 hash 缓存的最大条目数。 |

## 本地缓存

Parser 中有两个本地缓存：

| 缓存 | 用途 | 容量控制 |
| --- | --- | --- |
| `spanCache` | 保存 Span JSON，用于等待父 Span 并生成调用关系。 | 由 `cache.heap_percent` 和 `cache.entry_size_bytes` 计算最大条目数。 |
| `resource_hash_cache` | 保存目标资源名称到 hash UUID 的映射，减少重复计算。 | 由 `resource.hash.cache.size_entries` 控制最大条目数。 |

## resource_hash_cache 内存估算

`resource_hash_cache` 的默认最大条目数是 `100000`。每个缓存值是一个 UUID，UUID 本身至少需要 16 字节，因此只计算 UUID value 的理论下限约为：

```text
100000 * 16 B = 1,600,000 B ≈ 1.5 MiB
```

实际内存会更高，因为还需要保存资源名称字符串 key、Java 对象开销和 Ehcache 条目管理开销。资源名称越长，占用越高。例如平均资源名按 100 字节粗略估算，仅 key 和 UUID 的原始内容就约为：

```text
100000 * (100 B + 16 B) ≈ 11 MiB
```

再加上对象和缓存管理开销后，默认 `resource.hash.cache.size_entries=100000` 通常应按数 MB 到数十 MB 的堆内存预算看待。

## spanCache 容量估算

`spanCache` 的最大条目数不是直接配置的，而是根据 JVM 最大堆内存估算：

```text
spanCache 最大条目数 = JVM -Xmx * cache.heap_percent / 100 / cache.entry_size_bytes
```

例如，Parser 使用 `-Xmx2g`，`cache.heap_percent=25`，`cache.entry_size_bytes=1000` 时：

```text
2 GB * 25% / 1000 B ≈ 536,870 条
```

这个值是估算值。Ehcache 实际按条目数限制缓存，不会精确按字节限制内存；`cache.entry_size_bytes` 越小，允许的条目数越多，实际内存风险也越高。

## 缓存保留时间

`cache.timeout_s` 决定 Span 最长能在缓存中等待多久。它需要和延迟队列的父 Span 等待时间一起设置：

```text
cache.timeout_s * 1000 > dependency.timeoutThreshold
```

如果 `cache.timeout_s` 太短，父 Span 可能还没到达，子 Span 或父 Span 就已经从缓存中过期，最终调用关系会不完整。

如果 `cache.timeout_s` 太长，乱序 Span 更容易被正确关联，但缓存保留的数据更多，内存占用会增加。

## 调参建议

| 场景 | 建议 |
| --- | --- |
| `stats.log` 中缓存淘汰或过期很多，并且调用关系不完整 | 适当增大 `cache.timeout_s`，并确认它大于 `dependency.timeoutThreshold`。 |
| JVM 内存压力较高或频繁 GC | 降低 `cache.heap_percent`，增大 JVM `-Xmx`，或检查 Trace 写入量是否超过当前实例能力。 |
| `spanCache` 条目数明显不足 | 增大 `cache.heap_percent`，或适当增大 `-Xmx`。 |
| 单条 Span 内容明显大于默认估算 | 增大 `cache.entry_size_bytes`，让最大条目数估算更保守。 |
| 目标资源名称很多，hash 重复计算明显 | 增大 `resource.hash.cache.size_entries`。 |

生产环境建议先保留默认值运行，观察 `logs/stats.log` 和 `logs/cache.log`。如果 DelayQueue 堆积、缓存 miss/expiration/eviction 明显偏高，再结合 Trace 写入量和 JVM 内存逐步调整。

# 延迟队列配置

Kafka 中 Span 到达顺序不稳定，子 Span 可能先于父 Span 到达。Parser 使用本地 `DelayQueue` 等待父 Span，随后生成服务调用关系。

## 常用配置

| 配置项                                      | 默认值     | 说明                                                            |
| ---------------------------------------- | ------- | ------------------------------------------------------------- |
| `delay.component.local.thread_num`       | `4`     | 正常处理 DelayQueue 消费线程数。                                        |
| `delay.component.local.batch_size`       | `10`    | 每批最多处理的 Span 数。                                               |
| `delay.component.local.batch_timeout_ms` | `1000`  | 当数据数量达不到 `delay.component.local.batch_size` 的话，等待的最长时长。单位为毫秒。 |
| `delayMillis`                            | `3000`  | 父 Span 未到达时，延迟处理子 Span 的间隔，单位为毫秒。                             |
| `dependency.timeoutThreshold`            | `60000` | 等待父 Span 的最大时间，单位为毫秒。                                         |
| `executor.service.shutdown.timeout_ms`   | `10000` | 正常处理线程池关闭等待时间，单位为毫秒。                                          |

## 与缓存时间的关系

建议满足：

```text
cache.timeout_s > dependency.timeoutThreshold
```

如果 `dependency.timeoutThreshold` 过短，父 Span 可能尚未到达，子 Span 就会按无父 Span 记录保存，导致调用关系、上下游和拓扑不完整。

如果 `dependency.timeoutThreshold` 过长，父子关系可能更完整，但会增加缓存占用和数据写入延迟。

# 日志配置

日志配置位于：

```text
prism-apm-parser/src/main/resources/logback-spring.xml
```

## 日志目录

Parser 默认写入日志目录：

```text
logs
```

## 日志滚动

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `logging.custom.server.max-file-size` | `100MB` | 单个日志文件最大大小。 |
| `logging.custom.server.max-history` | `30` | 日志保留天数。 |
| `logging.custom.server.total-size-cap` | `50GB` | 日志总大小上限。 |

## 日志文件

| 文件                          | 说明                     |
| --------------------------- | ---------------------- |
| `logs/server.log`           | 主服务日志。                 |
| `logs/reprocessing.log`     | 重处理相关日志。               |
| `logs/unparsed_span.log`    | Span 解析失败或不完整相关日志。     |
| `logs/stats.log`            | 定时输出的缓存、队列和 Span 统计信息。 |
| `logs/cache.log`            | 缓存过期和淘汰事件日志。           |
