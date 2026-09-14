---
created: 2026-07-30 16:41:14
modified: 2026-08-03 14:51:11
---

# UI Server Configuration

This document describes runtime configuration for `prism-apm-ui-server`. UI Server provides query, topology, management, permission, login, and authentication APIs.

To understand UI Server responsibilities, API groups, and data access conventions, see:

- [UI Server architecture](../architecture/ui-server_architecture.md)

# Minimum Required Configuration

When starting UI Server, you usually need to provide read/write database connection information and a JWT signing secret. Do not write a full JDBC URL directly; the project assembles datasource configuration from the `DB_WRITE_*` and `DB_READ_*` environment variables.

## Environment Variables

| Environment variable | Example | Description |
| --- | --- | --- |
| `DB_WRITE_ENDPOINT` | `postgres:5432` | Write database endpoint, in `host:port` format. |
| `DB_WRITE_DB_NAME` | `apm` | Write database name. |
| `DB_WRITE_USERNAME` | `postgres` | Write database username. |
| `DB_WRITE_PASSWORD` | `your-db-password` | Write database password. |
| `DB_READ_ENDPOINT` | `postgres:5432` | Read database endpoint, in `host:port` format. |
| `DB_READ_DB_NAME` | `apm` | Read database name. |
| `DB_READ_USERNAME` | `postgres` | Read database username. |
| `DB_READ_PASSWORD` | `your-db-password` | Read database password. |
| `JWT_SECRET` | `replace-with-a-strong-random-secret` | JWT HMAC signing secret. |

For a single-database deployment, the read and write database can point to the same PostgreSQL/TimescaleDB instance, but both `DB_WRITE_*` and `DB_READ_*` variables must be set.

`JWT_SECRET` must be a sufficiently long random value. UI Server signs JWTs with HS256, which requires at least 32 bytes. Do not use example values or placeholder values from the repository.

## Example

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

## Internal Mapping

The application uses these configuration templates internally to generate final datasource and JWT settings:

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

## Default Configuration

`server.port` is not overridden in configuration files, so Spring Boot's default port `8080` is used. Override `server.port` only when the port conflicts or the deployment platform requires another port.

`JWT_EXPIRATION` defaults to `86400000` milliseconds, or 24 hours. Override it only when the login token lifetime needs to change.

# Database Configuration

UI Server uses dynamic datasources:

| Datasource | Internal name | Purpose |
| --- | --- | --- |
| Write database | `master` | Write operations for management, permissions, projects, topology rules, and similar data. |
| Read database | `slave` | Default datasource for normal queries. |

The default primary datasource is `slave`. Write operations switch to the write database through `@DS("master")` in code.

## Single-Database Deployment

If there is no separate read-only database, configure the read and write database as the same database:

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

## Read/Write Split Deployment

If using primary/standby or read/write splitting, make sure:

- `DB_WRITE_*` points to a writable database.
- `DB_READ_*` points to a read-only database or read replica.

## Database Initialization

UI Server depends on database objects and seed data under the repository root `sql/` directory. Database initialization must be completed before the first run; otherwise login, permissions, service queries, and topology queries may not work.

# JWT Configuration

UI Server uses JWT for login-state authentication.

## Environment Variables

| Environment variable | Default | Description |
| --- | --- | --- |
| `JWT_SECRET` | `change-me` | JWT HMAC signing secret. Must be overridden in real deployments. |
| `JWT_EXPIRATION` | `86400000` | JWT lifetime in milliseconds. |

Although `JWT_SECRET` has a placeholder default in configuration files, that value is not suitable for real deployments. HS256 requires at least a 32-byte secret, and a secret that is too short may cause application startup failure.

## Multi-Instance Deployment

If multiple UI Server instances are deployed, all instances must use the same `JWT_SECRET`. Otherwise, a token issued by one instance may not be validated by another instance.

If `JWT_SECRET` is changed, old tokens become invalid immediately and users must log in again.

## Lifetime Settings

`JWT_EXPIRATION` is in milliseconds:

| Example value | Meaning |
| --- | --- |
| `3600000` | 1 hour. |
| `86400000` | 24 hours. |
| `604800000` | 7 days. |

Shorter lifetimes reduce the risk window for leaked tokens, but users need to log in more often. Longer lifetimes improve user experience, but increase security risk.

# Prewarm Job Configuration

UI Server includes an optional PostgreSQL/TimescaleDB prewarm job that periodically calls a database function to reduce cold-query latency. This job is disabled by default.

## Configuration

| Configuration | Default | Description |
| --- | --- | --- |
| `apm.schedule.prewarm-enabled` | `false` | Whether to enable the prewarm job. |
| `apm.schedule.prewarm-function` | `prewarm_data` | Database function name to call, without parentheses. |
| `apm.schedule.prewarm-cron` | `* 0/10 * * * ?` | Cron expression for the job. |

## When to Enable

Enable the prewarm job only when all of these conditions are met:

- The corresponding prewarm function has already been created in the database.
- UI queries become noticeably slow after the system has not been accessed for a while.
- The database can handle the extra query load caused by scheduled prewarming.

Example:

```bash
java -jar prism-apm-ui-server/target/prism-apm-ui-server-*.jar \
  --apm.schedule.prewarm-enabled=true \
  --apm.schedule.prewarm-function=prewarm_data \
  --apm.schedule.prewarm-cron="0 0/10 * * * ?"
```

If the Cron expression is invalid, the code falls back to the default Cron expression and writes an error log.

# Logging Configuration

Logging configuration is located at:

```text
prism-apm-ui-server/src/main/resources/logback-spring.xml
```

## Default Behavior

When no Spring profile is specified, logs are written to the console.

When the `prod` or `test` profile is enabled, root logs are written to:

```text
logs/ui-server-yyyy-MM-dd.i.log
```

## Log Rotation

The current log rotation policy is written in `logback-spring.xml`, not configured through environment variables:

| Log | Single-file size | Retention | Total size cap |
| --- | --- | --- | --- |
| UI Server main log | `100MB` | `10` | `10GB` |
| Database troubleshooting log | `200MB` | `3` | `5GB` |

To change log file size, retention days, or log directory, modify `logback-spring.xml`.

## Database Troubleshooting Log

`logback-spring.xml` configures a separate database troubleshooting log:

```text
logs/db-trace-yyyy-MM-dd.i.log
```

This log is used for troubleshooting HikariCP, the PostgreSQL driver, dynamic datasources, and the Mapper layer. Database logs can grow quickly; after troubleshooting, restore the normal log level and remove unneeded log files.

# OpenTelemetry Java Agent Configuration

The `prism-apm-ui-server` Docker image includes OpenTelemetry Java Agent by default. Running the jar directly does not automatically enable the agent unless `-javaagent` is added manually.

## Docker Defaults

The Dockerfile sets these defaults:

| Environment variable | Default | Description |
| --- | --- | --- |
| `OTEL_SERVICE_NAME` | `prism-apm-ui-server` | Service name reported to OpenTelemetry. |
| `OTEL_TRACES_EXPORTER` | `otlp` | Trace exporter. |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://host.docker.internal:4318` | OTLP endpoint. |
| `OTEL_METRICS_EXPORTER` | `none` | Metrics are not reported. |
| `OTEL_LOGS_EXPORTER` | `none` | Logs are not reported. |
| `OTEL_SEMCONV_STABILITY_OPT_IN` | `http,database,messaging,rpc` | OpenTelemetry semantic convention opt-in. |

## When to Override

If OpenTelemetry Collector is not available at `host.docker.internal:4318`, override `OTEL_EXPORTER_OTLP_ENDPOINT`.

If you do not want UI Server to report traces, set:

```bash
export OTEL_TRACES_EXPORTER=none
```
