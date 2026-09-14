---
created: 2026-08-04 17:33:12
modified: 2026-08-11 16:25:33
---
# 页面能打开但没有任何数据

## 现象

Prism APM UI 可以正常打开，用户也可以登录，但服务概览、服务列表、端点详情、数据库详情或拓扑页面没有展示数据。

这种情况通常说明前端静态页面已经可访问，但查询链路、时间范围、权限范围或聚合数据存在问题。

## 可能原因

- 当前查询时间范围内没有 `service_call` 明细数据。
- Parser 已写入明细数据，但分钟级、5 分钟、小时或天级连续聚合还没有可查询数据。
- 页面选择的时区不正确，导致 UI Server 查询到错误的 UTC 时间范围。
- 当前用户没有目标服务所属项目的访问权限。
- UI Server 连接的读库不是 Parser 正在写入的数据库。

## 排查方式

先确认 UI Server 查询接口是否返回空数据。可以在浏览器开发者工具的 Network 面板中查看 `/api/apm/common/service-object-list`、`/api/apm/service/overview` 等请求：

- 如果接口返回 `401`，优先排查登录状态。
- 如果接口返回 `403`，优先排查角色权限和项目权限。
- 如果接口返回 `status=200` 但 `data` 为空，继续检查数据和查询条件。
- 如果接口返回 `status=500` 或 `err` 不为空，查看 UI Server 日志中的具体异常。

如果上面的检查都没问题，继续检查下面的数据。

- 检查明细表是否有当前时间范围内的数据。如果 `count(*)` 为 `0`，说明 Parser 还没有写入可查询数据，应继续排查“Parser 无法消费或无数据写入”。
- 如果 `service_call` 有数据，再检查服务名是否已经写入。
- 如果明细表有数据但页面仍为空，检查是分钟级聚合(`service_call_minute_entry`)是否有数据。
- 再检查 `UI-Server` 中的路由规则是否存在。


# 当前没有数据，但有一段时间之前的数据

## 现象

页面可以查到一段时间之前的数据，但最近一段时间没有数据。例如选择最近 5 分钟、15 分钟或 1 小时时为空，选择更早的时间范围时仍然可以看到服务、接口或拓扑数据。

这种情况和“页面能打开但没有任何数据”不同：历史数据存在，说明 UI Server、权限、表路由和基础查询链路大概率是可用的，问题更可能出在实时写入链路。

## 可能原因

- Kafka 消息积压，造成写到数据库里的数据时间超出了数据的聚合时间。例如：分级级数据的聚合时间是最近 10 分钟的数据。因为积压问题，数据处理完到数据时已经超出了聚合的时间范围，所以会一直没有新的聚合数据产生。
- `prism-apm-parser` 服务已经停止、重启失败，或无法继续消费 Kafka 消息。
- Parser 仍在运行，但消费速度低于上游写入速度，导致 Kafka 消息积压，最新数据还没有被处理到数据库。

## 排查方式


如果最近几分钟完全没有数据，但更早时间有数据，优先检查 Parser 是否还在正常处理。查看 Parser 主日志：

```text
logs/server.log
```

再查看 Parser 统计日志：

```text
logs/stats.log
```

重点看每分钟输出中的：

- `saved span` 是否还在增长。
- `timeout span` 是否快速增长。
- `delay queue size` 是否持续升高。
- `reprocessing delay queue size` 是否异常升高。

如果 `saved span` 不再增长，通常说明 Parser 没有继续成功保存 Span。继续查看解析失败日志：

```text
logs/unparsed_span.log
logs/unparsed_service.log
```

如果 `saved span` 仍在增长，但 `max(time)` 明显落后当前时间，通常说明 Parser 仍在处理积压数据，或者上游发送的是较早时间的 Trace。

如果使用读写分离数据库，再分别在读库和写库执行 `select max(time) from service_call;`。如果写库时间更新、读库时间落后，问题在读库同步延迟。



# Parser 无法消费或无数据写入

## 现象

上游已经发送 Trace 数据，但 Prism APM 页面没有数据，数据库中的 `service_call` 表也没有新增记录。

## 可能原因

- Parser 消费的 Kafka topic 与上游写入的 topic 不一致。
- Parser 使用默认消费组 `apm-span-parser`，且默认 `auto-offset-reset=latest`，启动前已经写入的历史消息不会被新消费组自动消费。
- Kafka 消息不是 Parser 支持的 OpenTelemetry Trace JSON。
- Parser 可以消费消息，但 Span 解析规则无法生成完整的 `SpanInfo`。
- Parser 连接数据库失败，或写入 `service_call` 时发生异常。

## 排查方式

先检查 Parser 的 topic 配置：

```properties
parser.kafka_topic=opentelemetry-trace
parser.reprocessing_kafka_topic=reprocessing-opentelemetry-trace
```

确认上游写入的 topic 与 `parser.kafka_topic` 一致。

再查看 Parser 主日志：

```text
logs/server.log
```

如果 Parser 正常消费但没有写入，继续查看解析失败日志：

```text
logs/unparsed_span.log
logs/unparsed_service.log
```

查看每分钟统计日志，确认 DelayQueue、缓存和 Span 处理数量是否变化：

```text
logs/stats.log
```

最后检查明细表是否有新增数据：

```sql
select max(time), count(*)
from service_call;
```



# 拓扑图不完整

## 现象

拓扑页面能展示部分节点，但缺少调用边、上游、下游，或服务之间的关系明显不完整。

## 可能原因

- 同一 Trace 的 Span 没有进入同一个 Kafka partition，父子 Span 被不同 Parser 实例处理。
- 子 Span 先于父 Span 到达，父 Span 超过等待阈值后才出现。
- `cache.timeout_s` 小于或接近 `dependency.timeoutThreshold`，父 Span 还没关联就被缓存淘汰。
- Parser 没有解析出 caller 或 callee 需要的关键字段。
- 拓扑聚合表尚未刷新。

## 排查方式


如果使用 OpenTelemetry Collector 的话，确认是否对 Kafka 的 exporter 设置了 `partition_traces_by_id: true`。

检查 Parser 缓存和 DelayQueue 统计：

```text
logs/stats.log
logs/cache.log
```

重点关注：

- DelayQueue 是否持续堆积。
- 缓存过期或淘汰是否明显偏高。
- Span 处理数量是否正常增长。

检查缓存和父子等待时间配置是否满足：

```text
cache.timeout_s * 1000 > dependency.timeoutThreshold
```

检查拓扑相关聚合表是否有数据：

```sql
select min(time), max(time), count(*)
from service_topology_relation_minute;
```

## 处理方式

确保上游 Kafka exporter 使用 `traceId` 作为消息 key，并按 trace id 分区。

如果缓存过期或淘汰明显偏高，适当增大 `cache.timeout_s`，并确认它大于 `dependency.timeoutThreshold`。

如果拓扑表没有数据但 `service_call` 有数据，等待连续聚合刷新，或检查拓扑相关路由规则和聚合对象。

# 登录失败

## 现象

打开登录页后，使用账号密码登录失败。

## 可能原因

- 用户不存在或密码不正确。
- 用户被禁用。
- UI Server 连接的数据库中没有初始化用户数据。
- `JWT_SECRET` 太短，导致 UI Server 无法正常生成 token。

## 排查方式

默认管理员账号来自初始化数据：

```text
username: admin
password: apm-admin
```

检查用户是否存在并启用：

```sql
select id, username, enabled
from users
where username = 'admin';
```

如果登录接口返回 `401`，说明 UI Server 可达，但认证未通过。

## 处理方式

确认 UI Server 连接的是包含 `users`、`roles`、`role_user` 初始化数据的数据库。

如果用户被禁用，使用管理员账号或数据库维护流程启用用户。

如果日志中出现 JWT 密钥相关错误，设置足够长的 `JWT_SECRET`。HS256 签名密钥建议至少 32 字节。


# 接口返回 403 或提示权限不足

## 现象

用户已登录，但访问某些页面或接口时返回 `403`，页面提示权限不足。

## 可能原因

- 用户没有分配角色。
- 用户角色没有分配对应 URL 权限。
- 普通用户没有被加入包含目标项目的项目组。
- 权限刚变更，权限缓存尚未刷新。

## 排查方式

检查用户角色关系：

```sql
select u.username, r.name
from users u
join role_user ru on ru.user_id = u.id
join roles r on r.id = ru.role_id
where u.username = '<username>';
```

检查角色权限关系：

```sql
select r.name as role_name, p.name as permission_name, p.url
from roles r
join role_permission rp on rp.role_id = r.id
join permissions p on p.id = rp.permission_id
where r.name = '<role-name>';
```

检查用户项目组关系：

```sql
select u.username, pg.name as project_group_name
from users u
join user_project_group upg on upg.user_id = u.id
join project_groups pg on pg.id = upg.project_group_id
where u.username = '<username>';
```

## 处理方式

为用户分配正确角色，并为角色分配对应 URL 权限。

如果用户需要查看业务数据，还需要把用户加入包含目标项目的项目组。

权限变更后，等待权限缓存刷新，再让用户重新访问或重新登录。


# 部分 Span 没有被解析

## 现象

Parser 正常运行，也有部分数据写入 `service_call`，但某些服务、组件、接口或数据库访问没有出现在页面中。

## 可能原因

- 当前组件没有匹配到 `parser-component-config.yml` 中的组件规则。
- Trace 属性名称与配置中的属性路径不一致。
- 必填字段没有取到有效值。
- 服务被 `service_parse_config` 配置为不解析。
- 字段替换规则处理后导致目标名称不符合预期。

## 排查方式

查看解析失败日志：

```text
logs/unparsed_span.log
logs/unparsed_service.log
```

检查组件解析配置：

```text
prism-apm-parser/src/main/resources/parser-component-config.yml
```

最终生成有效 `SpanInfo` 时，以下字段需要能取到有效值：

- `serviceName`
- `hostName`
- `spanKind`
- `actionName`
- `targetName`
- `component`
- `systemType`

## 处理方式

为当前 instrumentation scope 或 Span attributes 增加组件规则。

如果只是属性名变化，优先在对应字段的 `vals` 中追加新的候选属性路径。

如果部分字段没有来源，可以用 `defaultVal` 提供稳定默认值。

调整规则后重启 Parser，并重新发送 Trace 或使用重处理能力补数据。

# 目标名称过于分散或包含动态参数

## 现象

页面中端点、数据库语句、Redis key 或外部调用目标数量异常多，同一个逻辑接口被拆成大量不同目标。

## 可能原因

- `target_name` 中包含订单号、用户 ID、分页参数、完整 URL query string 等动态内容。
- `field_content_replacer_rule` 没有配置对应组件的替换规则。
- 替换规则被禁用。
- 替换规则的 `service_name`、`component` 或 `field_name` 与实际数据不匹配。

## 排查方式

先查看目标名称分布：

```sql
select component, span_kind, target_name, count(*)
from service_call
group by component, span_kind, target_name
order by count(*) desc
limit 50;
```

检查替换规则：

```sql
select service_name, component, span_kind, field_name, match_type, search_content, replacement, enabled
from field_content_replacer_rule
order by service_name, component, span_kind, field_name;
```

## 处理方式

为动态 URL、SQL、Redis key 或消息目标增加替换规则，将变量部分归一化为稳定占位符。

确认规则的 `field_name` 指向要处理的字段，例如 `targetName` 或 `remoteAddr`。

确认 `enabled=true`，并让 Parser 重新加载配置或重启 Parser。

# 如何使用 Parser 的 `stats.log`

## 现象

需要判断 Parser 是否还在处理数据、是否存在积压、Span 父子关联是否异常，或者缓存是否已经成为瓶颈时，可以优先查看 `logs/stats.log`。

`stats.log` 是 Parser 每分钟输出一次的运行统计日志。它不记录单条 Trace 的详细内容，而是用于观察 Parser 的整体处理状态。

## 日志内容

`stats.log` 每分钟会输出一组统计信息，通常包含以下几类内容：

```text
outputStats started
ehcache stats. exists:..., puts:..., gets:..., removals:..., hits:..., misses:..., Evictions:..., Expirations:..., OccupiedByteSize:...
delay queue size:...,reprocessing delay queue size:...
span stats. saved span: ..., timeout span: ...
outputStats end
```

其中：

- `saved span`：Parser 成功保存的 Span 记录累计数量。
- `timeout span`：等待父 Span 超时后保存的 Span 累计数量。
- `delay queue size`：正常处理队列中正在等待处理的 Span 数量。
- `reprocessing delay queue size`：重处理队列中正在等待处理的 Span 数量。
- `exists`：当前 `spanCache` 中保留的 Span 数量。
- `puts`：写入 `spanCache` 的累计次数。
- `gets`：读取 `spanCache` 的累计次数。
- `hits`：读取缓存并命中的累计次数。
- `misses`：读取缓存但未命中的累计次数。
- `Evictions`：因为缓存容量限制被淘汰的累计次数。
- `Expirations`：因为超过缓存保留时间过期的累计次数。
- `OccupiedByteSize`：Ehcache OnHeap 层估算占用的字节数。

这些值大多是 Parser 启动后的累计值。排查时不要只看某一行的绝对值，更应该连续观察几分钟，看它们是否增长、增长速度是否异常。

如果 `saved span` 不增长，先确认 Parser 是否仍在运行，再查看 `logs/server.log` 中是否有 Kafka、解析或数据库写入异常。

如果 `delay queue size` 持续升高，说明 Parser 处理链路跟不上输入速度。需要继续结合 `server.log`、数据库写入耗时、Kafka 积压和 Parser 实例资源使用情况判断瓶颈位置。

---


# Kafka 处理消息大小的设置

因为有的 Trace 可包含很多个 Span，所在消息大小可能会非常大。设置如下：

- `message.max.bytes`: 静态参数在 broker 级别生效，影响所有的 topic，需要修改 `server.properties` 文件，并重启 Kafka 集群。
- `max.message.bytes`: 动态参数在 topic 级别生效，只影响指定的 topic，修改后立即生效，无需重启 Kafka 集群。
- `replica.fetch.max.bytes`: 以便大消息可以正常复制到 broker 的副本中。
- `max.partition.fetch.bytes`: 在 consumer 可以消费的最大消息大小。


# 影响吞吐量的地方

Parser 的处理链路可以拆成三段看：Kafka 入口、Parser 内部处理、数据库写入出口。排查吞吐量时不要只看单个参数，应结合 `logs/stats.log` 中的 `delay queue size`、`saved span` 增长速度、Kafka 消费积压、Parser CPU/内存/GC 和数据库写入耗时一起判断。

## Kafka 入口

- Topic Partition 数量：Partition 数量决定同一个消费组内最多有多少个消费者线程可以并行消费。Partition 数量小于 Parser 消费并发数时，多出来的消费线程不会提升吞吐。
- Parser 实例数量：增加实例可以提升整体消费能力，但总并发仍受 Partition 数量限制。扩缩容时还要注意同一 `traceId` 是否仍能进入同一 Partition，否则父子 Span 关联可能不完整。
- Kafka consumer 并发数：`spring.kafka.listener.concurrency` 决定单个 Parser 实例的 Kafka listener 并发数。
- 单次 poll 数量：`spring.kafka.consumer.max-poll-records` 决定每次批量消费的最大消息数。过小会增加 poll 和处理批次开销；过大则会增加单批解析耗时、内存占用和 offset 确认延迟。
- 单条消息大小：如果单条 Trace 包含很多 Span，需要同时确认 Kafka broker、topic、replica 和 consumer 的消息大小限制。具体见上一节 Kafka 消息大小配置。

## Parser 内部处理

- Trace 大小和 Span 数量：Parser 会先反序列化 OpenTelemetry Trace JSON，再遍历其中的 `resourceSpans`、`scopeSpans` 和 `spans`。单条 Trace 中 Span 越多，单条 Kafka 消息的解析成本越高。
- 解析规则复杂度：Parser 会按 `parser-component-config.yml` 中的组件规则匹配 Span，再根据 outputs 取字段。组件规则越多、正则越复杂、字段取值路径越多，CPU 消耗越高。
- 字段替换规则：`field_content_replacer_rule` 中的正则替换会在解析字段时执行。规则数量多或正则复杂时，也会影响解析吞吐。
- Ehcache 容量和 JVM 内存：Span 会先写入本地缓存等待父 Span。`cache.heap_percent`、`cache.entry_size_bytes`、`cache.timeout_s` 设置不合理时，可能引起缓存淘汰、过期、内存压力或频繁 GC。
- DelayQueue 线程数：`delay.component.local.thread_num` 决定从 DelayQueue 取 Span 并写入数据库的线程数。具体请参看 [parser_configuration#延迟队列配置](configuration/parser_configuration.md#延迟队列配置)。
- DelayQueue 批量参数：`delay.component.local.batch_size` 决定每批最多处理多少 Span，`delay.component.local.batch_timeout_ms` 决定凑不满批次时等待多久。批次太小会增加 SQL 次数，批次太大可能增加单次写入耗时和连接占用时间。
- 父 Span 等待时间：`delayMillis` 和 `dependency.timeoutThreshold` 会影响 Span 在 DelayQueue 中重复等待的次数。等待时间过长会增加处理延迟和缓存占用；等待时间过短可能造成调用关系不完整。

## 数据库写入出口

- 数据库连接池大小：真正执行 `batchUpsert` 的主要是 DelayQueue 写入线程，不是 Kafka consumer 线程。连接池大小应至少覆盖 `delay.component.local.thread_num`，如果重处理也在运行，还要把重处理写入线程和其它数据库访问一起算进去。

1. `连接池大小 >= DelayQueue 写入线程数`
    - 影响：每个写入线程准备好一批数据后，通常可以立即拿到数据库连接执行 `batchUpsert`。数据库连接池不会成为主要瓶颈。
    - 业务表现：写入较流畅，处理延迟主要取决于 Parser CPU、`batchUpsert` SQL 执行效率和 TimescaleDB 实际写入能力。

2. `连接池大小 < DelayQueue 写入线程数`
    - 影响：例如 DelayQueue 写入线程数为 15，而连接池大小为 10。当 15 个线程都准备写入数据库时，只有 10 个线程能拿到连接，剩下 5 个线程必须等待连接释放。
    - 业务表现：数据库写入延迟增加，DelayQueue 中的数据可能持续积压。即使继续增加 Kafka consumer 并发或 Parser CPU，也无法突破连接池限制。

- `service_call` 写入 SQL：当前写入使用 `ON CONFLICT (time, span_id) DO UPDATE`，吞吐会受主键冲突检查、更新成本、索引维护、chunk 状态和 WAL 写入影响。
- TimescaleDB 写入能力：磁盘 I/O、WAL、checkpoint、autovacuum、`max_connections`、`work_mem`、`timescaledb.max_background_workers` 等设置都会影响实际写入能力。数据库配置建议放到 [timescaledb-configuration](configuration/timescaledb-configuration.md) 中统一说明。
- 连续聚合和列存压缩后台任务：连续聚合、columnstore 压缩、retention policy、autovacuum 会与 Parser 写入争抢数据库 CPU、I/O、WAL 和锁资源。写入延迟数据较多时，连续聚合需要刷新的时间窗口变大，数据库压力会更明显。






# UI Server 查询非常慢

在数据量非常大时，如果一段时间不查询数据的话，在查询数据量大的服务时，会感觉非常慢。主要原因如下：
- TimescaleDB 的 chunk pruning 问题
- 查询时聚合的数据量非常大

为了解决 TimescaleDB 的 chunk pruning 问题，可以使用下面两个方法：

- 在 client 端连接数据时设置 `preferQueryMode=simple`。
- 开启预热功能

数据量大的问题，请参看 `查询时聚合的数据量非常大` 解决方案。

# 查询时聚合的数据量非常大


## 现象

某些服务在 UI Server 查询时明显变慢，数据库侧看到 SQL 会读取很多数据，甚至单次查询需要扫描几百 MB 或更多。

## 常见原因

- 服务本身流量非常大，在查询时间范围内产生了大量 Span。
- 查询时间范围太大，路由到了小时级或天级聚合表，但仍需要读取大量聚合行。
- 聚合维度基数太高。例如接口名称等（`target_name` 字段） 中包含用户 ID、订单号、Trace ID、随机参数、完整 URL query string 等动态值。
- 查询没有限定主机、端点、组件或目标资源，导致一次查询覆盖了服务下大量维度。

其中最常见的问题是 `target_name` 过于离散。即使底层已经有连续聚合，如果每个 `target_name` 只出现一两次，聚合表也无法把数据有效合并，查询时仍然要处理大量行。

## 如何确认

先确认目标服务在某个时间段内的聚合行数。下面以小时级入口聚合表为例：

```sql
SELECT
  COUNT(*) AS rows,
  COUNT(DISTINCT target_name) AS target_count,
  COUNT(DISTINCT action_name) AS action_count,
  COUNT(DISTINCT host_name) AS host_count
FROM service_call_hour_entry
WHERE service_name = 'your-service-name'
  AND time >= '2026-08-07 00:00:00+08'
  AND time <  '2026-08-08 00:00:00+08';
```

如果 `rows` 和 `target_count` 都很大，通常说明目标资源名称过于分散。

再查看哪些 `target_name` 出现次数很少。低频目标很多时，通常说明目标名称里包含动态参数：

```sql
SELECT
  b.text_value,
  COUNT(*) AS cnt
FROM service_call_hour_entry a
JOIN resource_hash_dictionary b
  ON a.target_name = b.hash_key
WHERE a.service_name = 'your-service-name'
  AND a.time >= '2026-08-07 00:00:00+08'
  AND a.time <  '2026-08-08 00:00:00+08'
GROUP BY b.text_value
HAVING COUNT(*) <= 1
ORDER BY cnt,
  b.text_value;
```


也可以按组件查看数据主要来自哪里：

```sql
SELECT
  system_type,
  component,
  COUNT(*) AS rows,
  COUNT(DISTINCT target_name) AS target_count
FROM service_call_hour_entry
WHERE service_name = 'your-service-name'
  AND time >= '2026-08-07 00:00:00+08'
  AND time <  '2026-08-08 00:00:00+08'
GROUP BY system_type, component
ORDER BY rows DESC;
```

如果某个 HTTP client、JDBC、Redis 或 MQ 组件的 `target_count` 特别高，优先检查这个组件的 `targetName` 解析和替换规则。

## 处理建议

- 缩小查询时间范围。先用较短时间范围确认问题，再逐步扩大。
- 增加筛选条件，例如服务、主机、端点、组件或目标资源，减少一次查询覆盖的数据量。
- 使用字段替换规则归一化动态内容。比如把 URL 中的用户 ID、订单号、IP、随机 query 参数替换成占位符。
- 检查 `parser-component-config.yml` 中 `targetName` 的取值来源。对于 HTTP 请求，优先使用路由模板，不要直接使用完整 URL。对于 SQL，优先使用模板化 SQL，不要包含实际参数值。
- 如果某些服务流量确实过大，且不需要完整采集，可以在 OpenTelemetry Collector 或业务埋点侧设置采样率。

采样会降低数据完整性。启用前需要确认它对错误率、P95/P99、拓扑和上下游分析的影响是否可接受。


# 连续聚合刷新时间范围

Prism APM 使用 TimescaleDB Continuous Aggregate 保存不同粒度的指标数据。每条连续聚合策略都有三个关键时间参数：

- `start_offset`：从当前时间往前推多远，作为本次刷新窗口的开始。
- `end_offset`：从当前时间往前推多远，作为本次刷新窗口的结束。
- `schedule_interval`：多久执行一次刷新。

也就是说，一次刷新大致会处理下面这个时间范围：

```text
[当前时间 - start_offset, 当前时间 - end_offset)
```

`end_offset` 越大，说明越靠近当前时间的数据越晚进入该聚合表。这样做是为了等待迟到数据，避免刚写入的数据还不稳定时就被上层聚合使用。

## 服务指标和依赖指标

下面这些规则来自 `service_call_*_entry` 和 `service_call_*_dependency` 的连续聚合定义。

| 聚合粒度 | 表                                                              | 来源             | 刷新频率    | 每次刷新范围                 |
| ---- | -------------------------------------------------------------- | -------------- | ------- | ---------------------- |
| 1 分钟 | `service_call_minute_entry` / `service_call_minute_dependency` | `service_call` | 每 1 分钟  | 当前时间前 10 分钟到当前时间       |
| 5 分钟 | `service_call_5min_entry` / `service_call_5min_dependency`     | 分钟级聚合          | 每 1 分钟  | 当前时间前 30 分钟到当前时间前 5 分钟 |
| 1 小时 | `service_call_hour_entry` / `service_call_hour_dependency`     | 5 分钟聚合         | 每 30 分钟 | 当前时间前 6 小时到当前时间前 3 小时  |
| 1 天  | `service_call_day_entry` / `service_call_day_dependency`       | 小时级聚合          | 每 1 天   | 当前时间前 3 天到当前时间前 1 天    |

例如，现在是 `12:00`，小时级服务指标刷新时只会处理大约 `06:00` 到 `09:00` 的数据。`09:00` 之后的数据需要等待后续刷新进入小时级聚合表。

## 历史合并聚合

分钟、5 分钟和小时级服务指标还有对应的 `_merged` 表，用于把较早的数据归档到更适合历史查询的聚合表中。

| 表                                                                            | 来源     | 刷新频率  | 每次刷新范围                 |
| ---------------------------------------------------------------------------- | ------ | ----- | ---------------------- |
| `service_call_minute_entry_merged` / `service_call_minute_dependency_merged` | 分钟级聚合  | 每 1 天 | 当前时间前 27 小时到当前时间前 2 小时 |
| `service_call_5min_entry_merged` / `service_call_5min_dependency_merged`     | 5 分钟聚合 | 每 1 天 | 当前时间前 27 小时到当前时间前 2 小时 |
| `service_call_hour_entry_merged` / `service_call_hour_dependency_merged`     | 小时级聚合  | 每 1 天 | 当前时间前 27 小时到当前时间前 2 小时 |

这些表的策略设置了 `initial_start`，按 DDL 注释会在每天约 `02:00` 执行。它们主要用于历史数据合并，不代表页面一定直接查询这些物化表；UI Server 可能通过统一视图或表路由规则选择实际读取对象。

## 服务实例和拓扑指标

服务实例列表和拓扑关系也使用连续聚合。它们的分钟级、5 分钟级和天级刷新窗口与服务指标类似，但小时级 `end_offset` 是 `1 hour`，比服务指标更靠近当前时间。

| 聚合对象                          | 聚合粒度 | 刷新频率    | 每次刷新范围                 |
| ----------------------------- | ---- | ------- | ---------------------- |
| `service_host_list_*`         | 1 分钟 | 每 1 分钟  | 当前时间前 10 分钟到当前时间       |
| `service_host_list_*`         | 5 分钟 | 每 1 分钟  | 当前时间前 30 分钟到当前时间前 5 分钟 |
| `service_host_list_*`         | 1 小时 | 每 30 分钟 | 当前时间前 6 小时到当前时间前 1 小时  |
| `service_host_list_*`         | 1 天  | 每 1 天   | 当前时间前 3 天到当前时间前 1 天    |
| `service_topology_relation_*` | 1 分钟 | 每 1 分钟  | 当前时间前 10 分钟到当前时间       |
| `service_topology_relation_*` | 5 分钟 | 每 1 分钟  | 当前时间前 30 分钟到当前时间前 5 分钟 |
| `service_topology_relation_*` | 1 小时 | 每 30 分钟 | 当前时间前 6 小时到当前时间前 1 小时  |
| `service_topology_relation_*` | 1 天  | 每 1 天   | 当前时间前 3 天到当前时间前 1 天    |

# 升级后页面显示不正常

由于浏览器会缓存页面资源, 系统如果升级时涉及前端资源, 有可能系统前端资源显示异常, 此时强制刷新页面即可解决.

强制刷新方式 (在APM系统页面执行):
- Windows 平台快捷键 `Ctrl + F5`  
- Mac 平台快捷键 `Cmd + F5`

