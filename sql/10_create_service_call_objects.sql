-- ==================================================================
-- Table of Contents
-- ==================================================================
-- 0. Setup and base service_call objects
-- 1. service_call_* aggregations
--    1.1 1-minute aggregations
--    1.2 5-minute aggregations
--    1.3 1-hour aggregations
--    1.4 1-day aggregations
-- 2. hostname aggregations
--    2.1 1-minute aggregations
--    2.2 5-minute aggregations
--    2.3 1-hour aggregations
--    2.4 1-day aggregations
-- 3. topology relation aggregations
--    3.1 1-minute aggregations
--    3.2 5-minute aggregations
--    3.3 1-hour aggregations
--    3.4 1-day aggregations
-- ==================================================================


-- ==================================================================
-- Index Naming Convention
-- ==================================================================
-- Pattern:
--   idx_<family>[_<grain>][_<role>][_m]_<key_shape>[_inc_<included_cols>]
--
-- family:
--   sc  = service_call
--   shl = service_host_list
--   str = service_topology_relation
--
-- grain:
--   min, 5min, hour, day
--
-- role:
--   entry = entry-side aggregate
--   dep   = dependency-side aggregate
--
-- suffix:
--   m = merged/archive aggregate
--
-- key_shape:
--   Use abbreviated leading index columns in index order.
--   svc    = service_name
--   kind   = span_kind
--   tgt    = target_name
--   caller = caller_service_name / caller-side lookup path
--   callee = service_name / callee-side lookup path
--   type   = system_type
--   time   = time DESC
--
-- include:
--   inc_<cols> means non-key INCLUDE columns, for example inc_host for INCLUDE (host_name).
--
-- Examples:
--   idx_sc_min_entry_svc_kind_time
--     -> service_call_minute_entry (service_name, span_kind, time DESC)
--   idx_sc_min_entry_m_svc_kind_tgt_time
--     -> service_call_minute_entry_merged (service_name, span_kind, target_name, time DESC)
--   idx_sc_min_dep_caller
--     -> service_call_minute_dependency caller-side lookup path
--   idx_shl_min_entry_svc_kind_time_inc_host
--     -> service_host_list_minute_entry (service_name, span_kind, time DESC) INCLUDE (host_name)
--   idx_str_min_dep_type_kind_caller_svc_time
--     -> service_topology_relation_minute_dependency (system_type, span_kind, caller_service_name, service_name, time DESC)
--
-- Notes:
--   Keep names under PostgreSQL's 63-byte identifier limit.
--   For changed index definitions in existing databases, drop/recreate old names explicitly;
--   CREATE INDEX fails when the index name already exists.



-- ==================================================================
-- 0. BEGIN setup and base service_call objects
-- ==================================================================

-- ---------------------------------------------------------
-- 0.1 Extensions
-- ---------------------------------------------------------

-- create DATABASE apm;

CREATE EXTENSION IF NOT EXISTS timescaledb;
CREATE EXTENSION IF NOT EXISTS timescaledb_toolkit;
CREATE EXTENSION IF NOT EXISTS pg_prewarm;


-- ---------------------------------------------------------
-- 0.2 Initialize environment and create dedicated job user
-- ---------------------------------------------------------

CREATE USER ts_job_runner WITH PASSWORD 'ts_job_runner';

-- Force this user to disable sort and prefer HashAggregate
ALTER ROLE ts_job_runner SET enable_sort = off;
-- Grant enough memory
ALTER ROLE ts_job_runner SET work_mem = '512MB';
-- Grant schema permissions
GRANT USAGE ON SCHEMA public TO ts_job_runner;



-- ---------------------------------------------------------
-- 0.3 Resource dictionary table (stores Hash -> original text mappings)
-- ---------------------------------------------------------
CREATE TABLE resource_hash_dictionary
(
    hash_key    UUID NOT NULL, -- 128-bit Hash (XXH3)
    category    TEXT NOT NULL, -- Business category (e.g. 'target_name')
    algorithm   TEXT NOT NULL, -- Algorithm identifier (e.g. 'XXH3_128')
    text_value  TEXT NOT NULL, -- Original text
    create_time TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT resource_hash_dictionary_pkey PRIMARY KEY (hash_key, category)
);

-- ---------------------------------------------------------
-- 0.4 Create service_call table structure
-- ---------------------------------------------------------
CREATE TABLE service_call
(
    time                  TIMESTAMP WITH TIME ZONE NOT NULL,
    trace_id              TEXT                     NOT NULL,
    span_id               TEXT                     NOT NULL,
    service_name          TEXT                     NOT NULL,
    host_name             TEXT                     NOT NULL,
    action_name           TEXT                     NOT NULL,
    target_name           UUID                     NOT NULL,
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
    caller_target_name    UUID                     NOT NULL,
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


-- Ensure the job user can read raw data
GRANT SELECT, INSERT, UPDATE, DELETE ON service_call TO ts_job_runner;

-- Convert to a hypertable
SELECT create_hypertable('service_call', by_range('time', INTERVAL '2 hours'));


-- Enable columnstore
ALTER TABLE service_call SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'service_name, span_kind',
    timescaledb.orderby = 'time DESC'
    );

-- Compression policy
CALL add_columnstore_policy('service_call',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Indexes
CREATE INDEX idx_sc_kind_time ON service_call (span_kind, time DESC);

-- Retention policy
SELECT add_retention_policy('service_call', INTERVAL '4 days');


-- =========================================================
-- 0. END setup and base service_call objects
-- =========================================================


-- =========================================================
-- 1. BEGIN service_call_* aggregations
-- =========================================================

-- ---------------------------------------------------------
-- 1.1 1-minute aggregations
-- ---------------------------------------------------------
-- Tier 1: Entry minute-level aggregate
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
    -- Last write time for this minute of data, used to investigate late arrivals
    MAX(update_time) AS last_insert_time
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

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_minute_entry OWNER TO ts_job_runner;


-- Indexes
-- This index is used to query host name and entry-related data.
CREATE INDEX idx_sc_min_entry_svc_kind_time
    ON service_call_minute_entry (service_name, span_kind, time DESC);
-- This index is similar to the one above but performs better. Queried fields no longer need heap lookups.
-- The index size is about 18% larger than the one above.
-- CREATE INDEX idx_sc_min_entry_svc_kind_time_cov
--     ON service_call_minute_entry (service_name, span_kind, time DESC)


CREATE INDEX idx_sc_min_entry_svc_kind_tgt_time
    ON service_call_minute_entry (service_name, span_kind, target_name, time DESC);
-- This index is similar to the one above but performs better. Queried fields no longer need heap lookups.
-- The index size is about 60% larger than the one above.
-- CREATE INDEX idx_sc_min_entry_svc_kind_tgt_time_cov
--     ON service_call_minute_entry (service_name, span_kind, target_name, time DESC)


-- Set chunk size
SELECT set_chunk_time_interval('service_call_minute_entry', INTERVAL '2 hours');

-- Enable columnstore
ALTER MATERIALIZED VIEW service_call_minute_entry SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'service_name, span_kind, target_name, system_type'
    );

-- Aggregation policy
SELECT add_continuous_aggregate_policy('service_call_minute_entry',
                                       start_offset => INTERVAL '10 minutes',
                                       end_offset   => INTERVAL '0 minutes',
                                       schedule_interval => INTERVAL '1 minute');

CALL add_columnstore_policy('service_call_minute_entry',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Retention policy
SELECT add_retention_policy('service_call_minute_entry', INTERVAL '4 days');


-- =========================================================
-- Tier 2: Entry daily archive
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
    -- Pass through sum_duration
    SUM(sum_duration) AS sum_duration,
    -- Use exact division
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

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_minute_entry_merged OWNER TO ts_job_runner;





-- Indexes
CREATE INDEX idx_sc_min_entry_m_svc_kind_time
    ON service_call_minute_entry_merged (service_name, span_kind, time DESC);
-- CREATE INDEX idx_sc_min_entry_m_svc_kind_time_cov
--     ON service_call_minute_entry_merged (service_name, span_kind, time DESC)

CREATE INDEX idx_sc_min_entry_m_svc_kind_tgt_time
    ON service_call_minute_entry_merged (service_name, span_kind, target_name, time DESC);
-- CREATE INDEX idx_sc_min_entry_m_svc_kind_tgt_time_cov
--     ON service_call_minute_entry_merged (service_name, span_kind, target_name, time DESC)

-- Set chunk size
SELECT set_chunk_time_interval('service_call_minute_entry_merged', INTERVAL '1 day');

-- Enable columnstore
ALTER MATERIALIZED VIEW service_call_minute_entry_merged SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'service_name, span_kind, target_name, system_type'
    );

-- Run at 02:00 every day. Move the aggregate data time back by 1 hour to tolerate delayed startup.
-- Therefore, when the start time is 2000-01-12 02:00:00, the processed data time range is:
--   from: 2000-01-11 23:00:00  to: 2000-01-12 00:00:00
-- Therefore, when the start time is 2000-01-12 02:10:00, the processed data time range is:
--   from: 2000-01-11 23:10:00  to: 2000-01-12 00:10:00
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


-- Retention policy
SELECT add_retention_policy('service_call_minute_entry_merged', INTERVAL '30 days');



-- ==========================================
-- Minute-level Entry unified view
-- Boundary: 2 days (= merged chunk 1 day + compression delay 1 day)
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
    -- 
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

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_minute_dependency OWNER TO ts_job_runner;

CREATE INDEX idx_sc_min_dep_caller ON service_call_minute_dependency (caller_service_name, caller_span_kind, span_kind, caller_target_name, time DESC, host_name);
CREATE INDEX idx_sc_min_dep_callee ON service_call_minute_dependency (service_name, system_type, span_kind, caller_target_name, target_name, time DESC, host_name);


-- Set chunk size
SELECT set_chunk_time_interval('service_call_minute_dependency', INTERVAL '2 hours');

-- host_name is not included in segment by. Add it if performance feels slow.
ALTER MATERIALIZED VIEW service_call_minute_dependency SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'caller_service_name, caller_target_name, caller_span_kind, service_name, system_type, target_name, span_kind'
    );

-- Aggregation policy
SELECT add_continuous_aggregate_policy('service_call_minute_dependency',
                                       start_offset => INTERVAL '10 minutes',
                                       end_offset   => INTERVAL '0 minutes',
                                       schedule_interval => INTERVAL '1 minute');

CALL add_columnstore_policy('service_call_minute_dependency',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Retention policy
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
    SUM(sum_duration) AS sum_duration,
    -- 
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

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_minute_dependency_merged OWNER TO ts_job_runner;

CREATE INDEX idx_sc_min_dep_m_caller ON service_call_minute_dependency_merged (caller_service_name, caller_span_kind, span_kind, caller_target_name, time DESC, host_name);
CREATE INDEX idx_sc_min_dep_m_callee ON service_call_minute_dependency_merged (service_name, system_type, span_kind, caller_target_name, target_name, time DESC, host_name);


-- Set chunk size
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


-- Retention policy
SELECT add_retention_policy('service_call_minute_dependency_merged', INTERVAL '30 days');


-- ==========================================
-- Minute-level Dependency unified view
-- ==========================================
CREATE VIEW service_call_minute_dependency_unified AS
SELECT * FROM service_call_minute_dependency
WHERE time >= NOW() - INTERVAL '2 days'
UNION ALL
SELECT * FROM service_call_minute_dependency_merged
WHERE time < NOW() - INTERVAL '2 days';



-- ---------------------------------------------------------
-- 1.2 5-minute aggregations
-- ---------------------------------------------------------
-- 5 min Entry
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
    -- 
    SUM(sum_duration) as sum_duration,
    -- Use sum/cnt
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

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_5min_entry OWNER TO ts_job_runner;


CREATE INDEX idx_sc_5min_entry_svc_kind_time
    ON service_call_5min_entry (service_name, span_kind, time DESC);
-- CREATE INDEX idx_sc_5min_entry_svc_kind_time_cov
--     ON service_call_5min_entry (service_name, span_kind, time DESC)

CREATE INDEX idx_sc_5min_entry_svc_kind_tgt_time
    ON service_call_5min_entry (service_name, span_kind, target_name, time DESC);
-- CREATE INDEX idx_sc_5min_entry_svc_kind_tgt_time_cov
--     ON service_call_5min_entry (service_name, span_kind, target_name, time DESC)




-- Set chunk size
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

-- Retention policy
SELECT add_retention_policy('service_call_5min_entry', INTERVAL '10 days');


-- =========================================================
-- Tier 2: 5-minute Entry archive merge (based on 5min_entry)
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

-- Indexes
CREATE INDEX idx_sc_5min_entry_m_svc_kind_time
    ON service_call_5min_entry_merged (service_name, span_kind, time DESC);

CREATE INDEX idx_sc_5min_entry_m_svc_kind_tgt_time
    ON service_call_5min_entry_merged (service_name, span_kind, target_name, time DESC);

-- Set chunk size
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

-- Retention policy
SELECT add_retention_policy('service_call_5min_entry_merged', INTERVAL '90 days');


-- ==========================================
-- 5Minute-level Entry unified view
-- Boundary: 8 days (= merged chunk 7 days + compression delay 1 day)
-- ==========================================
CREATE VIEW service_call_5min_entry_unified AS
SELECT * FROM service_call_5min_entry
WHERE time >= NOW() - INTERVAL '8 days'
UNION ALL
SELECT * FROM service_call_5min_entry_merged
WHERE time < NOW() - INTERVAL '8 days';



-- 5 min Dependency
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
    SUM(sum_duration) as sum_duration,
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

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_5min_dependency OWNER TO ts_job_runner;

-- Indexes
CREATE INDEX idx_sc_5min_dep_caller ON service_call_5min_dependency (caller_service_name, caller_span_kind, span_kind, caller_target_name, time DESC, host_name);
CREATE INDEX idx_sc_5min_dep_callee ON service_call_5min_dependency (service_name, system_type, span_kind, caller_target_name, target_name, time DESC, host_name);



-- Set chunk size
SELECT set_chunk_time_interval('service_call_5min_dependency', INTERVAL '2 hours');

-- Enable columnstore
ALTER MATERIALIZED VIEW service_call_5min_dependency SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'caller_service_name, caller_target_name, caller_span_kind, service_name, system_type, target_name, span_kind'
    );

-- Aggregation policy
SELECT add_continuous_aggregate_policy('service_call_5min_dependency',
                                       start_offset => INTERVAL '30 minutes',
                                       end_offset   => INTERVAL '5 minutes',
                                       schedule_interval => INTERVAL '1 minute');

-- Compression policy
CALL add_columnstore_policy('service_call_5min_dependency',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Retention policy
SELECT add_retention_policy('service_call_5min_dependency', INTERVAL '10 days');



-- =========================================================
-- Tier 2: 5-minute Dependency archive merge (based on 5min_dependency)
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

-- Index creation
CREATE INDEX idx_sc_5min_dep_m_caller ON service_call_5min_dependency_merged (caller_service_name, caller_span_kind, span_kind, caller_target_name, time DESC, host_name);
CREATE INDEX idx_sc_5min_dep_m_callee ON service_call_5min_dependency_merged (service_name, system_type, span_kind, caller_target_name, target_name, time DESC, host_name);

-- Set chunk size
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

-- Retention policy
SELECT add_retention_policy('service_call_5min_dependency_merged', INTERVAL '90 days');


-- ==========================================
-- 5Minute-level Dependency unified view
-- ==========================================
CREATE VIEW service_call_5min_dependency_unified AS
SELECT * FROM service_call_5min_dependency
WHERE time >= NOW() - INTERVAL '8 days'
UNION ALL
SELECT * FROM service_call_5min_dependency_merged
WHERE time < NOW() - INTERVAL '8 days';


-- ---------------------------------------------------------
-- 1.3 1-hour aggregations
-- ---------------------------------------------------------
-- Tier 2: Entry hourly aggregate

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
    SUM(sum_duration) as sum_duration,
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

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_hour_entry OWNER TO ts_job_runner;

-- Indexes
CREATE INDEX idx_sc_hour_entry_svc_kind_time
    ON service_call_hour_entry (service_name, span_kind, time DESC);
-- CREATE INDEX idx_sc_hour_entry_svc_kind_time_cov
--     ON service_call_hour_entry (service_name, span_kind, time DESC)

CREATE INDEX idx_sc_hour_entry_svc_kind_tgt_time
    ON service_call_hour_entry (service_name, span_kind, target_name, time DESC);
-- CREATE INDEX idx_sc_hour_entry_svc_kind_tgt_time_cov
--     ON service_call_hour_entry (service_name, span_kind, target_name, time DESC)



-- Set chunk size
SELECT set_chunk_time_interval('service_call_hour_entry', INTERVAL '1 day');

-- Enable columnstore
ALTER MATERIALIZED VIEW service_call_hour_entry SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'service_name, span_kind, target_name, system_type'
    );

-- Refresh policy
SELECT add_continuous_aggregate_policy('service_call_hour_entry',
                                       start_offset => INTERVAL '6 hours',
                                       end_offset   => INTERVAL '3 hours',
                                       schedule_interval => INTERVAL '30 minutes');

-- Compression policy
CALL add_columnstore_policy('service_call_hour_entry',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Retention policy
SELECT add_retention_policy('service_call_hour_entry', INTERVAL '20 days');


-- =========================================================
-- Tier 2: Hour Entry archive merge (based on service_call_hour_entry)
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

-- Indexes
CREATE INDEX idx_sc_hour_entry_m_svc_kind_time
    ON service_call_hour_entry_merged (service_name, span_kind, time DESC);

CREATE INDEX idx_sc_hour_entry_m_svc_kind_tgt_time
    ON service_call_hour_entry_merged (service_name, span_kind, target_name, time DESC);

-- Set chunk size
SELECT set_chunk_time_interval('service_call_hour_entry_merged', INTERVAL '15 days');

-- Enable columnstore
ALTER MATERIALIZED VIEW service_call_hour_entry_merged SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'service_name, span_kind, target_name, system_type'
    );

-- Refresh policy
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

-- Retention policy
SELECT add_retention_policy('service_call_hour_entry_merged', INTERVAL '400 days');



-- ==========================================
-- Hourly Entry unified view
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
    SUM(sum_duration) as sum_duration,
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

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_hour_dependency OWNER TO ts_job_runner;


-- Indexes
CREATE INDEX idx_sc_hour_dep_caller ON service_call_hour_dependency (caller_service_name, caller_span_kind, span_kind, caller_target_name, time DESC, host_name);
CREATE INDEX idx_sc_hour_dep_callee ON service_call_hour_dependency (service_name, system_type, span_kind, caller_target_name, target_name, time DESC, host_name);



-- Set chunk size
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


-- Retention policy
SELECT add_retention_policy('service_call_hour_dependency', INTERVAL '20 days');

-- =========================================================
-- Tier 2: Hour Dependency archive merge (based on service_call_hour_dependency)
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
    SUM(sum_duration) AS sum_duration,
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

-- Indexes
CREATE INDEX idx_sc_hour_dep_m_caller ON service_call_hour_dependency_merged (caller_service_name, caller_span_kind, span_kind, caller_target_name, time DESC, host_name);
CREATE INDEX idx_sc_hour_dep_m_callee ON service_call_hour_dependency_merged (service_name, system_type, span_kind, caller_target_name, target_name, time DESC, host_name);

-- Set chunk size
SELECT set_chunk_time_interval('service_call_hour_dependency_merged', INTERVAL '15 days');

-- Enable columnstore
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

-- Retention policy
SELECT add_retention_policy('service_call_hour_dependency_merged', INTERVAL '400 days');


-- ==========================================
-- Hourly Dependency unified view
-- ==========================================
CREATE VIEW service_call_hour_dependency_unified AS
SELECT * FROM service_call_hour_dependency
WHERE time >= NOW() - INTERVAL '16 days'
UNION ALL
SELECT * FROM service_call_hour_dependency_merged
WHERE time < NOW() - INTERVAL '16 days';


-- ---------------------------------------------------------
-- 1.4 1-day aggregations
-- ---------------------------------------------------------
-- Tier 3: Entry daily aggregate

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
    SUM(sum_duration) as sum_duration,
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

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_day_entry OWNER TO ts_job_runner;


-- Indexes
CREATE INDEX idx_sc_day_entry_svc_kind_time
    ON service_call_day_entry (service_name, span_kind, time DESC);
-- CREATE INDEX idx_sc_day_entry_svc_kind_time_cov
--     ON service_call_day_entry (service_name, span_kind, time DESC)

CREATE INDEX idx_sc_day_entry_svc_kind_tgt_time
    ON service_call_day_entry (service_name, span_kind, target_name, time DESC);
-- CREATE INDEX idx_sc_day_entry_svc_kind_tgt_time_cov
--     ON service_call_day_entry (service_name, span_kind, target_name, time DESC)




-- Set chunk size
SELECT set_chunk_time_interval('service_call_day_entry', INTERVAL '30 days');

ALTER MATERIALIZED VIEW service_call_day_entry SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'service_name, span_kind, target_name, system_type'
    );

-- Refresh policy (unchanged: refresh daily and calculate data from 3 days ago to 1 day ago)
SELECT add_continuous_aggregate_policy('service_call_day_entry',
                                       start_offset => INTERVAL '3 days',
                                       end_offset   => INTERVAL '1 day',
                                       schedule_interval => INTERVAL '1 day');

-- Compression policy
CALL add_columnstore_policy('service_call_day_entry',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Retention policy
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
    SUM(sum_duration) as sum_duration,
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

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_call_day_dependency OWNER TO ts_job_runner;


-- Indexes
CREATE INDEX idx_sc_day_dep_caller ON service_call_day_dependency (caller_service_name, caller_span_kind, span_kind, caller_target_name, time DESC, host_name);
CREATE INDEX idx_sc_day_dep_callee ON service_call_day_dependency (service_name, system_type, span_kind, caller_target_name, target_name, time DESC, host_name);


-- Set chunk size to 30 days
SELECT set_chunk_time_interval('service_call_day_dependency', INTERVAL '30 days');

-- Enable columnstore
ALTER MATERIALIZED VIEW service_call_day_dependency SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'caller_service_name, caller_target_name, caller_span_kind, service_name, system_type, target_name, span_kind'
    );

-- Refresh policy
SELECT add_continuous_aggregate_policy('service_call_day_dependency',
                                       start_offset => INTERVAL '3 days',
                                       end_offset   => INTERVAL '1 day',
                                       schedule_interval => INTERVAL '1 day');

-- Compression policy
CALL add_columnstore_policy('service_call_day_dependency',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Retention policy
SELECT add_retention_policy('service_call_day_dependency', INTERVAL '1100 days');









-- =========================================================
-- 1. END service_call_* aggregations
-- =========================================================


-- =========================================================
-- 2. BEGIN hostname aggregations
-- =========================================================

-- ---------------------------------------------------------
-- 2.1 1-minute aggregations
-- ---------------------------------------------------------
-- Host List Minute (1-minute level)
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

-- Retention policy
SELECT add_retention_policy('service_host_list_minute_entry', INTERVAL '30 days');




-- ---------------------------------------------------------
-- 2.2 5-minute aggregations
-- ---------------------------------------------------------
-- Host List 5 Minutes (5-minute level)
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

-- Retention policy
SELECT add_retention_policy('service_host_list_5min_entry', INTERVAL '90 days');




-- ---------------------------------------------------------
-- 2.3 1-hour aggregations
-- ---------------------------------------------------------
-- Host List Hour (hourly level)
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

SELECT set_chunk_time_interval('service_host_list_hour_entry', INTERVAL '60 days');

ALTER MATERIALIZED VIEW service_host_list_hour_entry SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'service_name, span_kind, host_name'
    );

SELECT add_continuous_aggregate_policy('service_host_list_hour_entry',
                                       start_offset => INTERVAL '6 hours',
                                       end_offset   => INTERVAL '1 hour',
                                       schedule_interval => INTERVAL '30 minutes');

CALL add_columnstore_policy('service_host_list_hour_entry',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Retention policy
SELECT add_retention_policy('service_host_list_hour_entry', INTERVAL '400 days');





-- ---------------------------------------------------------
-- 2.4 1-day aggregations
-- ---------------------------------------------------------
-- Host List Day (daily level)
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

SELECT set_chunk_time_interval('service_host_list_day_entry', INTERVAL '90 days');

ALTER MATERIALIZED VIEW service_host_list_day_entry SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'service_name, span_kind, host_name'
    );

SELECT add_continuous_aggregate_policy('service_host_list_day_entry',
                                       start_offset => INTERVAL '3 days',
                                       end_offset   => INTERVAL '1 day',
                                       schedule_interval => INTERVAL '1 day');

CALL add_columnstore_policy('service_host_list_day_entry',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Retention policy
SELECT add_retention_policy('service_host_list_day_entry', INTERVAL '1100 days');

-- =========================================================
-- 2. END hostname aggregations
-- =========================================================













-- =========================================================
-- 3. BEGIN topology relation aggregations
-- =========================================================


-- ---------------------------------------------------------
-- 3.1 1-minute aggregations
-- ---------------------------------------------------------
-- Topology Relation minute-level aggregate (Entry side)
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

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_topology_relation_minute_entry OWNER TO ts_job_runner;

-- Aggregation policy
SELECT add_continuous_aggregate_policy('service_topology_relation_minute_entry',
                                       start_offset => INTERVAL '10 minutes',
                                       end_offset => INTERVAL '0 minute',
                                       schedule_interval => INTERVAL '1 minute');

-- Enable columnstore
ALTER MATERIALIZED VIEW service_topology_relation_minute_entry set (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'caller_service_name, span_kind, system_type',
    timescaledb.orderby = 'time DESC, service_name'
    );

-- Set chunk size
SELECT set_chunk_time_interval('service_topology_relation_minute_entry', INTERVAL '2 hours');

-- Indexes
CREATE INDEX idx_str_min_entry_kind_caller_svc_type_time ON service_topology_relation_minute_entry
    (span_kind, caller_service_name, service_name, system_type, time DESC);

CALL add_columnstore_policy('service_topology_relation_minute_entry',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Retention policy
SELECT add_retention_policy('service_topology_relation_minute_entry', INTERVAL '30 days');




-- Topology Relation minute-level aggregate (Dependency side)
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

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_topology_relation_minute_dependency OWNER TO ts_job_runner;

-- Aggregation policy
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

-- Retention policy
SELECT add_retention_policy('service_topology_relation_minute_dependency', INTERVAL '30 days');



-- Create the unified view (keep the original external table name)
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





-- ---------------------------------------------------------
-- 3.2 5-minute aggregations
-- ---------------------------------------------------------
-- Topology Relation 5-minute aggregate (Entry side)
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

-- Set chunk size
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

-- Retention policy
SELECT add_retention_policy('service_topology_relation_5min_entry', INTERVAL '90 days');



-- Topology Relation 5-minute aggregate (Dependency side)
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

-- Set chunk size
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

-- Indexes: note that the system_type and span_kind order differs from the entry table, matching the original design
CREATE INDEX idx_str_5min_dep_type_kind_caller_svc_time ON service_topology_relation_5min_dependency
    (system_type, span_kind, caller_service_name, service_name, time DESC);

-- Compression policy
CALL add_columnstore_policy('service_topology_relation_5min_dependency',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Retention policy
SELECT add_retention_policy('service_topology_relation_5min_dependency', INTERVAL '90 days');

-- =========================================================
-- Create the topology 5-minute unified view (external interface)
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




-- ---------------------------------------------------------
-- 3.3 1-hour aggregations
-- ---------------------------------------------------------
-- Create the Hour aggregate for the Entry side
-- Source data: service_topology_relation_5min_entry
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

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_topology_relation_hour_entry OWNER TO ts_job_runner;

-- Set chunk size
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

-- Retention policy
SELECT add_retention_policy('service_topology_relation_hour_entry', INTERVAL '400 days');


-- Create the Hour aggregate for the Dependency side
-- Source data: service_topology_relation_5min_dependency
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

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_topology_relation_hour_dependency OWNER TO ts_job_runner;

-- Set chunk size
SELECT set_chunk_time_interval('service_topology_relation_hour_dependency', INTERVAL '14 days');

-- Compression settings
ALTER MATERIALIZED VIEW service_topology_relation_hour_dependency SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'caller_service_name, system_type, span_kind',
    timescaledb.orderby = 'time DESC, remote_addr, service_name'
    );

-- Aggregation policy
SELECT add_continuous_aggregate_policy('service_topology_relation_hour_dependency',
                                       start_offset => INTERVAL '6 hours',
                                       end_offset   => INTERVAL '1 hour',
                                       schedule_interval => INTERVAL '30 minutes');

-- Indexes
CREATE INDEX idx_str_hour_dep_type_kind_caller_svc_time
    ON service_topology_relation_hour_dependency
        (system_type, span_kind, caller_service_name, service_name, time DESC);

-- Compression policy
CALL add_columnstore_policy('service_topology_relation_hour_dependency',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Retention policy
SELECT add_retention_policy('service_topology_relation_hour_dependency', INTERVAL '400 days');


-- =========================================================
-- Create the unified view (external interface)
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






-- ---------------------------------------------------------
-- 3.4 1-day aggregations
-- ---------------------------------------------------------
-- Create the Day aggregate for the Entry side
-- Source data: service_topology_relation_hour_entry
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

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_topology_relation_day_entry OWNER TO ts_job_runner;

-- Set chunk size
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

-- Retention policy
SELECT add_retention_policy('service_topology_relation_day_entry', INTERVAL '1100 days');


-- Create the Day aggregate for the Dependency side
-- Source data: service_topology_relation_hour_dependency
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

-- Change view owner to the job user
ALTER MATERIALIZED VIEW service_topology_relation_day_dependency OWNER TO ts_job_runner;

-- Set chunk size
SELECT set_chunk_time_interval('service_topology_relation_day_dependency', INTERVAL '30 days');

-- Compression settings
ALTER MATERIALIZED VIEW service_topology_relation_day_dependency SET (
    timescaledb.enable_columnstore = true,
    timescaledb.segmentby = 'caller_service_name, system_type, span_kind',
    timescaledb.orderby = 'time DESC, remote_addr, service_name'
    );

-- Aggregation policy
SELECT add_continuous_aggregate_policy('service_topology_relation_day_dependency',
                                       start_offset => INTERVAL '3 days',
                                       end_offset   => INTERVAL '1 day',
                                       schedule_interval => INTERVAL '1 day');

-- Indexes
CREATE INDEX idx_str_day_dep_type_kind_caller_svc_time
    ON service_topology_relation_day_dependency
        (system_type, span_kind, caller_service_name, service_name, time DESC);

-- Compression policy
CALL add_columnstore_policy('service_topology_relation_day_dependency',
                            after             => INTERVAL '60 minutes',
                            schedule_interval => INTERVAL '10 minutes');

-- Retention policy
SELECT add_retention_policy('service_topology_relation_day_dependency', INTERVAL '1100 days');





-- =========================================================
-- Create the unified view (external interface)
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


-- =========================================================
-- 3. END topology relation aggregations
-- =========================================================
