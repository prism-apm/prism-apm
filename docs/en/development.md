---
created: 2026-06-24 15:11:30
modified: 2026-08-05 17:43:21
---

# Development Guide

This document is for Prism APM developers. It explains what you need to understand and maintain during local development. Deployment flows and runtime parameters are covered by the deployment and configuration docs. This page focuses on project structure, module boundaries, startup methods, common change entry points, and verification.

# Project Overview

## Project Positioning

Prism APM is an application performance monitoring project for OpenTelemetry Trace data. The system consumes Trace data from Kafka. Parser parses it and writes it to PostgreSQL/TimescaleDB. UI Server provides query, permission, and management APIs. The frontend displays service overview, endpoint details, database access, topology, and admin pages.

The current project mainly supports OpenTelemetry Trace JSON ingestion. Parser does not directly receive OTLP HTTP or OTLP gRPC, and it does not store complete raw Traces.

## Module Responsibilities

| Module | Responsibility | Development focus |
| --- | --- | --- |
| `prism-apm-common` | Common entities, MyBatis Mappers, and test helper code. | Common database tables, parser configuration, field replacement rules, and Testcontainers base classes. |
| `prism-apm-parser` | Consumes OpenTelemetry Trace JSON from Kafka, parses Spans, correlates parent-child Spans, and writes to TimescaleDB. | Parsing rules, field extraction, cache and DelayQueue, batch writes, and reprocessing APIs. |
| `prism-apm-ui-server` | Provides frontend query APIs, login/authentication, permission management, project management, topology rules, and table routing. | Controllers, Services, Mapper XML, dynamic datasources, JWT permissions, and query performance. |
| `prism-apm-ui` | Vue 2 + Vite frontend. | Page modules, API wrappers, routing, Pinia state, internationalization, charts, and topology display. |
| `prism-explorer` | Local demo and mock data tool. | One-click demo environment, database initialization, mock data generation, and documentation screenshot data preparation. |
| `sql/` | Database initialization scripts shared by production and local environments. | TimescaleDB extensions, core tables, continuous aggregates, permissions, and routing-rule seed data. |
| `docker-compose/` | Full local dependency and service orchestration. | Integration environment for TimescaleDB, Kafka, Jaeger, Parser, UI Server, and UI. |
| `scripts/` | Documentation screenshot and mock data helper scripts. | Playwright screenshots, quickstart screenshots, feature documentation screenshots, and mock data generation. |

## Recommended Reading Order

When joining development for the first time, read in this order:

1. Read [project introduction](introduction.md) to understand Prism APM's goals.
2. Read [quickstart](getting-started.md) and run the `prism-explorer` demo environment.
3. Read [overall architecture](architecture/architecture.md) and [Parser architecture](architecture/parser_architecture.md) to understand the path from Kafka Trace data to pages.
4. Read [Parser configuration](configuration/parser_configuration.md) and [UI Server configuration](configuration/ui-server_configuration.md) to understand key environment variables.
5. Return to this document and choose startup and verification methods based on the module you need to change.

# Local Development

## Development Environment

Recommended tools:

| Tool | Recommended version | Purpose |
| --- | --- | --- |
| JDK | 21 | Backend compile, test, and runtime. |
| Maven Wrapper | Maven 3.9.9, managed by `./mvnw` | Build Java modules without requiring a local Maven installation. |
| Node.js | `>=22.12.0` | Frontend dev server and build; required by `prism-apm-ui/package.json`. |
| npm | Installed with Node.js | Install frontend dependencies. |
| Docker Desktop | 4.0+ | Run TimescaleDB, Kafka, Jaeger, and Testcontainers tests. |
| `psql` | Matches PostgreSQL | Manually execute and verify database scripts. |

Backend modules use Spring Boot 3.2.5 and Java 21. The frontend uses Vue 2.7, Vue Router 3, Pinia, iView, ECharts, and AntV G6.

Run Maven commands from the repository root with `./mvnw`. On Windows, use `mvnw.cmd` instead.

## Local Dependencies

Full integration usually needs:

| Component | Default address | Description |
| --- | --- | --- |
| TimescaleDB | `localhost:5432` | Default host mapping in the main Docker Compose environment. |
| Kafka | `localhost:9094` | Address exposed by the main Docker Compose environment to the host. |
| Jaeger Collector OTLP HTTP | `localhost:4318` | Receives OTLP HTTP Trace data. |
| Jaeger UI | `http://localhost:16686` | View Traces received by Jaeger. |
| Parser | `http://localhost:8090` | Parser service and reprocessing APIs. |
| UI Server | `http://localhost:8080` | Frontend API. |
| UI dev server | `http://127.0.0.1:8066` | Vite dev server. |

## Build

### Package Services

To build backend services, enter the `prism-apm` directory and run:

```bash
./mvnw -pl prism-apm-common clean install -DskipTests
./mvnw -pl prism-apm-parser clean package -DskipTests
./mvnw -pl prism-apm-ui-server clean package -DskipTests
```

Or:

```bash
./mvnw -pl prism-apm-parser -am clean package -DskipTests
./mvnw -pl prism-apm-ui-server -am clean package -DskipTests
```

Build frontend:

```bash
cd prism-apm-ui
npm install
npm run build
```

### Build Images

To build images, enter the `prism-apm` directory and run:

```bash
docker build -f prism-apm-parser/Dockerfile -t prism-apm-parser .
docker build -f prism-apm-ui-server/Dockerfile -t prism-apm-ui-server .
docker build -f prism-apm-ui/Dockerfile -t prism-apm-ui .
```

`prism-apm-ui-server/Dockerfile` downloads OpenTelemetry Java Agent, so building that image requires access to Maven Central.

## Start the Full Environment with Docker Compose

Start the main Docker Compose environment from the repository root:

```bash
cd docker-compose
docker compose up -d
```

This orchestration contains TimescaleDB, Kafka, Jaeger, Parser, UI Server, and UI. Build images before the first startup; otherwise the `prism-apm-parser`, `prism-apm-ui-server`, and `prism-apm-ui` images may not exist.

## Start Services Individually

### Start Parser

Parser's entry class is `prism-apm-parser/src/main/java/org/prism/apm/parser/Application.java`, and the default port is `8090`.

Minimum local configuration:

```bash
export DB_ENDPOINT=localhost:5432
export DB_NAME=apm
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export KAFKA_BOOTSTRAP_SERVERS=localhost:9094
```

Start:

```bash
./mvnw -pl prism-apm-parser spring-boot:run
```

If Kafka has SASL enabled, enable the `kafka-sasl` profile at startup and set variables such as `KAFKA_USERNAME` and `KAFKA_PASSWORD`. See [parser configuration](configuration/parser_configuration.md).

### Start UI Server

UI Server's entry class is `prism-apm-ui-server/src/main/java/org/prism/apm/ui/server/Application.java`, and the default port is Spring Boot's default `8080`.

Minimum local configuration:

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

Start:

```bash
./mvnw -pl prism-apm-ui-server spring-boot:run
```

UI Server automatically adds the `/api` prefix to all `@RestController` endpoints. The login endpoint is `POST /api/apm/login`. The frontend stores the login token in `localStorage.apm_token` and sends `Authorization: Bearer <token>` in later requests.

### Start Frontend

The frontend directory is `prism-apm-ui`. The development service listens on `127.0.0.1:8066` by default and proxies `/api` to `http://127.0.0.1:8080`.

```bash
cd prism-apm-ui
npm install
npm run dev
```

Open in a browser:

```text
http://127.0.0.1:8066/
```

The default login account comes from initialization SQL:

| Username | Password |
| --- | --- |
| `admin` | `apm-admin` |

# Database Development

Database initialization scripts are under the repository root `sql/` and should be executed in this order:

```bash
psql -d apm -f sql/10_create_service_call_objects.sql
psql -d apm -f sql/11_create_app_config_objects.sql
psql -d apm -f sql/20_init-common-permission.sql
psql -d apm -f sql/21_init-common-replacer-rule.sql
psql -d apm -f sql/22_init-common-table-routing-rules.sql
psql -d apm -f sql/30_warmup_data.sql
```

When developing database-related features, pay special attention to:

| Topic | Description |
| --- | --- |
| TimescaleDB extensions | Both `timescaledb` and `timescaledb_toolkit` must be installed. |
| Hypertable | Trace detail tables are organized by time; writes and queries should keep time conditions. |
| Continuous aggregates | UI queries mainly depend on minute-level aggregate data. Late Spans and reprocessing affect aggregate windows. |
| Table routing | UI Server selects query objects through table routing rules. Related code is in the `route` package. |
| Read/write split | UI Server's default primary datasource is `slave`; write operations must explicitly use `master`. |

After modifying SQL, at minimum verify that scripts can run in order against an empty database and confirm that core tables, extensions, initialized accounts, and routing rules exist.

# Verification and Troubleshooting

## Tests

Backend tests use JUnit 5, Spring Boot Test, and Testcontainers. Before running Testcontainers tests, make sure Docker is available.

Common test commands:

```bash
./mvnw -pl prism-apm-common test
./mvnw -pl prism-apm-parser -am test
./mvnw -pl prism-apm-ui-server -am test
```

Choose verification scope according to changes:

| Change | Recommended verification |
| --- | --- |
| SQL initialization, TimescaleDB tables, or continuous aggregates | `TimescaleDdlInitializationTest`, `ApmTimescaleSchemaDdlTest`, `ApmTimescaleCaggCalculationTest` |
| Parser field parsing or component rules | `OpenTelemetrySpanParserTest`, `OpenTelemetrySpanInfoBuilderTest`, `ParameterizedProcessingPipelineTest` |
| Kafka configuration | `KafkaNoAuthConnectionTest`, `KafkaSaslConnectionTest` |
| UI Server table routing | `TableRoutingServiceTest`, `TableRoutingStoreTest` |
| UI Server query SQL | `ServiceCallMinuteDOMapper4ServiceTest`, `ServiceCallMinuteDOMapper4TargetTest`, `ServiceCallMinuteDOMapper4DBTest` |
| Permissions and admin management | `AdminUserServiceTest`, `AdminProjectServiceTest`, `AdminPermissionServiceTest`, `AdminAssignServiceTest` |
| Frontend build | `npm run build` |

## Logs and Troubleshooting

Main Parser logs:

| File | Description |
| --- | --- |
| `logs/server.log` | Main service log. |
| `logs/reprocessing.log` | Reprocessing log. |
| `logs/unparsed_span.log` | Span parse failure or missing fields. |
| `logs/stats.log` | Cache, queue, and Span processing statistics. |
| `logs/cache.log` | Ehcache expiration and eviction events. |

UI Server mainly outputs to console under the default profile. After enabling the `prod` or `test` profile, it writes to `logs/ui-server-yyyy-MM-dd.i.log` and `logs/db-trace-yyyy-MM-dd.i.log`.

Common troubleshooting order:

1. When pages have no data, first confirm whether UI Server login and query APIs work.
2. Confirm whether `service_call` and minute-level aggregate tables have data.
3. If the database has no data, check whether Parser consumed Kafka messages.
4. If Parser consumed messages but did not write them, check `unparsed_span.log`, `stats.log`, and parsing rules.
5. If topology or upstream/downstream relationships are incomplete, check whether Kafka partitions by `traceId`, and check the relationship between `cache.timeout_s` and `dependency.timeoutThreshold`.

# Documentation Maintenance

## Docs to Update for Common Changes

During development, update documentation when these areas change:

| Change | Docs to update |
| --- | --- |
| Add or modify deployment method | `deployment.md` |
| Add Parser configuration | `configuration/parser_configuration.md` |
| Add UI Server configuration | `configuration/ui-server_configuration.md` |
| Modify database initialization scripts | Deployment docs, configuration docs, and related architecture docs |
| Modify Trace parsing rules or support new components | `architecture/parser_architecture.md`, `features/parser_feature.md` |
| Add frontend feature page | Corresponding `features/**` doc |
| Modify login, permissions, or default account | `features/admin_feature.md` and UI Server configuration docs |
| Modify Quickstart or Explorer experience flow | `getting-started.md` |
| Update documentation screenshots | `docs/assets/**` and `scripts/README.md` |

## Documentation Screenshots

Documentation screenshot scripts are under `scripts/`. Start the `prism-explorer` environment before running them.

Install Playwright:

```bash
mkdir -p .doc-tools
npm install --prefix .doc-tools playwright
npx --prefix .doc-tools playwright install chromium
```

Common commands:

```bash
node scripts/capture-quickstart-screenshots.mjs
node scripts/capture-service-metrics-screenshots.mjs
node scripts/capture-topology-screenshots.mjs
```

By default, screenshot scripts clean and generate mock data for the latest 7 days through Prism Explorer, then log in to Prism APM UI and capture screenshots. Use `--skip-data` when suitable data already exists.
