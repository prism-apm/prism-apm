DROP MATERIALIZED VIEW IF EXISTS service_call_minute;
DROP TABLE IF EXISTS service_call;

-- Step 1: Create raw table (Hypertable)
CREATE TABLE IF NOT EXISTS service_call
(
    time                  TIMESTAMP WITH TIME ZONE NOT NULL,
    trace_id              TEXT                     NOT NULL, -- Fill this field in test scripts
    span_id               TEXT                     NOT NULL,
    service_name          TEXT                     NOT NULL,
    host_name             TEXT                     NOT NULL,
    action_name           TEXT                     NOT NULL,
    target_name           TEXT                     NOT NULL, -- Fill this field in test scripts
    remote_addr           TEXT                     NOT NULL, -- Fill this field in test scripts
    system_type           TEXT                     NOT NULL, -- Fill this field in test scripts
    component             TEXT                     NOT NULL,
    span_kind             SMALLINT                 NOT NULL,
    caller_time           TIMESTAMP WITH TIME ZONE NOT NULL,
    caller_span_id        TEXT                     NOT NULL,
    caller_service_name   TEXT                     NOT NULL,
    caller_host_name      TEXT                     NOT NULL,
    caller_action_name    TEXT                     NOT NULL,
    caller_target_name    TEXT                     NOT NULL, -- Fill this field in test scripts
    caller_remote_addr    TEXT                     NOT NULL, -- Fill this field in test scripts
    caller_system_type    TEXT                     NOT NULL, -- Fill this field in test scripts
    caller_component      TEXT                     NOT NULL,
    caller_span_kind      SMALLINT                 NOT NULL,
    cnt                   BIGINT                   NOT NULL,
    duration              BIGINT                   NOT NULL,
    error                 BIGINT                   NOT NULL,
    create_time           TIMESTAMP WITH TIME ZONE NOT NULL,
    update_time           TIMESTAMP WITH TIME ZONE NOT NULL
);

-- create hypertable
SELECT create_hypertable('service_call', by_range('time', INTERVAL '1 day'));

-- Enable the extension utilities package
CREATE EXTENSION IF NOT EXISTS timescaledb_toolkit;

-- Minute-level data view start
CREATE MATERIALIZED VIEW service_call_minute
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '1 minute', time) AS time,
    service_name,
    host_name,
    action_name,
    target_name,
    remote_addr,
    system_type,
    component,
    span_kind,
    caller_service_name,
    caller_host_name,
    caller_action_name,
    caller_target_name,
    caller_remote_addr,
    caller_system_type,
    caller_component,
    caller_span_kind,
    SUM(cnt) as cnt,
    ROUND(AVG(duration)) AS avg_duration,
    MAX(duration) AS max_duration,
    MIN(duration) AS min_duration,
    COUNT(error) FILTER (WHERE error != 0) AS error_num,
    percentile_agg(duration) AS percent_duration
FROM service_call
GROUP BY
    time_bucket(INTERVAL '1 minute', time),
    service_name,
    host_name,
    action_name,
    target_name,
    remote_addr,
    system_type,
    component,
    span_kind,
    caller_service_name,
    caller_host_name,
    caller_action_name,
    caller_target_name,
    caller_remote_addr,
    caller_system_type,
    caller_component,
    caller_span_kind
WITH NO DATA;