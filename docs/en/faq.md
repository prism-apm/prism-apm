---
created: 2026-08-04 17:33:12
modified: 2026-08-11 16:25:33
---

# Page Opens but Has No Data

## Symptoms

Prism APM UI opens normally and the user can log in, but the service overview, service list, endpoint details, database details, or topology pages show no data.

This usually means the frontend static page is reachable, but there is a problem in the query path, time range, permission scope, or aggregate data.

## Possible Causes

- There is no `service_call` detail data in the current query time range.
- Parser has written detail data, but minute-level, 5-minute, hourly, or daily continuous aggregates do not yet have queryable data.
- The selected time zone on the page is incorrect, causing UI Server to query the wrong UTC time range.
- The current user does not have access to the project containing the target service.
- The read database connected by UI Server is not the database that Parser is writing to.

## Troubleshooting

First confirm whether UI Server query APIs return empty data. In browser developer tools, check requests such as `/api/apm/common/service-object-list` and `/api/apm/service/overview` in the Network panel:

- If the API returns `401`, troubleshoot login state first.
- If the API returns `403`, troubleshoot role permissions and project permissions first.
- If the API returns `status=200` but `data` is empty, continue checking data and query conditions.
- If the API returns `status=500` or `err` is not empty, check the concrete exception in UI Server logs.

If the checks above are fine, continue checking the data below.

- Check whether the detail table has data in the current time range. If `count(*)` is `0`, Parser has not written queryable data yet; continue with \"Parser Cannot Consume or No Data Is Written\".
- If `service_call` has data, check whether service names have been written.
- If the detail table has data but the page is still empty, check whether the minute-level aggregate table (`service_call_minute_entry`) has data.
- Then check whether routing rules exist in `UI-Server`.

# No Current Data, but Older Data Exists

## Symptoms

The page can query older data, but recent data is missing. For example, the last 5 minutes, 15 minutes, or 1 hour are empty, while earlier time ranges still show service, endpoint, or topology data.

This differs from \"Page Opens but Has No Data\": historical data exists, so UI Server, permissions, table routing, and basic query paths are probably working. The problem is more likely in the realtime write path.

## Possible Causes

- Kafka messages are backlogged, causing data to be written after the aggregate window has passed. For example, minute-level aggregation covers recent 10 minutes of data. Because of backlog, data may be processed only after it has exceeded the aggregation range, so new aggregate data is not produced.
- The `prism-apm-parser` service has stopped, failed to restart, or cannot continue consuming Kafka messages.
- Parser is running, but consumption is slower than upstream writes, so Kafka messages are backlogged and the latest data has not reached the database.

## Troubleshooting

If the last few minutes have no data but older data exists, first check whether Parser is still processing normally. Check the Parser main log:

```text
logs/server.log
```

Then check Parser statistics:

```text
logs/stats.log
```

Focus on the per-minute output:

- Whether `saved span` is still increasing.
- Whether `timeout span` is increasing quickly.
- Whether `delay queue size` keeps rising.
- Whether `reprocessing delay queue size` is abnormally high.

If `saved span` no longer increases, Parser usually is no longer successfully saving Spans. Continue checking parse failure logs:

```text
logs/unparsed_span.log
logs/unparsed_service.log
```

If `saved span` is still increasing but `max(time)` is clearly behind current time, Parser is probably still processing backlog data, or upstream is sending Traces with older timestamps.

If using read/write split databases, run `select max(time) from service_call;` on both read and write databases. If the write database is up to date but the read database lags, the problem is read-replica synchronization delay.

# Parser Cannot Consume or No Data Is Written

## Symptoms

Upstream has sent Trace data, but Prism APM pages have no data and the `service_call` table has no new records.

## Possible Causes

- The Kafka topic consumed by Parser differs from the topic written by upstream.
- Parser uses the default consumer group `apm-span-parser`, and the default `auto-offset-reset=latest` means historical messages written before startup are not automatically consumed by a new consumer group.
- Kafka messages are not OpenTelemetry Trace JSON supported by Parser.
- Parser can consume messages, but Span parsing rules cannot generate a complete `SpanInfo`.
- Parser fails to connect to the database, or an exception occurs while writing `service_call`.

## Troubleshooting

First check Parser topic configuration:

```properties
parser.kafka_topic=opentelemetry-trace
parser.reprocessing_kafka_topic=reprocessing-opentelemetry-trace
```

Confirm that the upstream topic matches `parser.kafka_topic`.

Then check the Parser main log:

```text
logs/server.log
```

If Parser consumes normally but does not write data, continue checking parse failure logs:

```text
logs/unparsed_span.log
logs/unparsed_service.log
```

Check per-minute statistics to confirm whether DelayQueue, cache, and Span processing counts are changing:

```text
logs/stats.log
```

Finally, check whether the detail table has new data:

```sql
select max(time), count(*)
from service_call;
```

# Topology Is Incomplete

## Symptoms

The topology page shows some nodes, but missing call edges, upstream, downstream, or service relationships are clearly incomplete.

## Possible Causes

- Spans from the same Trace did not enter the same Kafka partition, so parent and child Spans were processed by different Parser instances.
- Child Spans arrived before parent Spans, and the parent Span arrived after the wait threshold.
- `cache.timeout_s` is less than or close to `dependency.timeoutThreshold`, so parent Spans are evicted before correlation.
- Parser did not parse key fields required by caller or callee.
- Topology aggregate tables have not refreshed yet.

## Troubleshooting

If using OpenTelemetry Collector, confirm whether the Kafka exporter has `partition_traces_by_id: true`.

Check Parser cache and DelayQueue statistics:

```text
logs/stats.log
logs/cache.log
```

Focus on:

- Whether DelayQueue keeps accumulating.
- Whether cache expiration or eviction is clearly high.
- Whether Span processing count grows normally.

Check whether cache and parent-child wait-time configuration satisfies:

```text
cache.timeout_s * 1000 > dependency.timeoutThreshold
```

Check whether topology aggregate tables have data:

```sql
select min(time), max(time), count(*)
from service_topology_relation_minute;
```

## Fixes

Make sure the upstream Kafka exporter uses `traceId` as the message key and partitions by trace id.

If cache expiration or eviction is clearly high, increase `cache.timeout_s` appropriately and confirm that it is greater than `dependency.timeoutThreshold`.

If topology tables have no data but `service_call` has data, wait for continuous aggregate refresh, or check topology routing rules and aggregate objects.

# Login Failed

## Symptoms

After opening the login page, username/password login fails.

## Possible Causes

- The user does not exist or the password is incorrect.
- The user is disabled.
- The database connected by UI Server has no initialized user data.
- `JWT_SECRET` is too short, causing UI Server to fail to generate tokens normally.

## Troubleshooting

The default administrator account comes from initialization data:

```text
username: admin
password: apm-admin
```

Check whether the user exists and is enabled:

```sql
select id, username, enabled
from users
where username = 'admin';
```

If the login API returns `401`, UI Server is reachable but authentication failed.

## Fixes

Confirm that UI Server connects to a database containing initialized `users`, `roles`, and `role_user` data.

If the user is disabled, enable the user through an administrator account or database maintenance process.

If logs show JWT secret related errors, set a sufficiently long `JWT_SECRET`. HS256 signing secrets should be at least 32 bytes.

# API Returns 403 or Permission Denied

## Symptoms

The user is logged in, but accessing some pages or APIs returns `403`, and the page reports insufficient permissions.

## Possible Causes

- The user has no role.
- The user's role does not have the corresponding URL permission.
- The ordinary user has not been added to a project group containing the target project.
- Permissions were just changed, and the permission cache has not refreshed yet.

## Troubleshooting

Check user-role relationships:

```sql
select u.username, r.name
from users u
join role_user ru on ru.user_id = u.id
join roles r on r.id = ru.role_id
where u.username = '<username>';
```

Check role-permission relationships:

```sql
select r.name as role_name, p.name as permission_name, p.url
from roles r
join role_permission rp on rp.role_id = r.id
join permissions p on p.id = rp.permission_id
where r.name = '<role-name>';
```

Check user-project-group relationships:

```sql
select u.username, pg.name as project_group_name
from users u
join user_project_group upg on upg.user_id = u.id
join project_groups pg on pg.id = upg.project_group_id
where u.username = '<username>';
```

## Fixes

Assign the correct role to the user and assign the corresponding URL permissions to the role.

If the user needs to view business data, also add the user to a project group containing the target project.

After permission changes, wait for the permission cache to refresh, then ask the user to access again or log in again.

# Some Spans Are Not Parsed

## Symptoms

Parser runs normally and some data is written to `service_call`, but certain services, components, endpoints, or database accesses do not appear on the page.

## Possible Causes

- The current component does not match any component rule in `parser-component-config.yml`.
- Trace attribute names differ from the attribute paths in configuration.
- Required fields did not resolve to valid values.
- The service is configured as not parsed in `service_parse_config`.
- A field replacement rule changes the target name into an unexpected value.

## Troubleshooting

Check parse failure logs:

```text
logs/unparsed_span.log
logs/unparsed_service.log
```

Check component parsing configuration:

```text
prism-apm-parser/src/main/resources/parser-component-config.yml
```

To generate a valid `SpanInfo`, these fields must resolve to valid values:

- `serviceName`
- `hostName`
- `spanKind`
- `actionName`
- `targetName`
- `component`
- `systemType`

## Fixes

Add component rules for the current instrumentation scope or Span attributes.

If only attribute names changed, first append new candidate attribute paths to `vals` for the corresponding field.

If some fields have no source, use `defaultVal` to provide a stable default.

After adjusting rules, restart Parser and resend Traces, or use reprocessing to fill data.

# Target Names Are Too Scattered or Contain Dynamic Parameters

## Symptoms

The page shows an abnormally large number of endpoints, database statements, Redis keys, or external call targets. The same logical endpoint is split into many different targets.

## Possible Causes

- `target_name` contains order IDs, user IDs, pagination parameters, full URL query strings, or other dynamic content.
- `field_content_replacer_rule` does not have a replacement rule for the corresponding component.
- The replacement rule is disabled.
- The replacement rule's `service_name`, `component`, or `field_name` does not match actual data.

## Troubleshooting

First check target-name distribution:

```sql
select component, span_kind, target_name, count(*)
from service_call
group by component, span_kind, target_name
order by count(*) desc
limit 50;
```

Check replacement rules:

```sql
select service_name, component, span_kind, field_name, match_type, search_content, replacement, enabled
from field_content_replacer_rule
order by service_name, component, span_kind, field_name;
```

## Fixes

Add replacement rules for dynamic URLs, SQL, Redis keys, or message targets, normalizing variable parts into stable placeholders.

Confirm that the rule's `field_name` points to the field to process, such as `targetName` or `remoteAddr`.

Confirm `enabled=true`, then let Parser reload configuration or restart Parser.

# How to Use Parser `stats.log`

## Symptoms

When you need to determine whether Parser is still processing data, whether backlog exists, whether Span parent-child correlation is abnormal, or whether cache has become a bottleneck, check `logs/stats.log` first.

`stats.log` is written once per minute by Parser. It does not record details of individual Traces; it is used to observe overall Parser processing state.

## Log Content

`stats.log` writes a set of statistics each minute, usually including:

```text
outputStats started
ehcache stats. exists:..., puts:..., gets:..., removals:..., hits:..., misses:..., Evictions:..., Expirations:..., OccupiedByteSize:...
delay queue size:...,reprocessing delay queue size:...
span stats. saved span: ..., timeout span: ...
outputStats end
```

Where:

- `saved span`: accumulated number of Span records saved successfully by Parser.
- `timeout span`: accumulated number of Spans saved after parent-Span wait timeout.
- `delay queue size`: number of Spans waiting in the normal processing queue.
- `reprocessing delay queue size`: number of Spans waiting in the reprocessing queue.
- `exists`: number of Spans currently kept in `spanCache`.
- `puts`: accumulated writes to `spanCache`.
- `gets`: accumulated reads from `spanCache`.
- `hits`: accumulated cache read hits.
- `misses`: accumulated cache read misses.
- `Evictions`: accumulated evictions due to cache capacity limits.
- `Expirations`: accumulated expirations due to exceeding cache retention time.
- `OccupiedByteSize`: estimated bytes occupied by the Ehcache OnHeap layer.

Most values are accumulated since Parser startup. When troubleshooting, do not only look at the absolute value of one line. Observe several minutes continuously and check whether values are increasing and whether the growth rate is abnormal.

If `saved span` is not increasing, first confirm that Parser is still running, then check `logs/server.log` for Kafka, parsing, or database write exceptions.

If `delay queue size` keeps rising, the Parser processing path cannot keep up with input speed. Continue checking `server.log`, database write duration, Kafka backlog, Parser CPU/memory/GC, and database write performance to identify the bottleneck.

---

# Kafka Message Size Settings

Some Traces can contain many Spans, so message size can become very large. Related settings:

- `message.max.bytes`: a static broker-level parameter that affects all topics. It requires modifying `server.properties` and restarting the Kafka cluster.
- `max.message.bytes`: a dynamic topic-level parameter that affects only the specified topic. It takes effect immediately after modification and does not require restarting Kafka.
- `replica.fetch.max.bytes`: allows large messages to replicate normally to broker replicas.
- `max.partition.fetch.bytes`: maximum message size that a consumer can consume.

# Throughput Bottlenecks

Parser's processing chain can be divided into three sections: Kafka input, Parser internal processing, and database write output. When troubleshooting throughput, do not look at a single parameter only. Combine `delay queue size` and `saved span` growth rate in `logs/stats.log`, Kafka consumer lag, Parser CPU/memory/GC, and database write duration.

## Kafka Input

- Topic partition count: determines the maximum number of consumer threads in the same consumer group that can consume in parallel. When partition count is smaller than Parser consumer concurrency, extra consumer threads do not improve throughput.
- Parser instance count: adding instances can improve overall consumption capacity, but total concurrency is still limited by partition count. When scaling, also ensure the same `traceId` still enters the same partition; otherwise parent-child Span correlation may be incomplete.
- Kafka consumer concurrency: `spring.kafka.listener.concurrency` determines Kafka listener concurrency in a single Parser instance.
- Single poll size: `spring.kafka.consumer.max-poll-records` determines the maximum number of messages consumed in one batch. Too small increases poll and batch-processing overhead; too large increases single-batch parse duration, memory usage, and offset acknowledgment delay.
- Single message size: if one Trace contains many Spans, confirm message-size limits for Kafka broker, topic, replica, and consumer. See the previous section about Kafka message size configuration.

## Parser Internal Processing

- Trace size and Span count: Parser first deserializes OpenTelemetry Trace JSON, then traverses `resourceSpans`, `scopeSpans`, and `spans`. The more Spans in one Trace, the higher the parsing cost of one Kafka message.
- Parsing-rule complexity: Parser matches Spans against component rules in `parser-component-config.yml`, then reads fields according to outputs. More component rules, more complex regular expressions, and more field paths increase CPU cost.
- Field replacement rules: regex replacement in `field_content_replacer_rule` runs during field parsing. Many rules or complex regexes also affect parsing throughput.
- Ehcache capacity and JVM memory: Spans are written to local cache before waiting for parent Spans. Bad settings for `cache.heap_percent`, `cache.entry_size_bytes`, or `cache.timeout_s` can cause eviction, expiration, memory pressure, or frequent GC.
- DelayQueue thread count: `delay.component.local.thread_num` determines the number of threads that take Spans from DelayQueue and write to the database. See [parser_configuration#delay-queue-configuration](configuration/parser_configuration.md#delay-queue-configuration).
- DelayQueue batch parameters: `delay.component.local.batch_size` determines the maximum Spans per batch, and `delay.component.local.batch_timeout_ms` determines how long to wait when a batch is not full. Too small a batch increases SQL count; too large a batch can increase single-write duration and connection occupancy.
- Parent Span wait time: `delayMillis` and `dependency.timeoutThreshold` affect how many times a Span waits repeatedly in DelayQueue. Long waits increase processing delay and cache usage; short waits may make call relationships incomplete.

## Database Write Output

- Database connection pool size: `batchUpsert` is mainly executed by DelayQueue write threads, not Kafka consumer threads. The connection pool should at least cover `delay.component.local.thread_num`; if reprocessing is also running, include reprocessing write threads and other database access as well.

1. `Connection pool size >= DelayQueue write thread count`
   - Impact: after each write thread prepares a batch, it can usually obtain a database connection immediately and execute `batchUpsert`. The database connection pool is not the main bottleneck.
   - Business behavior: writes are relatively smooth. Processing latency mainly depends on Parser CPU, `batchUpsert` SQL efficiency, and actual TimescaleDB write capacity.

2. `Connection pool size < DelayQueue write thread count`
   - Impact: for example, if DelayQueue write threads are 15 but the connection pool size is 10, only 10 threads can obtain connections when all 15 are ready to write; the remaining 5 must wait for connections to be released.
   - Business behavior: database write latency increases and data may keep accumulating in DelayQueue. Increasing Kafka consumer concurrency or Parser CPU cannot break through the connection pool limit.

- `service_call` write SQL: current writes use `ON CONFLICT (time, span_id) DO UPDATE`. Throughput is affected by primary-key conflict checks, update cost, index maintenance, chunk state, and WAL writes.
- TimescaleDB write capacity: disk I/O, WAL, checkpoints, autovacuum, `max_connections`, `work_mem`, `timescaledb.max_background_workers`, and similar settings all affect actual write capacity. Database configuration recommendations should be kept in [timescaledb-configuration](configuration/timescaledb-configuration.md).
- Continuous aggregates and columnstore compression background jobs: continuous aggregates, columnstore compression, retention policy, and autovacuum compete with Parser writes for database CPU, I/O, WAL, and locks. When there is more late-written data, continuous aggregates need a larger refresh window and database pressure becomes more obvious.

# UI Server Queries Are Very Slow

When data volume is very large, queries for high-volume services can feel very slow after the data has not been queried for a period of time. Main reasons:

- TimescaleDB chunk pruning issues.
- Very large amounts of data are aggregated during query.

To address TimescaleDB chunk pruning issues, use these two methods:

- Set `preferQueryMode=simple` on the client connection.
- Enable prewarming.

For large aggregation volume, see the solution in \"Query Aggregates Too Much Data\".

# Query Aggregates Too Much Data

## Symptoms

Some services become clearly slow when queried by UI Server. On the database side, SQL reads a large amount of data, and a single query may scan hundreds of MB or more.

## Common Causes

- The service itself has high traffic and generates many Spans in the query time range.
- The query time range is too large and routes to hourly or daily aggregate tables, but still needs to read many aggregate rows.
- Aggregate dimension cardinality is too high. For example, endpoint names or the `target_name` field contain user IDs, order IDs, Trace IDs, random parameters, full URL query strings, or other dynamic values.
- The query does not limit host, endpoint, component, or target resource, causing one query to cover many dimensions under the service.

The most common issue is that `target_name` is too scattered. Even if continuous aggregates exist underneath, if each `target_name` only appears once or twice, aggregate tables cannot merge data effectively and queries still process many rows.

## How to Confirm

First confirm the aggregate row count for the target service in a time range. The following example uses the hourly entry aggregate table:

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

If both `rows` and `target_count` are large, target resource names are usually too scattered.

Then check which `target_name` values appear rarely. Many low-frequency targets usually indicate dynamic parameters in target names:

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

You can also check by component to see where most data comes from:

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

If an HTTP client, JDBC, Redis, or MQ component has a particularly high `target_count`, first check the `targetName` parsing and replacement rules for that component.

## Suggestions

- Narrow the query time range. Confirm the problem with a shorter range first, then gradually expand.
- Add filters such as service, host, endpoint, component, or target resource to reduce the amount of data covered by one query.
- Use field replacement rules to normalize dynamic content. For example, replace user IDs, order IDs, IPs, or random query parameters in URLs with placeholders.
- Check the value source of `targetName` in `parser-component-config.yml`. For HTTP requests, prefer route templates and avoid full URLs. For SQL, prefer parameterized SQL templates and avoid actual parameter values.
- If some services truly have very high traffic and complete collection is not required, set a sampling rate in OpenTelemetry Collector or application instrumentation.

Sampling reduces data completeness. Before enabling it, confirm whether the impact on error rate, P95/P99, topology, and upstream/downstream analysis is acceptable.

# Continuous Aggregate Refresh Time Ranges

Prism APM uses TimescaleDB Continuous Aggregate to store metrics at different granularities. Each continuous aggregate policy has three key time parameters:

- `start_offset`: how far back from current time the refresh window starts.
- `end_offset`: how far back from current time the refresh window ends.
- `schedule_interval`: how often refresh runs.

In other words, one refresh roughly processes this time range:

```text
[current time - start_offset, current time - end_offset)
```

The larger `end_offset` is, the later data close to current time enters the aggregate table. This waits for late data and avoids using newly written unstable data in upper-level aggregates.

## Service Metrics and Dependency Metrics

The following rules come from continuous aggregate definitions for `service_call_*_entry` and `service_call_*_dependency`.

| Granularity | Table | Source | Refresh frequency | Refresh range |
| --- | --- | --- | --- | --- |
| 1 minute | `service_call_minute_entry` / `service_call_minute_dependency` | `service_call` | Every 1 minute | From 10 minutes before current time to current time |
| 5 minutes | `service_call_5min_entry` / `service_call_5min_dependency` | Minute-level aggregate | Every 1 minute | From 30 minutes before current time to 5 minutes before current time |
| 1 hour | `service_call_hour_entry` / `service_call_hour_dependency` | 5-minute aggregate | Every 30 minutes | From 6 hours before current time to 3 hours before current time |
| 1 day | `service_call_day_entry` / `service_call_day_dependency` | Hourly aggregate | Every 1 day | From 3 days before current time to 1 day before current time |

For example, if the current time is `12:00`, an hourly service metrics refresh only processes roughly `06:00` to `09:00`. Data after `09:00` waits for later refreshes before entering hourly aggregate tables.

## Historical Merged Aggregates

Minute, 5-minute, and hourly service metrics also have corresponding `_merged` tables, used to archive older data into aggregate tables more suitable for historical queries.

| Table | Source | Refresh frequency | Refresh range |
| --- | --- | --- | --- |
| `service_call_minute_entry_merged` / `service_call_minute_dependency_merged` | Minute-level aggregate | Every 1 day | From 27 hours before current time to 2 hours before current time |
| `service_call_5min_entry_merged` / `service_call_5min_dependency_merged` | 5-minute aggregate | Every 1 day | From 27 hours before current time to 2 hours before current time |
| `service_call_hour_entry_merged` / `service_call_hour_dependency_merged` | Hourly aggregate | Every 1 day | From 27 hours before current time to 2 hours before current time |

These table policies set `initial_start`; according to DDL comments, they run at about `02:00` every day. They are mainly used for historical data merging and do not mean pages must query these materialized tables directly. UI Server may choose the actual read object through unified views or table routing rules.

## Service Instances and Topology Metrics

Service instance lists and topology relationships also use continuous aggregates. Their minute-level, 5-minute, and daily refresh windows are similar to service metrics, but hourly `end_offset` is `1 hour`, closer to current time than service metrics.

| Aggregate object | Granularity | Refresh frequency | Refresh range |
| --- | --- | --- | --- |
| `service_host_list_*` | 1 minute | Every 1 minute | From 10 minutes before current time to current time |
| `service_host_list_*` | 5 minutes | Every 1 minute | From 30 minutes before current time to 5 minutes before current time |
| `service_host_list_*` | 1 hour | Every 30 minutes | From 6 hours before current time to 1 hour before current time |
| `service_host_list_*` | 1 day | Every 1 day | From 3 days before current time to 1 day before current time |
| `service_topology_relation_*` | 1 minute | Every 1 minute | From 10 minutes before current time to current time |
| `service_topology_relation_*` | 5 minutes | Every 1 minute | From 30 minutes before current time to 5 minutes before current time |
| `service_topology_relation_*` | 1 hour | Every 30 minutes | From 6 hours before current time to 1 hour before current time |
| `service_topology_relation_*` | 1 day | Every 1 day | From 3 days before current time to 1 day before current time |

# Page Displays Incorrectly After Upgrade

Because browsers cache page assets, if an upgrade changes frontend assets, the APM frontend may display incorrectly. A hard refresh fixes this.

Hard refresh shortcuts on the APM page:

- Windows: `Ctrl + F5`
- Mac: `Cmd + F5`
