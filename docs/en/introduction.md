---
created: 2026-05-19 15:02:35
modified: 2026-08-10 17:33:54
---

# About

PrismAPM is a distributed application performance monitoring system based on OpenTelemetry. Its goal is not only to show endpoint call information, but also to provide important evidence when you are investigating complex and difficult problems.

You can use PrismAPM to:

- View service, endpoint, database, and other call information.
- View the topology of all services.
- Inspect internal calls inside an application.
- Compare metrics.
- View upstream and downstream data.
- Use fine-grained metrics.

# Highlights

## Inspect Internal Application Calls

When endpoint latency increases, we usually need to know what made the endpoint slow: DB, Redis, or another external call. PrismAPM lets you inspect internal endpoint calls, including duration and percentage of total duration.

![Internal details - grouped by request category](../assets/en/features/service-metrics/endpoint-internal-details-lower-by-type.png)

## Compare Metrics

When a problem occurs, you can compare concrete metrics. Endpoints can be compared by time range or by host.

![RED comparison - time comparison](../assets/en/features/service-metrics/endpoint-red-comparison-time.png)

## Provide Upstream and Downstream Data

For an endpoint, the system can provide both upstream and downstream call data.

- Upstream: which services called the endpoint, and how many times.
- Downstream: which services the endpoint called.

![Source statistics](../assets/en/features/service-metrics/endpoint-source-stats.png)

## Topology

Use the topology graph to understand service architecture and service call relationships.

![Topology](../assets/en/quickstart/topology.png)

## More

### Using Fine-Grained Metrics

In addition to endpoint names and counts, the system records other useful fields. You can inspect these fields to determine whether a problem is related to a component, a component version, a host, or another dimension.

Some fields and example values stored in the database include:

- `system_type`: `http`, `kafka`, `redis`, `postgresql`...
- `component`: `io.opentelemetry.jdbc`, `io.opentelemetry.apache-httpclient-2.0`, `io.opentelemetry.apache-httpclient-4.0`...
- `action_name`: `PUT`, `POST`, `SETEX`, `BATCH INSERT`...
- `host_name`: `your-server-prod-o-59595cdc5f-hkhz9`...
- `remote_addr`: `11.73.52.114`, `${public_ip}`...

### Inspect Logical Branch Calls

If trace output is added to logical branch code, the internal details feature can show how many times each branch is called. You can also infer branch usage by checking whether middleware or other services were called.

### Inspect Cache Calls

Database statistics can be used to see how many times cache was called during a specific time range.

# What It Cannot Do

The current system is not suitable for these scenarios:

- Analyzing very long calls that exceed the system wait time or continuous aggregate window. Very long durations may not be accurately shown in all aggregate views.
  - For example, minute-level aggregation covers data from the current time back to the previous 10 minutes. If data generated at `14:00:00` runs for 15 minutes and is processed at `14:15:00`, that record will not be aggregated. You can adjust the minute-level aggregation window, but the 5-minute, hourly, and daily aggregation windows may also need corresponding adjustments.

# Future Features

- MCP support
- ...

# Next Steps

- Quickstart
- Feature
- Architecture
- Configuration
