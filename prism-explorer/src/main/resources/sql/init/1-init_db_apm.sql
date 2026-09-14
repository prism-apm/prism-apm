CREATE EXTENSION IF NOT EXISTS timescaledb;
CREATE EXTENSION IF NOT EXISTS timescaledb_toolkit;

BEGIN;
-- ==================================================================
-- [Added]0. Initialize environment and create dedicated job user
-- ==================================================================

CREATE USER ts_job_runner WITH PASSWORD 'ts_job_runner';

-- [Key configuration]Force this user to disable sort and prefer HashAggregate
ALTER ROLE ts_job_runner SET enable_sort = off;
-- [Key configuration] Grant enough memory (512 MB is recommended for roughly 3 billion writes per day)
ALTER ROLE ts_job_runner SET work_mem = '512MB';
-- Grant schema permissions
GRANT USAGE ON SCHEMA public TO ts_job_runner;

-- ==================================================================
-- [Added]0.5 Resource dictionary table (stores Hash -> original text mappings)
-- ==================================================================
CREATE TABLE resource_hash_dictionary
(
    hash_key    UUID NOT NULL, -- 128-bit Hash (XXH3)
    category    TEXT NOT NULL, -- Business category (e.g. 'target_name')
    algorithm   TEXT NOT NULL, -- Algorithm identifier (e.g. 'XXH3_128')
    text_value  TEXT NOT NULL, -- Original text
    create_time TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT resource_hash_dictionary_pkey PRIMARY KEY (hash_key, category)
);

-- ==================================================================
-- 1. Create table structure (unchanged)
-- ==================================================================
CREATE TABLE service_call
(
    time                  TIMESTAMP WITH TIME ZONE NOT NULL,
    trace_id              TEXT                     NOT NULL,
    span_id               TEXT                     NOT NULL,
    service_name          TEXT                     NOT NULL,
    host_name             TEXT                     NOT NULL,
    action_name           TEXT                     NOT NULL,
    target_name           UUID                     NOT NULL, -- [Note]Stores a 128-bit hash (UUID type) here
    remote_addr           TEXT                     NOT NULL,
    system_type           TEXT                     NOT NULL,
    component             TEXT                     NOT NULL,
    span_kind             SMALLINT                 NOT NULL,
    -- Caller info
    caller_time           TIMESTAMP WITH TIME ZONE NOT NULL,
    caller_span_id        TEXT                     NOT NULL,
    caller_service_name   TEXT                     NOT NULL,
    caller_host_name      TEXT                     NOT NULL,
    caller_action_name    TEXT                     NOT NULL,
    caller_target_name    UUID                     NOT NULL, -- [Note]Stores a 128-bit hash (UUID type) here
    caller_remote_addr    TEXT                     NOT NULL,
    caller_system_type    TEXT                     NOT NULL,
    caller_component      TEXT                     NOT NULL,
    caller_span_kind      SMALLINT                 NOT NULL,
    -- Metrics
    cnt                   BIGINT                   NOT NULL,
    duration              BIGINT                   NOT NULL,
    error                 BIGINT                   NOT NULL,
    create_time           TIMESTAMP WITH TIME ZONE NOT NULL,
    update_time           TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT service_call_pkey PRIMARY KEY (time, span_id)
);


-- [Added]Ensure the job user can read raw data
GRANT SELECT, INSERT, UPDATE, DELETE ON service_call TO ts_job_runner;

-- 2. Convert to a hypertable
SELECT create_hypertable('service_call', by_range('time', INTERVAL '2 hours'));


-- ==================================================================
-- 2. Enable columnstore
-- ==================================================================
ALTER TABLE service_call SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'service_name, span_kind',
    timescaledb.orderby = 'time DESC'
    );

-- Compression policy
CALL add_columnstore_policy('service_call',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- 4. Indexes
-- for aggregates
CREATE INDEX idx_sc_kind_time ON service_call (span_kind, time DESC);

-- Retention policy
SELECT add_retention_policy('service_call', INTERVAL '4 days');


-- =========================================================
-- Tier 1: Entry minute-level aggregate (foundation view)
-- =========================================================
CREATE MATERIALIZED VIEW service_call_minute_entry
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
    -- Accumulate duration as sum_duration for upper layers
    SUM(duration) AS sum_duration,
    -- This layer aggregates directly; no change is needed
    ROUND(AVG(duration)) AS avg_duration,
    MAX(duration) AS max_duration,
    MIN(duration) AS min_duration,
    COUNT(error) FILTER (WHERE error != 0) AS error_num,
    percentile_agg(duration) AS percent_duration,
    MAX(update_time) AS last_insert_time  -- Last write time for this minute of data, used to investigate late arrivals
FROM service_call
WHERE span_kind IN (2, 5)
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

-- [Added]Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_minute_entry OWNER TO ts_job_runner;

-- This index is used to query host name and entry-related data.
CREATE INDEX idx_sc_min_entry_svc_kind_time
    ON service_call_minute_entry (service_name, span_kind, time DESC);
-- This index is similar to the one above but performs better. Queried fields no longer need heap lookups. The index size is about 18% larger than the one above.
-- CREATE INDEX idx_sc_min_entry_svc_kind_time_cov
--     ON service_call_minute_entry (service_name, span_kind, time DESC)


CREATE INDEX idx_sc_min_entry_svc_kind_tgt_time
    ON service_call_minute_entry (service_name, span_kind, target_name, time DESC);
-- This index is similar to the one above but performs better. Queried fields no longer need heap lookups. The index size is about 60% larger than the one above.
-- CREATE INDEX idx_sc_min_entry_svc_kind_tgt_time_cov
--     ON service_call_minute_entry (service_name, span_kind, target_name, time DESC)



SELECT set_chunk_time_interval('service_call_minute_entry', INTERVAL '2 hours');

ALTER MATERIALIZED VIEW service_call_minute_entry SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'service_name, span_kind, target_name, system_type'
    );

SELECT add_continuous_aggregate_policy('service_call_minute_entry',
                                       start_offset => INTERVAL '10 minutes',
                                       end_offset   => INTERVAL '0 minutes',
                                       schedule_interval => INTERVAL '1 minute');

CALL add_columnstore_policy('service_call_minute_entry',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Retention policy：keep 3 days of data
SELECT add_retention_policy('service_call_minute_entry', INTERVAL '4 days');


-- =========================================================
-- Tier 2: Entry daily archive (based on Minute)
-- =========================================================
CREATE MATERIALIZED VIEW service_call_minute_entry_merged
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '1 minute', time) as time,
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
    -- [Added]Pass through sum_duration
    SUM(sum_duration) AS sum_duration,
    -- [Changed]Use exact division
    ROUND(SUM(sum_duration)::numeric / NULLIF(SUM(cnt), 0), 0) AS avg_duration,
    MAX(max_duration) AS max_duration,
    MIN(min_duration) AS min_duration,
    SUM(error_num) AS error_num,
    rollup(percent_duration) AS percent_duration,
    MAX(last_insert_time) AS last_insert_time
FROM service_call_minute_entry
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

-- [Added]Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_minute_entry_merged OWNER TO ts_job_runner;





CREATE INDEX idx_sc_min_entry_m_svc_kind_time
    ON service_call_minute_entry_merged (service_name, span_kind, time DESC);
-- CREATE INDEX idx_sc_min_entry_m_svc_kind_time_cov
--     ON service_call_minute_entry_merged (service_name, span_kind, time DESC)

CREATE INDEX idx_sc_min_entry_m_svc_kind_tgt_time
    ON service_call_minute_entry_merged (service_name, span_kind, target_name, time DESC);
-- CREATE INDEX idx_sc_min_entry_m_svc_kind_tgt_time_cov
--     ON service_call_minute_entry_merged (service_name, span_kind, target_name, time DESC)


SELECT set_chunk_time_interval('service_call_minute_entry_merged', INTERVAL '1 day');

ALTER MATERIALIZED VIEW service_call_minute_entry_merged SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'service_name, span_kind, target_name, system_type'
    );

-- Run every day at 02:00
SELECT add_continuous_aggregate_policy(
               'service_call_minute_entry_merged',
               start_offset => INTERVAL '27 hours',
               end_offset   => INTERVAL '2 hours',
               schedule_interval => INTERVAL '1 day',
               initial_start => date_trunc('day', now() + interval '1 day') + interval '2 hours'
       );

-- Run compression 1 hour later than the aggregate above
CALL add_columnstore_policy('service_call_minute_entry_merged',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '1 day',
                            initial_start     => date_trunc('day', now() + interval '1 day') + interval '3 hours');


-- Retention policy：keep 30 days of data
SELECT add_retention_policy('service_call_minute_entry_merged', INTERVAL '30 days');



-- ==========================================
-- 1. Minute-level Entry unified view
-- Boundary: 2 days (= merged chunk 1 day + compression delay 1 day)
--
-- Compared with querying the entry table directly, this approach uses more buffers during queries, roughly 2.5x or more.
-- ==========================================
CREATE VIEW service_call_minute_entry_unified AS
SELECT * FROM service_call_minute_entry
WHERE time >= NOW() - INTERVAL '2 days'
UNION ALL
SELECT * FROM service_call_minute_entry_merged
WHERE time < NOW() - INTERVAL '2 days';



-- =========================================================
-- Tier 1: Dependency minute-level aggregate
-- =========================================================
CREATE MATERIALIZED VIEW service_call_minute_dependency
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
    -- [Added]
    SUM(duration) AS sum_duration,
    ROUND(AVG(duration)) AS avg_duration,
    MAX(duration) AS max_duration,
    MIN(duration) AS min_duration,
    COUNT(error) FILTER (WHERE error != 0) AS error_num,
    percentile_agg(duration) AS percent_duration,
    MAX(update_time) AS last_insert_time
FROM service_call
WHERE span_kind IN (1, 3, 4)
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

-- [Added]Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_minute_dependency OWNER TO ts_job_runner;

CREATE INDEX idx_sc_min_dep_caller ON service_call_minute_dependency (caller_service_name, caller_span_kind, span_kind, caller_target_name, time DESC, host_name);
CREATE INDEX idx_sc_min_dep_callee ON service_call_minute_dependency (service_name, system_type, span_kind, caller_target_name, target_name, time DESC, host_name);


SELECT set_chunk_time_interval('service_call_minute_dependency', INTERVAL '2 hours');

-- host_name is not included in segment by. Add it if performance feels slow.
ALTER MATERIALIZED VIEW service_call_minute_dependency SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'caller_service_name, caller_target_name, caller_span_kind, service_name, system_type, target_name, span_kind'
    );

SELECT add_continuous_aggregate_policy('service_call_minute_dependency',
                                       start_offset => INTERVAL '10 minutes',
                                       end_offset   => INTERVAL '0 minutes',
                                       schedule_interval => INTERVAL '1 minute');

CALL add_columnstore_policy('service_call_minute_dependency',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Retention policy：keep 3 days of data
SELECT add_retention_policy('service_call_minute_dependency', INTERVAL '4 days');


-- =========================================================
-- Tier 2: Dependency daily archive
-- =========================================================
CREATE MATERIALIZED VIEW service_call_minute_dependency_merged
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '1 minute', time) as time,
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
    -- [Added]
    SUM(sum_duration) AS sum_duration,
    -- [Changed]
    ROUND(SUM(sum_duration)::numeric / NULLIF(SUM(cnt), 0), 0) AS avg_duration,
    MAX(max_duration) AS max_duration,
    MIN(min_duration) AS min_duration,
    SUM(error_num) AS error_num,
    rollup(percent_duration) AS percent_duration,
    MAX(last_insert_time) AS last_insert_time
FROM service_call_minute_dependency
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

-- [Added]Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_minute_dependency_merged OWNER TO ts_job_runner;

CREATE INDEX idx_sc_min_dep_m_caller ON service_call_minute_dependency_merged (caller_service_name, caller_span_kind, span_kind, caller_target_name, time DESC, host_name);
CREATE INDEX idx_sc_min_dep_m_callee ON service_call_minute_dependency_merged (service_name, system_type, span_kind, caller_target_name, target_name, time DESC, host_name);

SELECT set_chunk_time_interval('service_call_minute_dependency_merged', INTERVAL '1 day');

ALTER MATERIALIZED VIEW service_call_minute_dependency_merged SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'caller_service_name, caller_target_name, caller_span_kind, service_name, system_type, target_name, span_kind'
    );

SELECT add_continuous_aggregate_policy(
               'service_call_minute_dependency_merged',
               start_offset => INTERVAL '27 hours',
               end_offset   => INTERVAL '2 hours',
               schedule_interval => INTERVAL '1 day',
               initial_start => date_trunc('day', now() + interval '1 day') + interval '2 hours'
       );

CALL add_columnstore_policy('service_call_minute_dependency_merged',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '1 day',
                            initial_start     => date_trunc('day', now() + interval '1 day') + interval '3 hours');


-- Retention policy：keep 30 days of data
SELECT add_retention_policy('service_call_minute_dependency_merged', INTERVAL '30 days');


-- ==========================================
-- 2. Minute-level Dependency unified view
-- ==========================================
CREATE VIEW service_call_minute_dependency_unified AS
SELECT * FROM service_call_minute_dependency
WHERE time >= NOW() - INTERVAL '2 days'
UNION ALL
SELECT * FROM service_call_minute_dependency_merged
WHERE time < NOW() - INTERVAL '2 days';


---------------- 5 min Entry ---------------
CREATE MATERIALIZED VIEW service_call_5min_entry
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '5 minutes', time) AS time,
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
    -- [Added]
    SUM(sum_duration) as sum_duration,
    -- [Changed]Use sum/cnt
    ROUND(SUM(sum_duration)::numeric / NULLIF(SUM(cnt), 0), 0) AS avg_duration,
    MAX(max_duration) AS max_duration,
    MIN(min_duration) AS min_duration,
    SUM(error_num) AS error_num,
    rollup(percent_duration) AS percent_duration
FROM service_call_minute_entry
GROUP BY
    time_bucket(INTERVAL '5 minutes', time),
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

-- [Added]Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_5min_entry OWNER TO ts_job_runner;


CREATE INDEX idx_sc_5min_entry_svc_kind_time
    ON service_call_5min_entry (service_name, span_kind, time DESC);
-- CREATE INDEX idx_sc_5min_entry_svc_kind_time_cov
--     ON service_call_5min_entry (service_name, span_kind, time DESC)

CREATE INDEX idx_sc_5min_entry_svc_kind_tgt_time
    ON service_call_5min_entry (service_name, span_kind, target_name, time DESC);
-- CREATE INDEX idx_sc_5min_entry_svc_kind_tgt_time_cov
--     ON service_call_5min_entry (service_name, span_kind, target_name, time DESC)




SELECT set_chunk_time_interval('service_call_5min_entry', INTERVAL '2 hours');

ALTER MATERIALIZED VIEW service_call_5min_entry SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'service_name, span_kind, target_name, system_type'
    );

SELECT add_continuous_aggregate_policy('service_call_5min_entry',
                                       start_offset => INTERVAL '30 minutes',
                                       end_offset   => INTERVAL '5 minutes',
                                       schedule_interval => INTERVAL '1 minute');

-- Compression policy
CALL add_columnstore_policy('service_call_5min_entry',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- 3. Keep 18 months
SELECT add_retention_policy('service_call_5min_entry', INTERVAL '10 days');


-- =========================================================
-- [Added]Tier 2: 5-minute Entry archive merge (based on 5min_entry)
-- =========================================================
CREATE MATERIALIZED VIEW service_call_5min_entry_merged
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '5 minutes', time) as time,
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
    -- Pass through sum_duration
    SUM(sum_duration) AS sum_duration,
    -- Use exact division to calculate avg
    ROUND(SUM(sum_duration)::numeric / NULLIF(SUM(cnt), 0), 0) AS avg_duration,
    MAX(max_duration) AS max_duration,
    MIN(min_duration) AS min_duration,
    SUM(error_num) AS error_num,
    rollup(percent_duration) AS percent_duration
FROM service_call_5min_entry
GROUP BY
    time_bucket(INTERVAL '5 minutes', time),
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

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_5min_entry_merged OWNER TO ts_job_runner;

-- Follow the original index creation
CREATE INDEX idx_sc_5min_entry_m_svc_kind_time
    ON service_call_5min_entry_merged (service_name, span_kind, time DESC);

CREATE INDEX idx_sc_5min_entry_m_svc_kind_tgt_time
    ON service_call_5min_entry_merged (service_name, span_kind, target_name, time DESC);

-- 0 days per chunk
SELECT set_chunk_time_interval('service_call_5min_entry_merged', INTERVAL '7 days');

ALTER MATERIALIZED VIEW service_call_5min_entry_merged SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'service_name, span_kind, target_name, system_type'
    );

-- Refresh policy (like dailychunk, delayed calculation, archives the day-before-yesterday data once per day)
SELECT add_continuous_aggregate_policy(
               'service_call_5min_entry_merged',
               start_offset => INTERVAL '27 hours',
               end_offset   => INTERVAL '2 hours',
               schedule_interval => INTERVAL '1 day',
               initial_start => date_trunc('day', now() + interval '1 day') + interval '2 hours'
       );

-- Compression policy
CALL add_columnstore_policy('service_call_5min_entry_merged',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '1 day',
                            initial_start     => date_trunc('day', now() + interval '1 day') + interval '3 hours');

-- Keep 90 days of data
SELECT add_retention_policy('service_call_5min_entry_merged', INTERVAL '90 days');


-- ==========================================
-- 3. 5Minute-level Entry unified view
-- Boundary: 8 days (= merged chunk 7 days + compression delay 1 day)
-- ==========================================
CREATE VIEW service_call_5min_entry_unified AS
SELECT * FROM service_call_5min_entry
WHERE time >= NOW() - INTERVAL '8 days'
UNION ALL
SELECT * FROM service_call_5min_entry_merged
WHERE time < NOW() - INTERVAL '8 days';



---------------- 5 min Dependency ---------------
CREATE MATERIALIZED VIEW service_call_5min_dependency
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '5 minutes', time) AS time,
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
    -- [Added]
    SUM(sum_duration) as sum_duration,
    -- [Changed]
    ROUND(SUM(sum_duration)::numeric / NULLIF(SUM(cnt), 0), 0) AS avg_duration,
    MAX(max_duration) AS max_duration,
    MIN(min_duration) AS min_duration,
    SUM(error_num) AS error_num,
    rollup(percent_duration) AS percent_duration
FROM service_call_minute_dependency
GROUP BY
    time_bucket(INTERVAL '5 minutes', time),
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

-- [Added]Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_5min_dependency OWNER TO ts_job_runner;


CREATE INDEX idx_sc_5min_dep_caller ON service_call_5min_dependency (caller_service_name, caller_span_kind, span_kind, caller_target_name, time DESC, host_name);
CREATE INDEX idx_sc_5min_dep_callee ON service_call_5min_dependency (service_name, system_type, span_kind, caller_target_name, target_name, time DESC, host_name);



SELECT set_chunk_time_interval('service_call_5min_dependency', INTERVAL '2 hours');

ALTER MATERIALIZED VIEW service_call_5min_dependency SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'caller_service_name, caller_target_name, caller_span_kind, service_name, system_type, target_name, span_kind'
    );

SELECT add_continuous_aggregate_policy('service_call_5min_dependency',
                                       start_offset => INTERVAL '30 minutes',
                                       end_offset   => INTERVAL '5 minutes',
                                       schedule_interval => INTERVAL '1 minute');

-- Compression policy
CALL add_columnstore_policy('service_call_5min_dependency',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

SELECT add_retention_policy('service_call_5min_dependency', INTERVAL '10 days');



-- =========================================================
-- [Added]Tier 2: 5-minute Dependency archive merge (based on 5min_dependency)
-- =========================================================
CREATE MATERIALIZED VIEW service_call_5min_dependency_merged
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '5 minutes', time) as time,
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
    -- Pass through sum_duration
    SUM(sum_duration) AS sum_duration,
    -- Use exact division to calculate avg
    ROUND(SUM(sum_duration)::numeric / NULLIF(SUM(cnt), 0), 0) AS avg_duration,
    MAX(max_duration) AS max_duration,
    MIN(min_duration) AS min_duration,
    SUM(error_num) AS error_num,
    rollup(percent_duration) AS percent_duration
FROM service_call_5min_dependency
GROUP BY
    time_bucket(INTERVAL '5 minutes', time),
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

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_5min_dependency_merged OWNER TO ts_job_runner;

-- Follow the original index creation
CREATE INDEX idx_sc_5min_dep_m_caller ON service_call_5min_dependency_merged (caller_service_name, caller_span_kind, span_kind, caller_target_name, time DESC, host_name);
CREATE INDEX idx_sc_5min_dep_m_callee ON service_call_5min_dependency_merged (service_name, system_type, span_kind, caller_target_name, target_name, time DESC, host_name);

-- [Difference 1]10 days per chunk
SELECT set_chunk_time_interval('service_call_5min_dependency_merged', INTERVAL '7 days');

ALTER MATERIALIZED VIEW service_call_5min_dependency_merged SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'caller_service_name, caller_target_name, caller_span_kind, service_name, system_type, target_name, span_kind'
    );

-- Refresh policy
SELECT add_continuous_aggregate_policy(
               'service_call_5min_dependency_merged',
               start_offset => INTERVAL '27 hours',
               end_offset   => INTERVAL '2 hours',
               schedule_interval => INTERVAL '1 day',
               initial_start => date_trunc('day', now() + interval '1 day') + interval '2 hours'
       );

-- Compression policy
CALL add_columnstore_policy('service_call_5min_dependency_merged',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '1 day',
                            initial_start     => date_trunc('day', now() + interval '1 day') + interval '3 hours');

-- Keep 90 days of data
SELECT add_retention_policy('service_call_5min_dependency_merged', INTERVAL '90 days');


-- ==========================================
-- 4. 5Minute-level Dependency unified view
-- ==========================================
CREATE VIEW service_call_5min_dependency_unified AS
SELECT * FROM service_call_5min_dependency
WHERE time >= NOW() - INTERVAL '8 days'
UNION ALL
SELECT * FROM service_call_5min_dependency_merged
WHERE time < NOW() - INTERVAL '8 days';


-- =========================================================
-- Tier 2: Entry hourly aggregate
-- =========================================================

CREATE MATERIALIZED VIEW service_call_hour_entry
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '1 hour', time) AS time,
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
    -- [Added]
    SUM(sum_duration) as sum_duration,
    -- [Changed]
    ROUND(SUM(sum_duration)::numeric / NULLIF(SUM(cnt), 0), 0) AS avg_duration,
    MAX(max_duration) AS max_duration,
    MIN(min_duration) AS min_duration,
    SUM(error_num) AS error_num,
    rollup(percent_duration) AS percent_duration
FROM service_call_5min_entry
GROUP BY
    time_bucket(INTERVAL '1 hour', time),
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

-- [Added]Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_hour_entry OWNER TO ts_job_runner;


CREATE INDEX idx_sc_hour_entry_svc_kind_time
    ON service_call_hour_entry (service_name, span_kind, time DESC);
-- CREATE INDEX idx_sc_hour_entry_svc_kind_time_cov
--     ON service_call_hour_entry (service_name, span_kind, time DESC)

CREATE INDEX idx_sc_hour_entry_svc_kind_tgt_time
    ON service_call_hour_entry (service_name, span_kind, target_name, time DESC);
-- CREATE INDEX idx_sc_hour_entry_svc_kind_tgt_time_cov
--     ON service_call_hour_entry (service_name, span_kind, target_name, time DESC)




SELECT set_chunk_time_interval('service_call_hour_entry', INTERVAL '1 day');

ALTER MATERIALIZED VIEW service_call_hour_entry SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'service_name, span_kind, target_name, system_type'
    );

-- Refresh policy (unchanged)
SELECT add_continuous_aggregate_policy('service_call_hour_entry',
                                       start_offset => INTERVAL '6 hours',
                                       end_offset   => INTERVAL '3 hours',
                                       schedule_interval => INTERVAL '30 minutes');

-- Compression policy
CALL add_columnstore_policy('service_call_hour_entry',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Retention policy：Keep 35 days
SELECT add_retention_policy('service_call_hour_entry', INTERVAL '20 days');


-- =========================================================
-- [Added]Tier 2: Hour Entry archive merge (based on service_call_hour_entry)
-- =========================================================
CREATE MATERIALIZED VIEW service_call_hour_entry_merged
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '1 hour', time) as time,
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
    -- Pass through sum_duration
    SUM(sum_duration) AS sum_duration,
    -- Use exact division to calculate avg
    ROUND(SUM(sum_duration)::numeric / NULLIF(SUM(cnt), 0), 0) AS avg_duration,
    MAX(max_duration) AS max_duration,
    MIN(min_duration) AS min_duration,
    SUM(error_num) AS error_num,
    rollup(percent_duration) AS percent_duration
FROM service_call_hour_entry
GROUP BY
    time_bucket(INTERVAL '1 hour', time),
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

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_hour_entry_merged OWNER TO ts_job_runner;

-- Follow the original index creation
CREATE INDEX idx_sc_hour_entry_m_svc_kind_time
    ON service_call_hour_entry_merged (service_name, span_kind, time DESC);

CREATE INDEX idx_sc_hour_entry_m_svc_kind_tgt_time
    ON service_call_hour_entry_merged (service_name, span_kind, target_name, time DESC);

-- [Difference 1]30 days per chunk
SELECT set_chunk_time_interval('service_call_hour_entry_merged', INTERVAL '15 days');

ALTER MATERIALIZED VIEW service_call_hour_entry_merged SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'service_name, span_kind, target_name, system_type'
    );

-- Refresh policy (like dailychunk, delayed calculation, archives the day-before-yesterday data once per day)
SELECT add_continuous_aggregate_policy(
               'service_call_hour_entry_merged',
               start_offset => INTERVAL '27 hours',
               end_offset   => INTERVAL '2 hours',
               schedule_interval => INTERVAL '1 day',
               initial_start => date_trunc('day', now() + interval '1 day') + interval '2 hours'
       );

-- Compression policy
CALL add_columnstore_policy('service_call_hour_entry_merged',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '1 day',
                            initial_start     => date_trunc('day', now() + interval '1 day') + interval '3 hours');

-- Keep 400 days of data
SELECT add_retention_policy('service_call_hour_entry_merged', INTERVAL '400 days');



-- ==========================================
-- 5. Hourly Entry unified view
-- Boundary: 16 days (= merged chunk 15 days + compression delay 1 day)
-- ==========================================
CREATE VIEW service_call_hour_entry_unified AS
SELECT * FROM service_call_hour_entry
WHERE time >= NOW() - INTERVAL '16 days'
UNION ALL
SELECT * FROM service_call_hour_entry_merged
WHERE time < NOW() - INTERVAL '16 days';


-- =========================================================
-- Tier 2: Dependency hourly aggregate
-- =========================================================
CREATE MATERIALIZED VIEW service_call_hour_dependency
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '1 hour', time) AS time,
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
    -- [Added]
    SUM(sum_duration) as sum_duration,
    -- [Changed]
    ROUND(SUM(sum_duration)::numeric / NULLIF(SUM(cnt), 0), 0) AS avg_duration,
    MAX(max_duration) AS max_duration,
    MIN(min_duration) AS min_duration,
    SUM(error_num) AS error_num,
    rollup(percent_duration) AS percent_duration
FROM service_call_5min_dependency
GROUP BY
    time_bucket(INTERVAL '1 hour', time),
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

-- [Added]Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_hour_dependency OWNER TO ts_job_runner;

CREATE INDEX idx_sc_hour_dep_caller ON service_call_hour_dependency (caller_service_name, caller_span_kind, span_kind, caller_target_name, time DESC, host_name);
CREATE INDEX idx_sc_hour_dep_callee ON service_call_hour_dependency (service_name, system_type, span_kind, caller_target_name, target_name, time DESC, host_name);



SELECT set_chunk_time_interval('service_call_hour_dependency', INTERVAL '1 day');

ALTER MATERIALIZED VIEW service_call_hour_dependency SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'caller_service_name, caller_target_name, caller_span_kind, service_name, system_type, target_name, span_kind'
    );

-- Refresh policy (unchanged)
SELECT add_continuous_aggregate_policy('service_call_hour_dependency',
                                       start_offset => INTERVAL '6 hours',
                                       end_offset   => INTERVAL '3 hours',
                                       schedule_interval => INTERVAL '30 minutes');

-- Compression policy
CALL add_columnstore_policy('service_call_hour_dependency',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- [Change 2]Retention policy：35 days
SELECT add_retention_policy('service_call_hour_dependency', INTERVAL '20 days');

-- =========================================================
-- [Added]Tier 2: Hour Dependency archive merge (based on service_call_hour_dependency)
-- =========================================================
CREATE MATERIALIZED VIEW service_call_hour_dependency_merged
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '1 hour', time) as time,
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
    -- Pass through sum_duration
    SUM(sum_duration) AS sum_duration,
    -- Use exact division to calculate avg
    ROUND(SUM(sum_duration)::numeric / NULLIF(SUM(cnt), 0), 0) AS avg_duration,
    MAX(max_duration) AS max_duration,
    MIN(min_duration) AS min_duration,
    SUM(error_num) AS error_num,
    rollup(percent_duration) AS percent_duration
FROM service_call_hour_dependency
GROUP BY
    time_bucket(INTERVAL '1 hour', time),
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

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_hour_dependency_merged OWNER TO ts_job_runner;

-- Follow the original index creation
CREATE INDEX idx_sc_hour_dep_m_caller ON service_call_hour_dependency_merged (caller_service_name, caller_span_kind, span_kind, caller_target_name, time DESC, host_name);
CREATE INDEX idx_sc_hour_dep_m_callee ON service_call_hour_dependency_merged (service_name, system_type, span_kind, caller_target_name, target_name, time DESC, host_name);

-- [Difference 1]30 days per chunk
SELECT set_chunk_time_interval('service_call_hour_dependency_merged', INTERVAL '15 days');

ALTER MATERIALIZED VIEW service_call_hour_dependency_merged SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'caller_service_name, caller_target_name, caller_span_kind, service_name, system_type, target_name, span_kind'
    );

-- Refresh policy (like dailychunk, delayed calculation, archives the day-before-yesterday data once per day)
SELECT add_continuous_aggregate_policy(
               'service_call_hour_dependency_merged',
               start_offset => INTERVAL '27 hours',
               end_offset   => INTERVAL '2 hours',
               schedule_interval => INTERVAL '1 day',
               initial_start => date_trunc('day', now() + interval '1 day') + interval '2 hours'
       );

-- Compression policy
CALL add_columnstore_policy('service_call_hour_dependency_merged',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '1 day',
                            initial_start     => date_trunc('day', now() + interval '1 day') + interval '3 hours');

-- [Difference 2]Keep 400 days of data
SELECT add_retention_policy('service_call_hour_dependency_merged', INTERVAL '400 days');


-- ==========================================
-- 6. Hourly Dependency unified view
-- ==========================================
CREATE VIEW service_call_hour_dependency_unified AS
SELECT * FROM service_call_hour_dependency
WHERE time >= NOW() - INTERVAL '16 days'
UNION ALL
SELECT * FROM service_call_hour_dependency_merged
WHERE time < NOW() - INTERVAL '16 days';


-- =========================================================
-- Tier 3: Entry daily aggregate
-- =========================================================

CREATE MATERIALIZED VIEW service_call_day_entry
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '1 day', time) AS time,
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
    -- [Added]
    SUM(sum_duration) as sum_duration,
    -- [Changed]
    ROUND(SUM(sum_duration)::numeric / NULLIF(SUM(cnt), 0), 0) AS avg_duration,
    MAX(max_duration) AS max_duration,
    MIN(min_duration) AS min_duration,
    SUM(error_num) AS error_num,
    rollup(percent_duration) AS percent_duration
FROM service_call_hour_entry
GROUP BY
    time_bucket(INTERVAL '1 day', time),
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

-- [Added]Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_day_entry OWNER TO ts_job_runner;


CREATE INDEX idx_sc_day_entry_svc_kind_time
    ON service_call_day_entry (service_name, span_kind, time DESC);
-- CREATE INDEX idx_sc_day_entry_svc_kind_time_cov
--     ON service_call_day_entry (service_name, span_kind, time DESC)

CREATE INDEX idx_sc_day_entry_svc_kind_tgt_time
    ON service_call_day_entry (service_name, span_kind, target_name, time DESC);
-- CREATE INDEX idx_sc_day_entry_svc_kind_tgt_time_cov
--     ON service_call_day_entry (service_name, span_kind, target_name, time DESC)




-- [Change 1]Set chunk size to 30 days
SELECT set_chunk_time_interval('service_call_day_entry', INTERVAL '30 days');

ALTER MATERIALIZED VIEW service_call_day_entry SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'service_name, span_kind, target_name, system_type'
    );

-- Refresh policy (unchanged：refresh daily and calculate data from 3 days ago to 1 day ago)
SELECT add_continuous_aggregate_policy('service_call_day_entry',
                                       start_offset => INTERVAL '3 days',
                                       end_offset   => INTERVAL '1 day',
                                       schedule_interval => INTERVAL '1 day');

-- Compression policy
CALL add_columnstore_policy('service_call_day_entry',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Retention policy：keep up to 1100 days (previously permanent because no policy was added)
SELECT add_retention_policy('service_call_day_entry', INTERVAL '1100 days');


-- =========================================================
-- Tier 3: Dependency daily aggregate
-- =========================================================

CREATE MATERIALIZED VIEW service_call_day_dependency
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '1 day', time) AS time,
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
    -- [Added]
    SUM(sum_duration) as sum_duration,
    -- [Changed]
    ROUND(SUM(sum_duration)::numeric / NULLIF(SUM(cnt), 0), 0) AS avg_duration,
    MAX(max_duration) AS max_duration,
    MIN(min_duration) AS min_duration,
    SUM(error_num) AS error_num,
    rollup(percent_duration) AS percent_duration
FROM service_call_hour_dependency
GROUP BY
    time_bucket(INTERVAL '1 day', time),
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

-- [Added]Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_day_dependency OWNER TO ts_job_runner;



CREATE INDEX idx_sc_day_dep_caller ON service_call_day_dependency (caller_service_name, caller_span_kind, span_kind, caller_target_name, time DESC, host_name);
CREATE INDEX idx_sc_day_dep_callee ON service_call_day_dependency (service_name, system_type, span_kind, caller_target_name, target_name, time DESC, host_name);


-- [Change 1]Set chunk size to 30 days
SELECT set_chunk_time_interval('service_call_day_dependency', INTERVAL '30 days');

ALTER MATERIALIZED VIEW service_call_day_dependency SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'caller_service_name, caller_target_name, caller_span_kind, service_name, system_type, target_name, span_kind'
    );

-- Refresh policy (unchanged)
SELECT add_continuous_aggregate_policy('service_call_day_dependency',
                                       start_offset => INTERVAL '3 days',
                                       end_offset   => INTERVAL '1 day',
                                       schedule_interval => INTERVAL '1 day');

-- Compression policy
CALL add_columnstore_policy('service_call_day_dependency',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Retention policy：keep up to 1100 days
SELECT add_retention_policy('service_call_day_dependency', INTERVAL '1100 days');

-- =========================================================
-- Host Name List start
-- =========================================================
-- 1. Host List Minute (1-minute level)
CREATE MATERIALIZED VIEW service_host_list_minute_entry
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '1 minute', time) AS time,
    service_name,
    host_name,
    span_kind,
    count(*) as cnt
FROM service_call
WHERE span_kind IN (2, 5)
GROUP BY
    time_bucket(INTERVAL '1 minute', time),
    service_name,
    host_name,
    span_kind
WITH NO DATA;

ALTER MATERIALIZED VIEW service_host_list_minute_entry OWNER TO ts_job_runner;

CREATE INDEX idx_shl_min_entry_svc_kind_time_inc_host
    ON service_host_list_minute_entry (service_name, span_kind, time DESC)
    INCLUDE (host_name);

SELECT set_chunk_time_interval('service_host_list_minute_entry', INTERVAL '1 day');

-- Placing span_kind first helps when a specific span_kind is specified.
ALTER MATERIALIZED VIEW service_host_list_minute_entry SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'service_name, span_kind, host_name'
    );

SELECT add_continuous_aggregate_policy('service_host_list_minute_entry',
                                       start_offset => INTERVAL '10 minutes',
                                       end_offset   => INTERVAL '0 minutes',
                                       schedule_interval => INTERVAL '1 minute');

CALL add_columnstore_policy('service_host_list_minute_entry',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- retention is 30 days
SELECT add_retention_policy('service_host_list_minute_entry', INTERVAL '30 days');




-- 2. Host List 5 Minutes (5-minute level)
CREATE MATERIALIZED VIEW service_host_list_5min_entry
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '5 minutes', time) AS time,
    service_name,
    host_name,
    span_kind,
    SUM(cnt) as cnt
FROM service_host_list_minute_entry
GROUP BY
    time_bucket(INTERVAL '5 minutes', time),
    service_name,
    host_name,
    span_kind
WITH NO DATA;

ALTER MATERIALIZED VIEW service_host_list_5min_entry OWNER TO ts_job_runner;

CREATE INDEX idx_shl_5min_entry_svc_kind_time_inc_host
    ON service_host_list_5min_entry (service_name, span_kind, time DESC)
    INCLUDE (host_name);

SELECT set_chunk_time_interval('service_host_list_5min_entry', INTERVAL '10 days');

ALTER MATERIALIZED VIEW service_host_list_5min_entry SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'service_name, span_kind, host_name'
    );

SELECT add_continuous_aggregate_policy('service_host_list_5min_entry',
                                       start_offset => INTERVAL '30 minutes',
                                       end_offset   => INTERVAL '5 minutes',
                                       schedule_interval => INTERVAL '1 minute');

CALL add_columnstore_policy('service_host_list_5min_entry',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- retention is 90 days (previously INTERVAL '18 months')
SELECT add_retention_policy('service_host_list_5min_entry', INTERVAL '90 days');


-- 3. Host List Hour (hourly level)
CREATE MATERIALIZED VIEW service_host_list_hour_entry
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '1 hour', time) AS time,
    service_name,
    host_name,
    span_kind,
    SUM(cnt) as cnt
FROM service_host_list_5min_entry
GROUP BY
    time_bucket(INTERVAL '1 hour', time),
    service_name,
    host_name,
    span_kind
WITH NO DATA;

ALTER MATERIALIZED VIEW service_host_list_hour_entry OWNER TO ts_job_runner;

CREATE INDEX idx_shl_hour_entry_svc_kind_time_inc_host
    ON service_host_list_hour_entry (service_name, span_kind, time DESC)
    INCLUDE (host_name);

-- [Aligned with Entry]Chunk = 7 days
SELECT set_chunk_time_interval('service_host_list_hour_entry', INTERVAL '60 days');

ALTER MATERIALIZED VIEW service_host_list_hour_entry SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'service_name, span_kind, host_name'
    );

SELECT add_continuous_aggregate_policy('service_host_list_hour_entry',
                                       start_offset => INTERVAL '6 hours',
                                       end_offset   => INTERVAL '1 hour',
                                       schedule_interval => INTERVAL '30 minutes');

-- [Aligned with Entry]
-- after = 60 minutes
-- schedule_interval = 1 day (removed initial_start)
CALL add_columnstore_policy('service_host_list_hour_entry',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- retention is 400 days
SELECT add_retention_policy('service_host_list_hour_entry', INTERVAL '400 days');

-- 4. Host List Day (daily level)
CREATE MATERIALIZED VIEW service_host_list_day_entry
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '1 day', time) AS time,
    service_name,
    host_name,
    span_kind,
    SUM(cnt) as cnt
FROM service_host_list_hour_entry
GROUP BY
    time_bucket(INTERVAL '1 day', time),
    service_name,
    host_name,
    span_kind
WITH NO DATA;

ALTER MATERIALIZED VIEW service_host_list_day_entry OWNER TO ts_job_runner;

CREATE INDEX idx_shl_day_entry_svc_kind_time_inc_host
    ON service_host_list_day_entry (service_name, span_kind, time DESC)
    INCLUDE (host_name);

-- [Aligned with Entry]Chunk = 30 days
SELECT set_chunk_time_interval('service_host_list_day_entry', INTERVAL '90 days');

ALTER MATERIALIZED VIEW service_host_list_day_entry SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'service_name, span_kind, host_name'
    );

SELECT add_continuous_aggregate_policy('service_host_list_day_entry',
                                       start_offset => INTERVAL '3 days',
                                       end_offset   => INTERVAL '1 day',
                                       schedule_interval => INTERVAL '1 day');

-- [Aligned with Entry]
-- after = 60 minutes
-- schedule_interval = 1 day (removed initial_start)
CALL add_columnstore_policy('service_host_list_day_entry',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- [Changed]retention is 1100 days
SELECT add_retention_policy('service_host_list_day_entry', INTERVAL '1100 days');

-- =========================================================
-- Host Name List end
-- =========================================================


-- =========================================================
-- Topology Relation start
-- =========================================================


-- 1. Create the topology aggregate for the Entry side
CREATE MATERIALIZED VIEW service_topology_relation_minute_entry
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '1 minute', time) AS time,
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name,
    SUM(cnt) as cnt,
    SUM(sum_duration) as sum_duration,
    ROUND(SUM(sum_duration)::numeric / NULLIF(SUM(cnt), 0), 0) AS avg_duration,
    SUM(error_num) as error_num
FROM service_call_minute_entry
GROUP BY
    time_bucket(INTERVAL '1 minute', time),
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name
WITH NO DATA;

-- [Added]Change view owner to the job user
ALTER MATERIALIZED VIEW service_topology_relation_minute_entry OWNER TO ts_job_runner;

-- Policy and indexes
-- end_off is the same as service_call_minute_entry
SELECT add_continuous_aggregate_policy('service_topology_relation_minute_entry',
                                       start_offset => INTERVAL '10 minutes',
                                       end_offset => INTERVAL '0 minute',
                                       schedule_interval => INTERVAL '1 minute');

ALTER MATERIALIZED VIEW service_topology_relation_minute_entry set (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'caller_service_name, span_kind, system_type',
    timescaledb.orderby = 'time DESC, service_name'
    );

SELECT set_chunk_time_interval('service_topology_relation_minute_entry', INTERVAL '2 hours');

CREATE INDEX idx_str_min_entry_kind_caller_svc_type_time ON service_topology_relation_minute_entry
    (span_kind, caller_service_name, service_name, system_type, time DESC);

CALL add_columnstore_policy('service_topology_relation_minute_entry',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Keep 30 days
SELECT add_retention_policy('service_topology_relation_minute_entry', INTERVAL '30 days');




-- 2. Create the topology aggregate for the Dependency side
CREATE MATERIALIZED VIEW service_topology_relation_minute_dependency
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '1 minute', time) AS time,
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name,
    SUM(cnt) as cnt,
    SUM(sum_duration) as sum_duration,
    ROUND(SUM(sum_duration)::numeric / NULLIF(SUM(cnt), 0), 0) AS avg_duration,
    SUM(error_num) as error_num
FROM service_call_minute_dependency
GROUP BY
    time_bucket(INTERVAL '1 minute', time),
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name
WITH NO DATA;

-- [Added]Change view owner to the job user
ALTER MATERIALIZED VIEW service_topology_relation_minute_dependency OWNER TO ts_job_runner;

-- Policy and indexes
SELECT add_continuous_aggregate_policy('service_topology_relation_minute_dependency',
                                       start_offset => INTERVAL '10 minutes',
                                       end_offset => INTERVAL '0 minute',
                                       schedule_interval => INTERVAL '1 minute');

ALTER MATERIALIZED VIEW service_topology_relation_minute_dependency set (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'caller_service_name, system_type, span_kind',
    timescaledb.orderby = 'time DESC, remote_addr, service_name'
    );

SELECT set_chunk_time_interval('service_topology_relation_minute_dependency', INTERVAL '2 hours');

-- The index field order differs slightly from the entry table; system_type and span_kind are swapped mainly because of query conditions.
CREATE INDEX idx_str_min_dep_type_kind_caller_svc_time ON service_topology_relation_minute_dependency
    (system_type, span_kind, caller_service_name, service_name, time DESC);


CALL add_columnstore_policy('service_topology_relation_minute_dependency',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Keep 30 days
SELECT add_retention_policy('service_topology_relation_minute_dependency', INTERVAL '30 days');



-- 3. Create the unified view (keep the original external table name)
-- The span_kind constraint lets the optimizer fully prune the other branch when querying a specific span_kind, avoiding unnecessary scans
CREATE OR REPLACE VIEW service_topology_relation_minute AS
SELECT
    time,
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name,
    cnt,
    avg_duration,
    error_num
FROM service_topology_relation_minute_entry
WHERE span_kind IN (2, 5)
UNION ALL
SELECT
    time,
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name,
    cnt,
    avg_duration,
    error_num
FROM service_topology_relation_minute_dependency
WHERE span_kind IN (1, 3, 4);






-- >>> Create topology 5-minute aggregate start

-- =========================================================
-- [Added]Topology Relation 5-minute aggregate (Entry side)
-- =========================================================
CREATE MATERIALIZED VIEW service_topology_relation_5min_entry
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '5 minutes', time) AS time,
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name,
    SUM(cnt) as cnt,
    SUM(sum_duration) as sum_duration,
    ROUND(SUM(sum_duration)::numeric / NULLIF(SUM(cnt), 0), 0) AS avg_duration,
    SUM(error_num) as error_num
FROM service_topology_relation_minute_entry
GROUP BY
    time_bucket(INTERVAL '5 minutes', time),
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name
WITH NO DATA;

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_topology_relation_5min_entry OWNER TO ts_job_runner;

-- [Requirement]7 days per chunk
SELECT set_chunk_time_interval('service_topology_relation_5min_entry', INTERVAL '1 day');

-- Compression settings
ALTER MATERIALIZED VIEW service_topology_relation_5min_entry SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'caller_service_name, span_kind, system_type',
    timescaledb.orderby = 'time DESC, service_name'
    );

-- Refresh policy: like the 5-minute view, refresh every 1 minute and calculate data from 30 minutes ago to 5 minutes ago
SELECT add_continuous_aggregate_policy('service_topology_relation_5min_entry',
                                       start_offset => INTERVAL '30 minutes',
                                       end_offset   => INTERVAL '5 minutes',
                                       schedule_interval => INTERVAL '1 minute');

-- Indexes
CREATE INDEX idx_str_5min_entry_kind_caller_svc_type_time ON service_topology_relation_5min_entry
    (span_kind, caller_service_name, service_name, system_type, time DESC);

-- Compression policy
CALL add_columnstore_policy('service_topology_relation_5min_entry',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- [Requirement]Keep 90 days
SELECT add_retention_policy('service_topology_relation_5min_entry', INTERVAL '90 days');



-- =========================================================
-- [Added]Topology Relation 5-minute aggregate (Dependency side)
-- =========================================================
CREATE MATERIALIZED VIEW service_topology_relation_5min_dependency
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '5 minutes', time) AS time,
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name,
    SUM(cnt) as cnt,
    SUM(sum_duration) as sum_duration,
    ROUND(SUM(sum_duration)::numeric / NULLIF(SUM(cnt), 0), 0) AS avg_duration,
    SUM(error_num) as error_num
FROM service_topology_relation_minute_dependency
GROUP BY
    time_bucket(INTERVAL '5 minutes', time),
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name
WITH NO DATA;

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_topology_relation_5min_dependency OWNER TO ts_job_runner;

-- [Requirement]7 days per chunk
SELECT set_chunk_time_interval('service_topology_relation_5min_dependency', INTERVAL '1 day');

-- Compression settings
ALTER MATERIALIZED VIEW service_topology_relation_5min_dependency SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'caller_service_name, system_type, span_kind',
    timescaledb.orderby = 'time DESC, remote_addr, service_name'
    );

-- Refresh policy
SELECT add_continuous_aggregate_policy('service_topology_relation_5min_dependency',
                                       start_offset => INTERVAL '30 minutes',
                                       end_offset   => INTERVAL '5 minutes',
                                       schedule_interval => INTERVAL '1 minute');

-- Indexes: (note that the system_type and span_kind order differs from the entry table, matching the original design)
CREATE INDEX idx_str_5min_dep_type_kind_caller_svc_time ON service_topology_relation_5min_dependency
    (system_type, span_kind, caller_service_name, service_name, time DESC);

-- Compression policy
CALL add_columnstore_policy('service_topology_relation_5min_dependency',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- [Requirement]Keep 90 days
SELECT add_retention_policy('service_topology_relation_5min_dependency', INTERVAL '90 days');

-- =========================================================
-- [Added]Create the topology 5-minute unified view (external interface)
-- =========================================================
-- The span_kind constraint lets the optimizer fully prune the other branch when querying a specific span_kind, avoiding unnecessary scans
CREATE OR REPLACE VIEW service_topology_relation_5min AS
SELECT
    time,
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name,
    cnt,
    avg_duration,
    error_num
FROM service_topology_relation_5min_entry
WHERE span_kind IN (2, 5)
UNION ALL
SELECT
    time,
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name,
    cnt,
    avg_duration,
    error_num
FROM service_topology_relation_5min_dependency
WHERE span_kind IN (1, 3, 4);




-- >>> Create topology Hour aggregate start

-- =========================================================
-- 1. Create the Hour aggregate for the Entry side
--    Source data: service_topology_relation_5min_entry
-- =========================================================
CREATE MATERIALIZED VIEW service_topology_relation_hour_entry
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '1 hour', time) AS time,
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name,
    SUM(cnt) as cnt,
    SUM(sum_duration) as sum_duration,
    ROUND(SUM(sum_duration)::numeric / NULLIF(SUM(cnt), 0), 0) AS avg_duration,
    SUM(error_num) as error_num
FROM service_topology_relation_5min_entry
GROUP BY
    time_bucket(INTERVAL '1 hour', time),
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name
WITH NO DATA;

-- [Added]Change view owner to the job user
ALTER MATERIALIZED VIEW service_topology_relation_hour_entry OWNER TO ts_job_runner;

-- Chunk: set to 14 days
SELECT set_chunk_time_interval('service_topology_relation_hour_entry', INTERVAL '14 days');

-- Compression settings
ALTER MATERIALIZED VIEW service_topology_relation_hour_entry SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'caller_service_name, span_kind, system_type',
    timescaledb.orderby = 'time DESC, service_name'
    );

-- Refresh policy: refresh every 30 minutes; the calculation window is from 6 hours ago to 1 hour ago
SELECT add_continuous_aggregate_policy('service_topology_relation_hour_entry',
                                       start_offset => INTERVAL '6 hours',
                                       end_offset   => INTERVAL '1 hour',
                                       schedule_interval => INTERVAL '30 minutes');

-- Indexes: manually create covering indexes
CREATE INDEX idx_str_hour_entry_kind_caller_svc_type_time
    ON service_topology_relation_hour_entry
        (span_kind, caller_service_name, service_name, system_type, time DESC);


-- Compression policy
CALL add_columnstore_policy('service_topology_relation_hour_entry',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Retention policy: [Changed]Keep 400 days
SELECT add_retention_policy('service_topology_relation_hour_entry', INTERVAL '400 days');


-- =========================================================
-- 2. Create the Hour aggregate for the Dependency side
--    Source data: service_topology_relation_5min_dependency
-- =========================================================
CREATE MATERIALIZED VIEW service_topology_relation_hour_dependency
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '1 hour', time) AS time,
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name,
    SUM(cnt) as cnt,
    SUM(sum_duration) as sum_duration,
    ROUND(SUM(sum_duration)::numeric / NULLIF(SUM(cnt), 0), 0) AS avg_duration,
    SUM(error_num) as error_num
FROM service_topology_relation_5min_dependency
GROUP BY
    time_bucket(INTERVAL '1 hour', time),
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name
WITH NO DATA;

-- [Added]Change view owner to the job user
ALTER MATERIALIZED VIEW service_topology_relation_hour_dependency OWNER TO ts_job_runner;

-- Chunk: set to 14 days
SELECT set_chunk_time_interval('service_topology_relation_hour_dependency', INTERVAL '14 days');

-- Compression settings
ALTER MATERIALIZED VIEW service_topology_relation_hour_dependency SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'caller_service_name, system_type, span_kind',
    timescaledb.orderby = 'time DESC, remote_addr, service_name'
    );

-- Refresh policy: refresh every 30 minutes
SELECT add_continuous_aggregate_policy('service_topology_relation_hour_dependency',
                                       start_offset => INTERVAL '6 hours',
                                       end_offset   => INTERVAL '1 hour',
                                       schedule_interval => INTERVAL '30 minutes');

-- Indexes: manually create covering indexes
CREATE INDEX idx_str_hour_dep_type_kind_caller_svc_time
    ON service_topology_relation_hour_dependency
        (system_type, span_kind, caller_service_name, service_name, time DESC);

-- Compression policy
CALL add_columnstore_policy('service_topology_relation_hour_dependency',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Retention policy: Keep 400 days
SELECT add_retention_policy('service_topology_relation_hour_dependency', INTERVAL '400 days');


-- =========================================================
-- 3. Create the unified view (external interface)
-- =========================================================
-- The span_kind constraint lets the optimizer fully prune the other branch when querying a specific span_kind, avoiding unnecessary scans
CREATE OR REPLACE VIEW service_topology_relation_hour AS
SELECT
    time,
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name,
    cnt,
    avg_duration,
    error_num
FROM service_topology_relation_hour_entry
WHERE span_kind IN (2, 5)
UNION ALL
SELECT
    time,
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name,
    cnt,
    avg_duration,
    error_num
FROM service_topology_relation_hour_dependency
WHERE span_kind IN (1, 3, 4);

-- >>> Create topology Hour aggregate end








-- >>> Create topology Day aggregate start

-- =========================================================
-- 1. Create the Day aggregate for the Entry side
--    Source data: service_topology_relation_hour_entry
-- =========================================================
CREATE MATERIALIZED VIEW service_topology_relation_day_entry
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '1 day', time) AS time,
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name,
    SUM(cnt) as cnt,
    SUM(sum_duration) as sum_duration,
    ROUND(SUM(sum_duration)::numeric / NULLIF(SUM(cnt), 0), 0) AS avg_duration,
    SUM(error_num) as error_num
FROM service_topology_relation_hour_entry
GROUP BY
    time_bucket(INTERVAL '1 day', time),
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name
WITH NO DATA;

-- [Added]Change view owner to the job user
ALTER MATERIALIZED VIEW service_topology_relation_day_entry OWNER TO ts_job_runner;

-- Chunk: set to 30 days
SELECT set_chunk_time_interval('service_topology_relation_day_entry', INTERVAL '30 days');

-- Compression settings
ALTER MATERIALIZED VIEW service_topology_relation_day_entry SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'caller_service_name, span_kind, system_type',
    timescaledb.orderby = 'time DESC, service_name'
    );

-- Refresh policy: run once per day and calculate data from 3 days ago to 1 day ago
SELECT add_continuous_aggregate_policy('service_topology_relation_day_entry',
                                       start_offset => INTERVAL '3 days',
                                       end_offset   => INTERVAL '1 day',
                                       schedule_interval => INTERVAL '1 day');

-- Indexes: manually create covering indexes
CREATE INDEX idx_str_day_entry_kind_caller_svc_type_time
    ON service_topology_relation_day_entry
        (span_kind, caller_service_name, service_name, system_type, time DESC);

-- Compression policy
CALL add_columnstore_policy('service_topology_relation_day_entry',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Retention policy: Keep 1100 days
SELECT add_retention_policy('service_topology_relation_day_entry', INTERVAL '1100 days');


-- =========================================================
-- 2. Create the Day aggregate for the Dependency side
--    Source data: service_topology_relation_hour_dependency
-- =========================================================
CREATE MATERIALIZED VIEW service_topology_relation_day_dependency
            WITH (timescaledb.continuous, timescaledb.create_group_indexes=false) AS
SELECT
    time_bucket(INTERVAL '1 day', time) AS time,
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name,
    SUM(cnt) as cnt,
    SUM(sum_duration) as sum_duration,
    ROUND(SUM(sum_duration)::numeric / NULLIF(SUM(cnt), 0), 0) AS avg_duration,
    SUM(error_num) as error_num
FROM service_topology_relation_hour_dependency
GROUP BY
    time_bucket(INTERVAL '1 day', time),
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name
WITH NO DATA;

-- [Added]Change view owner to the job user
ALTER MATERIALIZED VIEW service_topology_relation_day_dependency OWNER TO ts_job_runner;

-- Chunk: set to 30 days
SELECT set_chunk_time_interval('service_topology_relation_day_dependency', INTERVAL '30 days');

-- Compression settings
ALTER MATERIALIZED VIEW service_topology_relation_day_dependency SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'caller_service_name, system_type, span_kind',
    timescaledb.orderby = 'time DESC, remote_addr, service_name'
    );

-- Refresh policy: run once per day
SELECT add_continuous_aggregate_policy('service_topology_relation_day_dependency',
                                       start_offset => INTERVAL '3 days',
                                       end_offset   => INTERVAL '1 day',
                                       schedule_interval => INTERVAL '1 day');

-- Indexes: manually create covering indexes
CREATE INDEX idx_str_day_dep_type_kind_caller_svc_time
    ON service_topology_relation_day_dependency
        (system_type, span_kind, caller_service_name, service_name, time DESC);

-- Compression policy
CALL add_columnstore_policy('service_topology_relation_day_dependency',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Retention policy: Keep 1100 days
SELECT add_retention_policy('service_topology_relation_day_dependency', INTERVAL '1100 days');





-- =========================================================
-- 3. Create the unified view (external interface)
-- =========================================================
-- The span_kind constraint lets the optimizer fully prune the other branch when querying a specific span_kind, avoiding unnecessary scans
CREATE OR REPLACE VIEW service_topology_relation_day AS
SELECT
    time,
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name,
    cnt,
    avg_duration,
    error_num
FROM service_topology_relation_day_entry
WHERE span_kind IN (2, 5)
UNION ALL
SELECT
    time,
    service_name,
    system_type,
    span_kind,
    remote_addr,
    caller_service_name,
    cnt,
    avg_duration,
    error_num
FROM service_topology_relation_day_dependency
WHERE span_kind IN (1, 3, 4);

-- >>> Create topology Day aggregate end

-- =========================================================
-- Topology Relation end
-- =========================================================






























-- ---------------------------------------------------------------------------------------------------------------------------
-- >>> Create alert rule table start
CREATE TABLE "topology_warning_rules"
(
    "id"            SERIAL                                      NOT NULL,
    "rule_name"     varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
    "rule_details"  json                                        NOT NULL,
    "user_id"       int4                                        NOT NULL,
    "enabled"       int4 DEFAULT 0,
    "service_name"  varchar(255) COLLATE "pg_catalog"."default",
    "created_at"    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    "updated_at"    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    "warning_level" int4 DEFAULT 1,
    PRIMARY KEY ("id") -- Use id as the primary key
);
-- >>> Create alert rule table end

-- >>> Create permission-related tables start
-- 1. Entity tables

-- User table (users)
CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       username VARCHAR(255) NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       enabled BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE users IS 'User table';
COMMENT ON COLUMN users.id IS 'Primary key ID';
COMMENT ON COLUMN users.username IS 'Username';
COMMENT ON COLUMN users.password IS 'Password (should store a hash value)';
COMMENT ON COLUMN users.enabled IS 'Whether the account is enabled';
COMMENT ON COLUMN users.created_at IS 'Record creation time';
COMMENT ON COLUMN users.updated_at IS 'Record last update time';


-- Role table (roles)
CREATE TABLE roles (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE roles IS 'Role table';
COMMENT ON COLUMN roles.id IS 'Primary key ID';
COMMENT ON COLUMN roles.name IS 'Role name';
COMMENT ON COLUMN roles.created_at IS 'Record creation time';
COMMENT ON COLUMN roles.updated_at IS 'Record last update time';


-- Permission table (permissions)
CREATE TABLE permissions (
                             id SERIAL PRIMARY KEY,
                             name VARCHAR(255) NOT NULL,
                             url VARCHAR(255) NOT NULL,
                             created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                             updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE permissions IS 'Permission table';
COMMENT ON COLUMN permissions.id IS 'Primary key ID';
COMMENT ON COLUMN permissions.name IS 'Permission name';
COMMENT ON COLUMN permissions.url IS 'Endpoint path';
COMMENT ON COLUMN permissions.created_at IS 'Record creation time';
COMMENT ON COLUMN permissions.updated_at IS 'Record last update time';


-- Project group table (project_groups)
CREATE TABLE project_groups (
                                id SERIAL PRIMARY KEY,
                                name VARCHAR(255) NOT NULL,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE project_groups IS 'Project group table';
COMMENT ON COLUMN project_groups.id IS 'Primary key ID';
COMMENT ON COLUMN project_groups.name IS 'Project group name';
COMMENT ON COLUMN project_groups.created_at IS 'Record creation time';
COMMENT ON COLUMN project_groups.updated_at IS 'Record last update time';


-- Project table (projects)
CREATE TABLE projects (
                          id SERIAL PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          display VARCHAR(255),
                          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE projects IS 'Project table';
COMMENT ON COLUMN projects.id IS 'Primary key ID';
COMMENT ON COLUMN projects.name IS 'Project name (used for internal identification)';
COMMENT ON COLUMN projects.display IS 'Project display name (used in the UI)';
COMMENT ON COLUMN projects.created_at IS 'Record creation time';
COMMENT ON COLUMN projects.updated_at IS 'Record last update time';
ALTER TABLE projects ADD CONSTRAINT uq_projects_name UNIQUE (name);


-- 2. Relationship tables (join tables)

-- Role-user relationship table (role_user)
CREATE TABLE role_user (
                           id SERIAL PRIMARY KEY,
                           role_id INTEGER NOT NULL,
                           user_id INTEGER NOT NULL,
                           created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE role_user IS 'Role-user relationship table (many-to-many)';
COMMENT ON COLUMN role_user.role_id IS 'ID referencing the role table';
COMMENT ON COLUMN role_user.user_id IS 'ID referencing the user table';
COMMENT ON COLUMN role_user.created_at IS 'Record creation time';
COMMENT ON COLUMN role_user.updated_at IS 'Record last update time';


-- Role-permission relationship table (role_permission)
CREATE TABLE role_permission (
                                 id SERIAL PRIMARY KEY,
                                 role_id INTEGER NOT NULL,
                                 permission_id INTEGER NOT NULL,
                                 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE role_permission IS 'Role-permission relationship table (many-to-many)';
COMMENT ON COLUMN role_permission.role_id IS 'ID referencing the role table';
COMMENT ON COLUMN role_permission.permission_id IS 'ID referencing the permission table';
COMMENT ON COLUMN role_permission.created_at IS 'Record creation time';
COMMENT ON COLUMN role_permission.updated_at IS 'Record last update time';


-- User-project-group relationship table (user_project_group)
CREATE TABLE user_project_group (
                                    id SERIAL PRIMARY KEY,
                                    user_id INTEGER NOT NULL,
                                    project_group_id INTEGER NOT NULL,
                                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE user_project_group IS 'User-project-group relationship table (many-to-many)';
COMMENT ON COLUMN user_project_group.user_id IS 'ID referencing the user table';
COMMENT ON COLUMN user_project_group.project_group_id IS 'ID referencing the project group table';
COMMENT ON COLUMN user_project_group.created_at IS 'Record creation time';
COMMENT ON COLUMN user_project_group.updated_at IS 'Record last update time';


-- Project-project-group relationship table (project_project_group)
CREATE TABLE project_project_group (
                                       id SERIAL PRIMARY KEY,
                                       project_id INTEGER NOT NULL,
                                       project_group_id INTEGER NOT NULL,
                                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE project_project_group IS 'Project-project-group relationship table (many-to-many)';
COMMENT ON COLUMN project_project_group.project_id IS 'ID referencing the project table';
COMMENT ON COLUMN project_project_group.project_group_id IS 'ID referencing the project group table';
COMMENT ON COLUMN project_project_group.created_at IS 'Record creation time';
COMMENT ON COLUMN project_project_group.updated_at IS 'Record last update time';
-- >>> Create permission-related tables end

-- >>> Initialize database table routing rules start
CREATE TABLE "table_routing_rules"
(
    "id"              SERIAL                                      NOT NULL,
    "min_seconds"     int8,                                       -- Corresponds to Long minSeconds (nullable)
    "max_seconds"     int8,                                       -- Corresponds to Long maxSeconds (nullable)
    "rule_type"       varchar(50) COLLATE "pg_catalog"."default", --
    "target_table"    varchar(255) COLLATE "pg_catalog"."default",
    "optimized_table" varchar(255) COLLATE "pg_catalog"."default",
    "chunk_lag_days"  int4 NOT NULL DEFAULT 0,
    "tag"             varchar(50) COLLATE "pg_catalog"."default", --
    "data_step"       int4 NOT NULL DEFAULT 0,
    "created_at"      TIMESTAMPTZ NOT NULL DEFAULT NOW(),         --
    "updated_at"      TIMESTAMPTZ NOT NULL DEFAULT NOW(),         --
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "table_routing_rules" IS 'APM table routing rule configuration';
COMMENT ON COLUMN "table_routing_rules"."min_seconds" IS 'Minimum duration in seconds';
COMMENT ON COLUMN "table_routing_rules"."max_seconds" IS 'Maximum duration in seconds';
COMMENT ON COLUMN "table_routing_rules"."rule_type" IS 'Rule type: SIMPLE, COMPLEX';
COMMENT ON COLUMN "table_routing_rules"."target_table" IS 'Original target table name';
COMMENT ON COLUMN "table_routing_rules"."optimized_table" IS 'Optimized table name';
COMMENT ON COLUMN "table_routing_rules"."chunk_lag_days" IS 'Chunk lag in days';

-- <<< Initialize database table routing rules end



-- >>> Configure whether to process Trace data for each service start
create table service_parse_config
(
    id SERIAL PRIMARY KEY,
    service_name   text                     not null,
    parsed BOOLEAN,
    enabled BOOLEAN
);

COMMENT ON TABLE service_parse_config IS 'Service parsing configuration table';
COMMENT ON COLUMN service_parse_config.id IS 'Primary key ID';
COMMENT ON COLUMN service_parse_config.service_name IS 'Service name';
COMMENT ON COLUMN service_parse_config.parsed IS 'Whether to parse Trace data for this service';
COMMENT ON COLUMN service_parse_config.enabled IS 'Whether the configuration is enabled';
-- >>> Configure whether to process Trace data for each service end


-- >>> Configure field replacement rules for Trace data start
create table field_content_replacer_rule
(
    id SERIAL PRIMARY KEY,
    service_name   text                     not null,
    component      text                     not null,
    span_kind      SMALLINT                 not null, -- reserved, unused currently
    field_name     text                     not null,
    match_type     text                     not null,
    search_content text                     not null,
    replacement    text                     not null,
    enabled        BOOLEAN                  not null
);

COMMENT ON TABLE field_content_replacer_rule IS 'Field content replacement rule table';
COMMENT ON COLUMN field_content_replacer_rule.id IS 'Primary key ID';
COMMENT ON COLUMN field_content_replacer_rule.service_name IS 'Service name; supports the __ANY__ wildcard';
COMMENT ON COLUMN field_content_replacer_rule.component IS 'Component name; supports the __ANY__ wildcard';
COMMENT ON COLUMN field_content_replacer_rule.span_kind IS 'Span type; reserved and currently unused';
COMMENT ON COLUMN field_content_replacer_rule.field_name IS 'Field name whose content should be replaced';
COMMENT ON COLUMN field_content_replacer_rule.match_type IS 'Match type: regex, direct_return';
COMMENT ON COLUMN field_content_replacer_rule.search_content IS 'Match content or regular expression';
COMMENT ON COLUMN field_content_replacer_rule.replacement IS 'Replacement content';
COMMENT ON COLUMN field_content_replacer_rule.enabled IS 'Whether the rule is enabled';
-- >>> Configure field replacement rules for Trace data end
















-- -------------------------------------------------------------------------------------------------------------------------
INSERT INTO "permissions" ("id", "name", "url", "created_at", "updated_at") VALUES (1, 'fetch-data', '/api/apm/**', '2025-07-11 10:19:00.277939+08', '2025-07-11 10:19:00.277939+08');
INSERT INTO "permissions" ("id", "name", "url", "created_at", "updated_at") VALUES (2, 'admin', '/api/admin/**', '2025-07-11 10:19:00.303189+08', '2025-07-11 10:19:00.303189+08');
INSERT INTO "role_permission" ("id", "role_id", "permission_id", "created_at", "updated_at") VALUES (1, 1, 1, '2025-07-11 10:27:51.919464+08', '2025-07-11 10:27:51.919464+08');
INSERT INTO "role_permission" ("id", "role_id", "permission_id", "created_at", "updated_at") VALUES (2, 1, 2, '2025-07-11 10:28:25.605298+08', '2025-07-11 10:28:25.605298+08');
INSERT INTO "role_permission" ("id", "role_id", "permission_id", "created_at", "updated_at") VALUES (3, 2, 1, '2025-07-11 10:38:45.394928+08', '2025-07-11 10:38:45.394928+08');
INSERT INTO "role_user" ("id", "role_id", "user_id", "created_at", "updated_at") VALUES (1, 1, 1, '2025-07-11 10:27:30.464716+08', '2025-07-11 10:27:30.464716+08');
INSERT INTO "roles" ("id", "name", "created_at", "updated_at") VALUES (1, 'ADMIN', '2025-07-11 10:19:10.068803+08', '2025-07-11 10:19:10.068803+08');
INSERT INTO "roles" ("id", "name", "created_at", "updated_at") VALUES (2, 'USER', '2025-07-11 10:19:10.094897+08', '2025-07-11 10:19:10.094897+08');
INSERT INTO "users" ("id", "username", "password", "enabled", "created_at", "updated_at") VALUES (1, 'admin', '$2a$10$ZipLVeZelkEtd86V/rMhEOy9V2bwMP9.5sV1pTTB.BD30W5GIT00K', 't', '2025-07-11 10:19:21.958391+08', '2025-07-11 10:19:21.958391+08');

-- Reset auto-increment sequences to their maximum values
SELECT setval(pg_get_serial_sequence('"public"."permissions"', 'id'), (SELECT MAX(id) FROM "public"."permissions"));
SELECT setval(pg_get_serial_sequence('"public"."project_groups"', 'id'), (SELECT MAX(id) FROM "public"."project_groups"));
SELECT setval(pg_get_serial_sequence('"public"."projects"', 'id'), (SELECT MAX(id) FROM "public"."projects"));
SELECT setval(pg_get_serial_sequence('"public"."project_project_group"', 'id'), (SELECT MAX(id) FROM "public"."project_project_group"));
SELECT setval(pg_get_serial_sequence('"public"."role_permission"', 'id'), (SELECT MAX(id) FROM "public"."role_permission"));
SELECT setval(pg_get_serial_sequence('"public"."role_user"', 'id'), (SELECT MAX(id) FROM "public"."role_user"));
SELECT setval(pg_get_serial_sequence('"public"."roles"', 'id'), (SELECT MAX(id) FROM "public"."roles"));
SELECT setval(pg_get_serial_sequence('"public"."topology_warning_rules"', 'id'), (SELECT MAX(id) FROM "public"."topology_warning_rules"));
SELECT setval(pg_get_serial_sequence('"public"."user_project_group"', 'id'), (SELECT MAX(id) FROM "public"."user_project_group"));
SELECT setval(pg_get_serial_sequence('"public"."users"', 'id'), (SELECT MAX(id) FROM "public"."users"));

INSERT INTO public.table_routing_rules (id, min_seconds, max_seconds, rule_type, target_table, optimized_table, chunk_lag_days, tag, data_step, created_at, updated_at) VALUES (1, 0, 86400, 'SIMPLE', 'service_call_minute_dependency_unified', null, 0, 'dependency', 1, '2026-01-05 06:34:40.220793 +00:00', '2026-01-05 06:34:40.220793 +00:00');
INSERT INTO public.table_routing_rules (id, min_seconds, max_seconds, rule_type, target_table, optimized_table, chunk_lag_days, tag, data_step, created_at, updated_at) VALUES (2, 86401, 259200, 'SIMPLE', 'service_call_5min_dependency_unified', null, 0, 'dependency', 5, '2026-01-05 06:43:53.484373 +00:00', '2026-01-05 06:43:53.484373 +00:00');
INSERT INTO public.table_routing_rules (id, min_seconds, max_seconds, rule_type, target_table, optimized_table, chunk_lag_days, tag, data_step, created_at, updated_at) VALUES (3, 259201, 5184000, 'SIMPLE', 'service_call_hour_dependency_unified', null, 0, 'dependency', 60, '2026-01-05 08:10:51.108986 +00:00', '2026-01-05 08:10:51.108986 +00:00');
INSERT INTO public.table_routing_rules (id, min_seconds, max_seconds, rule_type, target_table, optimized_table, chunk_lag_days, tag, data_step, created_at, updated_at) VALUES (4, 5184001, null, 'SIMPLE', 'service_call_day_dependency', null, 0, 'dependency', 1440, '2026-01-05 08:11:28.029470 +00:00', '2026-01-05 08:11:28.029470 +00:00');
INSERT INTO public.table_routing_rules (id, min_seconds, max_seconds, rule_type, target_table, optimized_table, chunk_lag_days, tag, data_step, created_at, updated_at) VALUES (5, 0, 86400, 'SIMPLE', 'service_call_minute_entry_unified', null, 0, 'entry', 1, '2026-01-05 09:41:42.155594 +00:00', '2026-01-05 09:41:42.155594 +00:00');
INSERT INTO public.table_routing_rules (id, min_seconds, max_seconds, rule_type, target_table, optimized_table, chunk_lag_days, tag, data_step, created_at, updated_at) VALUES (6, 86401, 259200, 'SIMPLE', 'service_call_5min_entry_unified', null, 0, 'entry', 5, '2026-01-05 09:42:18.633341 +00:00', '2026-01-05 09:42:18.633341 +00:00');
INSERT INTO public.table_routing_rules (id, min_seconds, max_seconds, rule_type, target_table, optimized_table, chunk_lag_days, tag, data_step, created_at, updated_at) VALUES (7, 259201, 5184000, 'SIMPLE', 'service_call_hour_entry_unified', null, 0, 'entry', 60, '2026-01-05 09:43:30.045593 +00:00', '2026-01-05 09:43:30.045593 +00:00');
INSERT INTO public.table_routing_rules (id, min_seconds, max_seconds, rule_type, target_table, optimized_table, chunk_lag_days, tag, data_step, created_at, updated_at) VALUES (8, 5184001, null, 'SIMPLE', 'service_call_day_entry', null, 0, 'entry', 1440, '2026-01-05 09:44:17.855668 +00:00', '2026-01-05 09:44:17.855668 +00:00');
INSERT INTO public.table_routing_rules (id, min_seconds, max_seconds, rule_type, target_table, optimized_table, chunk_lag_days, tag, data_step, created_at, updated_at) VALUES (9, 0, 86400, 'SIMPLE', 'service_topology_relation_minute', null, 0, 'topology', 1, '2026-01-08 02:52:14.784224 +00:00', '2026-01-08 02:52:14.784224 +00:00');
INSERT INTO public.table_routing_rules (id, min_seconds, max_seconds, rule_type, target_table, optimized_table, chunk_lag_days, tag, data_step, created_at, updated_at) VALUES (10, 259201, 5184000, 'SIMPLE', 'service_topology_relation_hour', null, 0, 'topology', 60, '2026-01-08 02:56:21.310155 +00:00', '2026-01-08 02:56:21.310155 +00:00');
INSERT INTO public.table_routing_rules (id, min_seconds, max_seconds, rule_type, target_table, optimized_table, chunk_lag_days, tag, data_step, created_at, updated_at) VALUES (11, 5184001, null, 'SIMPLE', 'service_topology_relation_day', null, 0, 'topology', 1440, '2026-01-08 02:57:00.363779 +00:00', '2026-01-08 02:57:00.363779 +00:00');
INSERT INTO public.table_routing_rules (id, min_seconds, max_seconds, rule_type, target_table, optimized_table, chunk_lag_days, tag, data_step, created_at, updated_at) VALUES (12, 0, 86400, 'SIMPLE', 'service_host_list_minute_entry', null, 0, 'hostname', 1, '2026-01-05 09:41:42.155594 +00:00', '2026-01-05 09:41:42.155594 +00:00');
INSERT INTO public.table_routing_rules (id, min_seconds, max_seconds, rule_type, target_table, optimized_table, chunk_lag_days, tag, data_step, created_at, updated_at) VALUES (13, 86401, 259200, 'SIMPLE', 'service_host_list_5min_entry', null, 0, 'hostname', 5, '2026-01-05 09:42:18.633341 +00:00', '2026-01-05 09:42:18.633341 +00:00');
INSERT INTO public.table_routing_rules (id, min_seconds, max_seconds, rule_type, target_table, optimized_table, chunk_lag_days, tag, data_step, created_at, updated_at) VALUES (14, 259201, 5184000, 'SIMPLE', 'service_host_list_hour_entry', null, 0, 'hostname', 60, '2026-01-05 09:43:30.045593 +00:00', '2026-01-05 09:43:30.045593 +00:00');
INSERT INTO public.table_routing_rules (id, min_seconds, max_seconds, rule_type, target_table, optimized_table, chunk_lag_days, tag, data_step, created_at, updated_at) VALUES (15, 5184001, null, 'SIMPLE', 'service_host_list_day_entry', null, 0, 'hostname', 1440, '2026-01-05 09:44:17.855668 +00:00', '2026-01-05 09:44:17.855668 +00:00');
INSERT INTO public.table_routing_rules (id, min_seconds, max_seconds, rule_type, target_table, optimized_table, chunk_lag_days, tag, data_step, created_at, updated_at) VALUES (16, 86401, 259200, 'SIMPLE', 'service_topology_relation_5min', null, 0, 'topology', 5, '2026-01-08 02:52:14.784224 +00:00', '2026-01-08 02:52:14.784224 +00:00');

-- Reset auto-increment sequences to their maximum values
SELECT setval(pg_get_serial_sequence('"public"."table_routing_rules"', 'id'), (SELECT MAX(id) FROM "public"."table_routing_rules"));

END;
