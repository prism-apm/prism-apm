---
created: 2026-05-18 16:04:05
modified: 2026-08-07 17:51:54
---
# Roles

- Parser: receives traces, parses them according to rules, and stores the results in the database.
- UI Server: provides data query capabilities across different dimensions.

# Architecture

The system receives traces from Kafka, parses them, and stores the parsed data in the database. Direct ingestion from OpenTelemetry Collector is not supported yet.

![](../../assets/PrismAPM-architecture.svg)

Prism APM currently consists of these main modules:

| Module | Description |
| --- | --- |
| `prism-apm-parser` | Consumes OpenTelemetry trace data from Kafka, parses it, and writes it to the database. |
| `prism-apm-ui-server` | Backend service for data queries, permission configuration, and related capabilities. |
| `prism-apm-ui` | Frontend UI. In development it runs through Vite; in production it is built as static assets. |
| `prism-apm-common` | Shared backend entities, mappers, utilities, and test helpers. |
| `sql/` | Database schema, permission initialization, routing-rule initialization, and related scripts. |

# Storage

Prism APM uses TimescaleDB as its database. The main reasons are:

- Powerful features: it provides many data aggregation features and functions.
- Strong performance: single-node performance is strong enough for many company workloads.
- Easy to adopt: it is based on PostgreSQL, so the learning curve is low.
- Primary/standby support: inherited from PostgreSQL.
