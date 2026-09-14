---
created: 2026-07-29 16:46:48
modified: 2026-08-03 17:12:16
---

# Parser Configuration

This document describes runtime configuration for `prism-apm-parser`. Parser consumes OpenTelemetry Trace JSON from Kafka, parses it, and writes the result to PostgreSQL/TimescaleDB.

To understand the overall Parser processing flow, see:

- [Parser architecture](../architecture/parser_architecture.md)

# Minimum Required Configuration

## Environment Variables

| Environment variable | Example | Description |
| --- | --- | --- |
| `DB_ENDPOINT` | `postgres:5432` | PostgreSQL/TimescaleDB endpoint, in `host:port` format. |
| `DB_NAME` | `apm` | Database name. |
| `DB_USERNAME` | `postgres` | Database username. |
| `DB_PASSWORD` | `your-db-password` | Database password. |
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | Kafka broker address. |

## Example

```bash
export DB_ENDPOINT=localhost:5432
export DB_NAME=apm
export DB_USERNAME=postgres
export DB_PASSWORD=your-db-password
export KAFKA_BOOTSTRAP_SERVERS=localhost:9094
```

## Internal Mapping

The application uses these configuration templates internally to generate final connection settings:

```properties
spring.datasource.url=jdbc:postgresql://${DB_ENDPOINT:localhost:5432}/${DB_NAME:apm}?useUnicode=true&characterEncoding=UTF-8&rewriteBatchedStatements=true
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9094}
```

## Default Configuration

`server.port`, `parser.kafka_topic`, and `parser.reprocessing_kafka_topic` all have defaults. Override them only when the port conflicts, the upstream Trace topic name is not the default, or a non-default reprocessing topic is required.

# Kafka Configuration

Parser creates consumers and producers through Spring Kafka auto-configuration. Normally, only `KAFKA_BOOTSTRAP_SERVERS` is required; the project maps it to `spring.kafka.bootstrap-servers`.

## Built-In Behavior

| Behavior | Default | Description |
| --- | --- | --- |
| Consumer group | `apm-span-parser` | Default Parser consumer group. |
| Offset commit | Manual commit | Messages are acknowledged after Parser finishes batch processing. |
| Consumption mode | Batch consumption | Each poll receives a batch of Trace messages. |
| Start point when no offset exists | `latest` | A new consumer group starts from the latest messages. |

## Optional Tuning Parameters

These parameters usually do not need to be configured. Override them only when throughput, message size, or resource usage is not as expected:

| Configuration | Default | Description |
| --- | --- | --- |
| `spring.kafka.consumer.max-poll-records` | `50` | Maximum number of messages in one poll. |
| `spring.kafka.listener.concurrency` | `4` | Kafka listener concurrency. |
| `spring.kafka.producer.properties.max.request.size` | `10485760` | Maximum producer request size, 10 MB by default. |
| `spring.kafka.producer.buffer-memory` | `67108864` | Producer buffer memory, 64 MB by default. |

## Topic Configuration

| Configuration | Default | Description |
| --- | --- | --- |
| `parser.kafka_topic` | `opentelemetry-trace` | Trace topic consumed by the normal Parser flow. |
| `parser.reprocessing_kafka_topic` | `reprocessing-opentelemetry-trace` | Trace topic consumed by the reprocessing flow. |

`parser.reprocessing_kafka_topic` does not need to be explicitly set for normal Trace ingestion; the default is used when it is not configured. Override it only when you need reprocessing or the reprocessing topic name is not the default.

## Notes

- Use `traceId` as the Kafka message key. If you use OpenTelemetry Collector, set `partition_traces_by_id: true` for the Kafka exporter.
- The topic partition count should be no smaller than Parser consumer concurrency.

# Kafka SASL Configuration

This configuration is used when Kafka requires authentication. If Kafka has SASL enabled, enable `application-kafka-sasl.properties`:

## Enable

```bash
java -jar prism-apm-parser/target/prism-apm-parser-*.jar \
  --spring.profiles.active=kafka-sasl
```

## Environment Variables

| Environment variable | Default | Description |
| --- | --- | --- |
| `KAFKA_SECURITY_PROTOCOL` | `SASL_PLAINTEXT` | Kafka security protocol. |
| `KAFKA_SASL_MECHANISM` | `PLAIN` | Kafka SASL mechanism. |
| `KAFKA_USERNAME` | `admin` | Kafka username. |
| `KAFKA_PASSWORD` | `change-me` | Kafka password. Must be replaced in production. |

After the `kafka-sasl` profile is enabled, the project maps these environment variables to Spring Kafka security settings and automatically assembles the JAAS configuration.

If Kafka uses PLAINTEXT, do not enable the `kafka-sasl` profile.

# Cache Configuration

Parser uses local Ehcache to temporarily store Spans while waiting for parent Spans to arrive and generate call relationships. Spans from the same Trace may arrive out of order in Kafka. Parser writes parsed Spans into cache first, then a delay queue takes them by `spanId` and tries to find their parent Span.

## Common Configuration

| Configuration | Default | Description |
| --- | --- | --- |
| `cache.timeout_s` | `600` | How long a Span remains in cache, in seconds. |
| `cache.heap_percent` | `25` | Percentage of JVM max heap available to `spanCache`. |
| `cache.entry_size_bytes` | `1000` | Estimated size of one cached Span entry, in bytes. |
| `resource.hash.cache.size_entries` | `100000` | Maximum entries in the target resource hash cache. |

## Local Caches

Parser has two local caches:

| Cache | Purpose | Capacity control |
| --- | --- | --- |
| `spanCache` | Stores Span JSON while waiting for parent Spans and generating relationships. | Maximum entries are calculated from `cache.heap_percent` and `cache.entry_size_bytes`. |
| `resource_hash_cache` | Stores mappings from target resource name to hash UUID to reduce repeated computation. | Maximum entries are controlled by `resource.hash.cache.size_entries`. |

## `resource_hash_cache` Memory Estimate

The default maximum entry count for `resource_hash_cache` is `100000`. Each cached value is a UUID, and the UUID itself needs at least 16 bytes, so the theoretical lower bound for UUID values alone is:

```text
100000 * 16 B = 1,600,000 B ≈ 1.5 MiB
```

Actual memory usage is higher because resource-name string keys, Java object overhead, and Ehcache entry-management overhead are also stored. Longer resource names consume more memory. For example, if the average resource name is roughly 100 bytes, the raw key and UUID content alone is about:

```text
100000 * (100 B + 16 B) ≈ 11 MiB
```

After object and cache-management overhead, the default `resource.hash.cache.size_entries=100000` should usually be budgeted as several MB to tens of MB of heap memory.

## `spanCache` Capacity Estimate

The maximum entry count of `spanCache` is not configured directly; it is estimated from the JVM max heap:

```text
spanCache maximum entries = JVM -Xmx * cache.heap_percent / 100 / cache.entry_size_bytes
```

For example, when Parser uses `-Xmx2g`, `cache.heap_percent=25`, and `cache.entry_size_bytes=1000`:

```text
2 GB * 25% / 1000 B ≈ 536,870 entries
```

This value is an estimate. Ehcache limits the cache by entry count and does not precisely limit memory by bytes. The smaller `cache.entry_size_bytes` is, the more entries are allowed and the higher the actual memory risk.

## Cache Retention Time

`cache.timeout_s` determines the longest time a Span can wait in cache. It should be configured together with the delay queue's parent-Span wait time:

```text
cache.timeout_s * 1000 > dependency.timeoutThreshold
```

If `cache.timeout_s` is too short, a child Span or parent Span may expire from cache before the parent arrives, resulting in incomplete call relationships.

If `cache.timeout_s` is longer, out-of-order Spans are more likely to be correlated correctly, but more data remains in cache and memory usage increases.

## Tuning Suggestions

| Scenario | Suggestion |
| --- | --- |
| Many cache evictions or expirations in `stats.log`, and incomplete call relationships | Increase `cache.timeout_s` appropriately and confirm that it is greater than `dependency.timeoutThreshold`. |
| High JVM memory pressure or frequent GC | Lower `cache.heap_percent`, increase JVM `-Xmx`, or check whether Trace ingestion exceeds the current instance capacity. |
| `spanCache` entry count is clearly insufficient | Increase `cache.heap_percent`, or increase `-Xmx` appropriately. |
| A single Span is clearly larger than the default estimate | Increase `cache.entry_size_bytes` so the maximum entry estimate is more conservative. |
| Many target resource names and obvious repeated hash computation | Increase `resource.hash.cache.size_entries`. |

In production, start with the defaults and observe `logs/stats.log` and `logs/cache.log`. If DelayQueue backlog, cache misses, expirations, or evictions are clearly high, tune gradually based on Trace ingest volume and JVM memory.

# Delay Queue Configuration

Span arrival order in Kafka is unstable; child Spans may arrive before parent Spans. Parser uses a local `DelayQueue` to wait for parent Spans and then generate service call relationships.

## Common Configuration

| Configuration | Default | Description |
| --- | --- | --- |
| `delay.component.local.thread_num` | `4` | Consumer thread count for the normal DelayQueue. |
| `delay.component.local.batch_size` | `10` | Maximum Spans per batch. |
| `delay.component.local.batch_timeout_ms` | `1000` | Maximum wait time, in milliseconds, when the batch cannot reach `delay.component.local.batch_size`. |
| `delayMillis` | `3000` | Delay interval before retrying a child Span when the parent Span has not arrived, in milliseconds. |
| `dependency.timeoutThreshold` | `60000` | Maximum wait time for the parent Span, in milliseconds. |
| `executor.service.shutdown.timeout_ms` | `10000` | Graceful shutdown wait time for the normal processing thread pool, in milliseconds. |

## Relationship to Cache Time

Recommended:

```text
cache.timeout_s > dependency.timeoutThreshold
```

If `dependency.timeoutThreshold` is too short, a child Span may be saved as having no parent before the parent Span arrives, making call relationships, upstream/downstream views, and topology incomplete.

If `dependency.timeoutThreshold` is too long, parent-child relationships may be more complete, but cache usage and data write latency increase.

# Logging Configuration

Logging configuration is located at:

```text
prism-apm-parser/src/main/resources/logback-spring.xml
```

## Log Directory

Parser writes logs to this directory by default:

```text
logs
```

## Log Rotation

| Configuration | Default | Description |
| --- | --- | --- |
| `logging.custom.server.max-file-size` | `100MB` | Maximum size of a single log file. |
| `logging.custom.server.max-history` | `30` | Log retention days. |
| `logging.custom.server.total-size-cap` | `50GB` | Total log size cap. |

## Log Files

| File | Description |
| --- | --- |
| `logs/server.log` | Main service log. |
| `logs/reprocessing.log` | Reprocessing logs. |
| `logs/unparsed_span.log` | Logs for Span parse failures or incomplete Spans. |
| `logs/stats.log` | Periodic cache, queue, and Span statistics. |
| `logs/cache.log` | Cache expiration and eviction event logs. |
