-- ==================================================================
-- TimescaleDB CAGG prewarm function
-- Create this on the primary database; WAL automatically replicates it to replicas
-- ==================================================================
-- Prerequisites:
-- - shared_preload_libraries = 'pg_prewarm'
-- - pg_prewarm.autoprewarm = on
-- - CREATE EXTENSION IF NOT EXISTS pg_prewarm;
-- ==================================================================


-- ==================================================================
-- Example calls (run on a replica)
-- ==================================================================

-- Use default parameters (4 CAGGs, 3 days)
-- SELECT prewarm_data();

-- Customize the CAGG list and day count
-- SELECT prewarm_data(
--     ARRAY['service_call_minute_entry', 'service_call_5min_entry'],
--     5
-- );



CREATE OR REPLACE FUNCTION prewarm_data(
    cagg_names text[] DEFAULT ARRAY[
        'service_call_minute_entry',
        'service_call_5min_entry',
        'service_topology_relation_minute_entry',
        'service_topology_relation_minute_dependency',
        'service_host_list_minute_entry'
        ],
    days int DEFAULT 3
)
    RETURNS void AS $func$
DECLARE
    _time_threshold bigint;
BEGIN
    _time_threshold := extract(epoch from (now() - (days || ' days')::interval))::bigint * 1000000;

    -- ==========================================================
    -- Part 1: Catalog prewarm (same as the manual procedure)
    -- Prewarmed parts: the table's main data file and its indexes (the table main fork and index relation main fork)
    -- Not prewarmed: table fsm, table vm, TOAST tables and their indexes, and init forks for unlogged relations
    -- ==========================================================

    -- Prewarm catalog tables
    PERFORM pg_prewarm('_timescaledb_catalog.hypertable');
    PERFORM pg_prewarm('_timescaledb_catalog.chunk');
    PERFORM pg_prewarm('_timescaledb_catalog.chunk_constraint');
    PERFORM pg_prewarm('_timescaledb_catalog.dimension');
    PERFORM pg_prewarm('_timescaledb_catalog.dimension_slice');
    PERFORM pg_prewarm('_timescaledb_catalog.continuous_agg');
    PERFORM pg_prewarm('_timescaledb_catalog.continuous_aggs_bucket_function');
    PERFORM pg_prewarm('_timescaledb_catalog.continuous_aggs_watermark');
    PERFORM pg_prewarm('_timescaledb_catalog.compression_settings');
    PERFORM pg_prewarm('_timescaledb_catalog.compression_chunk_size');
    PERFORM pg_prewarm('_timescaledb_catalog.chunk_column_stats');
    PERFORM pg_prewarm('_timescaledb_catalog.continuous_aggs_invalidation_threshold');

    -- Prewarm catalog indexes
    PERFORM pg_prewarm(indexrelid)
    FROM pg_index
    WHERE indrelid IN (
                       '_timescaledb_catalog.hypertable'::regclass,
                       '_timescaledb_catalog.chunk'::regclass,
                       '_timescaledb_catalog.chunk_constraint'::regclass,
                       '_timescaledb_catalog.dimension'::regclass,
                       '_timescaledb_catalog.dimension_slice'::regclass,
                       '_timescaledb_catalog.continuous_agg'::regclass,
                       '_timescaledb_catalog.continuous_aggs_bucket_function'::regclass,
                       '_timescaledb_catalog.continuous_aggs_watermark'::regclass,
                       '_timescaledb_catalog.compression_settings'::regclass,
                       '_timescaledb_catalog.compression_chunk_size'::regclass,
                       '_timescaledb_catalog.chunk_column_stats'::regclass,
                       '_timescaledb_catalog.continuous_aggs_invalidation_threshold'::regclass
        );

    -- ==========================================================
    -- Part 1.5: PostgreSQL core system catalog prewarm
    -- Prewarmed parts: the table's main data file and its indexes (the table main fork and index relation main fork)
    -- Not prewarmed: table fsm, table vm, TOAST tables and their indexes, and init forks for unlogged relations

    -- In environments with many chunks (>1000), pg_class / pg_attribute and similar
    -- system catalogs contain many rows and produce heavy random I/O during cold starts.
    -- pg_class      : one row for each chunk/TOAST/index, ~10,000+ rows
    -- pg_attribute  : one row per column, ~200,000+ rows
    -- pg_index      : one row per index
    -- pg_inherits   : hypertable -> chunk inheritance relationships
    -- pg_statistic  : column statistics, affecting optimizer cost estimation
    -- ==========================================================

    -- Core system catalog tables
    PERFORM pg_prewarm('pg_catalog.pg_class');
    PERFORM pg_prewarm('pg_catalog.pg_attribute');
    PERFORM pg_prewarm('pg_catalog.pg_index');
    PERFORM pg_prewarm('pg_catalog.pg_inherits');
    PERFORM pg_prewarm('pg_catalog.pg_statistic');

    -- Core system catalog indexes
    PERFORM pg_prewarm(indexrelid)
    FROM pg_index
    WHERE indrelid IN (
        'pg_catalog.pg_class'::regclass,
        'pg_catalog.pg_attribute'::regclass,
        'pg_catalog.pg_index'::regclass,
        'pg_catalog.pg_inherits'::regclass,
        'pg_catalog.pg_statistic'::regclass
    );

    -- ==========================================================
    -- Part 2: CAGG hot-data prewarm
    -- The four SQL statements below are identical to cagg_prewarm.sql.
    -- The only differences:
    --   SELECT → PERFORM
    --   WHERE user_view_name = 'some_name'
    --     -> WHERE user_view_name = ANY(cagg_names)
    --   interval '3 days'
    --     -> _time_threshold variable
    -- ==========================================================

    -- 1. Uncompressed chunks (table data)
    PERFORM pg_prewarm(format('_timescaledb_internal.%I', orig.table_name)::regclass)
    FROM _timescaledb_catalog.chunk orig
             JOIN _timescaledb_catalog.chunk_constraint cc ON cc.chunk_id = orig.id
             JOIN _timescaledb_catalog.dimension_slice ds ON ds.id = cc.dimension_slice_id
    WHERE orig.hypertable_id IN (
        SELECT mat_hypertable_id FROM _timescaledb_catalog.continuous_agg
        WHERE user_view_name = ANY(cagg_names)
    ) AND NOT orig.dropped AND orig.compressed_chunk_id IS NULL
      AND ds.range_end > _time_threshold;

    -- 2. Indexes for uncompressed chunks
    PERFORM pg_prewarm(i.indexrelid)
    FROM _timescaledb_catalog.chunk orig
             JOIN pg_index i ON i.indrelid = format('_timescaledb_internal.%I', orig.table_name)::regclass
             JOIN _timescaledb_catalog.chunk_constraint cc ON cc.chunk_id = orig.id
             JOIN _timescaledb_catalog.dimension_slice ds ON ds.id = cc.dimension_slice_id
    WHERE orig.hypertable_id IN (
        SELECT mat_hypertable_id FROM _timescaledb_catalog.continuous_agg
        WHERE user_view_name = ANY(cagg_names)
    ) AND NOT orig.dropped AND orig.compressed_chunk_id IS NULL
      AND ds.range_end > _time_threshold;


    -- 3a. Compressed chunk heap (covers all compressed chunks, regardless of whether TOAST exists)
    -- Before the fix: heap and TOAST were handled in the same PERFORM. WHERE reltoastrelid != 0
    -- filtered out chunks without TOAST, so their heap could not be prewarmed either.
    -- After the fix: split into two independent PERFORM statements; heap prewarm no longer depends on TOAST.
    -- In our data this case is unlikely because much of the compressed chunk data is stored in TOAST.
    -- This change is defensive for a low-probability case.
    PERFORM pg_prewarm(format('_timescaledb_internal.%I', comp.table_name)::regclass)
    FROM _timescaledb_catalog.chunk orig
             JOIN _timescaledb_catalog.chunk comp ON comp.id = orig.compressed_chunk_id
             JOIN pg_class comp_cls ON comp_cls.relname = comp.table_name
        AND comp_cls.relnamespace = '_timescaledb_internal'::regnamespace
             JOIN _timescaledb_catalog.chunk_constraint cc ON cc.chunk_id = orig.id
             JOIN _timescaledb_catalog.dimension_slice ds ON ds.id = cc.dimension_slice_id
    WHERE orig.hypertable_id IN (
        SELECT mat_hypertable_id FROM _timescaledb_catalog.continuous_agg
        WHERE user_view_name = ANY(cagg_names)
    ) AND NOT orig.dropped
      AND ds.range_end > _time_threshold;

    -- 3b. Compressed chunk TOAST (prewarm only when TOAST exists)
    PERFORM pg_prewarm(comp_cls.reltoastrelid)
    FROM _timescaledb_catalog.chunk orig
             JOIN _timescaledb_catalog.chunk comp ON comp.id = orig.compressed_chunk_id
             JOIN pg_class comp_cls ON comp_cls.relname = comp.table_name
        AND comp_cls.relnamespace = '_timescaledb_internal'::regnamespace
             JOIN _timescaledb_catalog.chunk_constraint cc ON cc.chunk_id = orig.id
             JOIN _timescaledb_catalog.dimension_slice ds ON ds.id = cc.dimension_slice_id
    WHERE orig.hypertable_id IN (
        SELECT mat_hypertable_id FROM _timescaledb_catalog.continuous_agg
        WHERE user_view_name = ANY(cagg_names)
    ) AND NOT orig.dropped AND comp_cls.reltoastrelid != 0
      AND ds.range_end > _time_threshold;

    -- 4. TOAST indexes for compressed chunks
    PERFORM pg_prewarm(i.indexrelid)
    FROM _timescaledb_catalog.chunk orig
             JOIN _timescaledb_catalog.chunk comp ON comp.id = orig.compressed_chunk_id
             JOIN pg_class comp_cls ON comp_cls.relname = comp.table_name
        AND comp_cls.relnamespace = '_timescaledb_internal'::regnamespace
             JOIN pg_index i ON i.indrelid = comp_cls.reltoastrelid
             JOIN _timescaledb_catalog.chunk_constraint cc ON cc.chunk_id = orig.id
             JOIN _timescaledb_catalog.dimension_slice ds ON ds.id = cc.dimension_slice_id
    WHERE orig.hypertable_id IN (
        SELECT mat_hypertable_id FROM _timescaledb_catalog.continuous_agg
        WHERE user_view_name = ANY(cagg_names)
    ) AND NOT orig.dropped AND comp_cls.reltoastrelid != 0
      AND ds.range_end > _time_threshold;


END;
$func$ LANGUAGE plpgsql;
