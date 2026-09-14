---
created: 2026-05-28 14:29:02
modified: 2026-08-04 16:48:05
---

This document explains how to deploy Prism APM in a public environment. It is based on the current repository state and focuses on local development and single-node server deployment.

For detailed configuration of each service, see:

- [parser configuration](configuration/parser_configuration.md)
- [ui-server configuration](configuration/ui-server_configuration.md)

# Preparation

## Deployment Architecture

A minimum usable environment needs:

- `prism-apm-parser`: ingests, parses, and writes data.
- `prism-apm-ui-server`: provides UI query APIs and login/authentication APIs.
- `prism-apm-ui`: provides browser pages.
- `TimescaleDB`: stores trace parsing results, aggregate results, permission data, and query configuration.
- `Kafka`: receives upstream trace data for `prism-apm-parser` to consume.

## Prerequisites

Prepare these environments first:

| Dependency | Recommended version | Purpose |
| --- | --- | --- |
| JDK | 21 | Backend build and runtime. |
| Maven | 3.9+ | Build Java modules. |
| Node.js | `>=22.12.0` | Build and run the frontend development service. |
| npm | Installed with Node.js | Install frontend dependencies. |
| TimescaleDB | Supports the `timescaledb` extension | Create hypertables and continuous aggregates. |
| TimescaleDB Toolkit | Supports the `timescaledb_toolkit` extension | The SQL scripts create this extension. |
| Kafka | Compatible with Spring Kafka client | Trace data input. |
| `psql` | Matches PostgreSQL | Execute database initialization scripts. |

If running in containers, make sure the container network can access TimescaleDB and Kafka. `localhost` inside a container points to the container itself, not the host.

## Ports and Defaults

| Component | Default port | Source |
| --- | --- | --- |
| `prism-apm-parser` | `8090` | `prism-apm-parser/src/main/resources/application.properties` |
| `prism-apm-ui-server` | `8080` | Spring Boot default port; the current configuration files do not override it. |
| `prism-apm-ui` | `8066` | `prism-apm-ui/vite.config.ts` |
| Frontend dev proxy | `/api` -> `http://127.0.0.1:8080` | `prism-apm-ui/vite.config.ts` |

Default Kafka topics:

| Configuration | Default |
| --- | --- |
| `parser.kafka_topic` | `opentelemetry-trace` |
| `parser.reprocessing_kafka_topic` | `reprocessing-opentelemetry-trace` |

## Database Initialization

### Create Database

This example uses a local database named `apm`:

```bash
CREATE DATABASE apm;
```

### Execute SQL Scripts

Run these scripts from the repository root in order:

```bash
psql -d apm -f sql/10_create_service_call_objects.sql
psql -d apm -f sql/11_create_app_config_objects.sql
psql -d apm -f sql/20_init-common-permission.sql
psql -d apm -f sql/21_init-common-replacer-rule.sql
psql -d apm -f sql/22_init-common-table-routing-rules.sql
psql -d apm -f sql/30_warmup_data.sql
```

Notes:

- `10_create_service_call_objects.sql` creates TimescaleDB extensions, core trace tables, hypertables, and continuous aggregates.
- `11_create_app_config_objects.sql` creates application tables such as permissions, projects, routing rules, and parser configuration.
- The `20_*`, `21_*`, and `22_*` scripts insert common seed data.
- `30_warmup_data.sql` contains prewarm-related functions and is useful when TimescaleDB metadata prewarming is needed.

### Database Permission Notes

`10_create_service_call_objects.sql` currently does the following:

- Creates the `timescaledb` extension.
- Creates the `timescaledb_toolkit` extension.
- Creates database user `ts_job_runner` for TimescaleDB jobs.

The database user running the script must be allowed to create extensions, tables, views, users, and grants. Do not use example passwords in production. Replace them with secure passwords before going online, or pre-create users and adjust scripts according to your DBA process.

### Verify Database

Run:

```bash
psql -d apm -c "select extname from pg_extension where extname in ('timescaledb', 'timescaledb_toolkit');"
psql -d apm -c "select count(*) from information_schema.tables where table_schema = 'public';"
psql -d apm -c "select count(*) from service_call;"
```

If the `service_call` table does not exist, the core SQL did not execute successfully.

# Start the Project Locally

1. Download the project first.

2. Start it:

```shell
cd docker-compose
docker compose up -d
```

`docker-compose` contains Jaeger Collector related components. After startup, you can try sending traces directly to port `4318`. Use this address to view received traces and processing status:

- `http://localhost:16686`: check whether traces have been received normally.

3. Configure projects through `http://localhost:8066/#/admin` first. Use `http://localhost:8066` to view project call data. Default username and password:

- User Name: `admin`
- Password: `apm-admin`

# Production or Single-Node Deployment

A single-node deployment can use this flow:

1. Deploy TimescaleDB on the server.
2. Deploy Kafka, or connect to an existing Kafka cluster.
3. Execute initialization scripts under `sql/`.
4. Build and start `prism-apm-parser`.
5. Build and start `prism-apm-ui-server`.
6. Build `prism-apm-ui` and host `dist/` with Nginx or another static server.
7. Configure Nginx to reverse proxy `/api` to `prism-apm-ui-server`.

## Start Parser

The following is an example of running Parser in a container. Modify the related configuration before startup.

```bash
docker run --rm \
  -p 8090:8090 \
  -e DB_ENDPOINT=host.docker.internal:5432 \
  -e DB_NAME=apm \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=your-db-password \
  -e KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9094 \
  -e KAFKA_USERNAME=admin \
  -e KAFKA_PASSWORD=your-kafka-password \
  -e PARSER_KAFKA_TOPIC=opentelemetry-trace \
  prism-apm-parser \
  --spring.profiles.active=kafka-sasl
```

If Kafka does not require SASL authentication:

```bash
docker run --rm \
  -p 8090:8090 \
  -e DB_ENDPOINT=host.docker.internal:5432 \
  -e DB_NAME=apm \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=your-db-password \
  -e KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9094 \
  -e PARSER_KAFKA_TOPIC=opentelemetry-trace \
  prism-apm-parser
```

## Start UI Server

The following is an example of running UI Server in a container. Modify the related configuration before startup.

```bash
docker run --rm \
  -p 8080:8080 \
  -e JWT_SECRET=replace-with-a-strong-secret \
  -e JWT_EXPIRATION=86400000 \
  -e DB_WRITE_ENDPOINT=host.docker.internal:5432 \
  -e DB_WRITE_DB_NAME=apm \
  -e DB_WRITE_USERNAME=postgres \
  -e DB_WRITE_PASSWORD=your-db-password \
  -e DB_READ_ENDPOINT=host.docker.internal:5432 \
  -e DB_READ_DB_NAME=apm \
  -e DB_READ_USERNAME=postgres \
  -e DB_READ_PASSWORD=your-db-password \
  prism-apm-ui-server --spring.profiles.active=local
```

## Start UI

Build UI:

```bash
cd prism-apm-ui
npm install
npm run build
```

Build output goes to:

```text
prism-apm-ui/dist/
```

Example Nginx configuration:

```nginx
server {
    listen 80;
    server_name prism-apm.example.com;

    root /opt/prism-apm-ui/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

# Verification

## Verify Parser

1. Check whether there is data in the `PARSER_KAFKA_TOPIC` topic.
2. Check whether `log/server.log` outputs normally.
3. Check whether the database `service_call` table has data.
4. Check whether the database `service_call_minute_entry` table has data.

## Verify UI Server

The login endpoint allows anonymous access and can be used to verify whether the HTTP service is reachable. Replace the IP address as needed.

```bash
curl -i -X POST "http://localhost:8080/api/apm/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data "username=admin&password=replace-with-real-password"
```

If it returns `401` or login fails but the HTTP response is normal, the service has started. Next, check initialized users and passwords.

## Verify Frontend

Open this address to verify access. Replace the IP address as needed.

```text
http://localhost:8066
```

In production, open the deployed static site address. Open browser developer tools and confirm that `/api` requests are correctly proxied to `prism-apm-ui-server`.

# Scaling

`prism-apm-ui-server` is stateless and supports horizontal scaling. `prism-apm-parser` is stateful; restart or scaling can lose some in-progress data. See [parser](architecture/parser_architecture.md) for details.
