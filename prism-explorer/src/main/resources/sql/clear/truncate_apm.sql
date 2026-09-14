TRUNCATE TABLE public.service_call, resource_hash_dictionary, projects;
-- ==================================================================
-- Manually clear all continuous aggregate views (drop all partitions through drop_chunks)
-- Applies to time columns of type TIMESTAMPTZ
-- ==================================================================

-- Tier 1: minute-level aggregates
SELECT drop_chunks('service_call_minute_entry', older_than => '3000-01-01'::timestamptz);
SELECT drop_chunks('service_call_minute_dependency', older_than => '3000-01-01'::timestamptz);
SELECT drop_chunks('service_host_list_minute_entry', older_than => '3000-01-01'::timestamptz);
SELECT drop_chunks('service_topology_relation_minute_entry', older_than => '3000-01-01'::timestamptz);
SELECT drop_chunks('service_topology_relation_minute_dependency', older_than => '3000-01-01'::timestamptz);

-- Tier 2a: minute-level archives
SELECT drop_chunks('service_call_minute_entry_merged', older_than => '3000-01-01'::timestamptz);
SELECT drop_chunks('service_call_minute_dependency_merged', older_than => '3000-01-01'::timestamptz);

-- Tier 2b: 5-minute aggregates
SELECT drop_chunks('service_call_5min_entry', older_than => '3000-01-01'::timestamptz);
SELECT drop_chunks('service_call_5min_dependency', older_than => '3000-01-01'::timestamptz);
SELECT drop_chunks('service_host_list_5min_entry', older_than => '3000-01-01'::timestamptz);
SELECT drop_chunks('service_topology_relation_5min_entry', older_than => '3000-01-01'::timestamptz);
SELECT drop_chunks('service_topology_relation_5min_dependency', older_than => '3000-01-01'::timestamptz);

-- Tier 2c: 5-minute archives
SELECT drop_chunks('service_call_5min_entry_merged', older_than => '3000-01-01'::timestamptz);
SELECT drop_chunks('service_call_5min_dependency_merged', older_than => '3000-01-01'::timestamptz);

-- Tier 3a: hourly aggregates
SELECT drop_chunks('service_call_hour_entry', older_than => '3000-01-01'::timestamptz);
SELECT drop_chunks('service_call_hour_dependency', older_than => '3000-01-01'::timestamptz);
SELECT drop_chunks('service_host_list_hour_entry', older_than => '3000-01-01'::timestamptz);
SELECT drop_chunks('service_topology_relation_hour_entry', older_than => '3000-01-01'::timestamptz);
SELECT drop_chunks('service_topology_relation_hour_dependency', older_than => '3000-01-01'::timestamptz);

-- Tier 3b: hourly archives
SELECT drop_chunks('service_call_hour_entry_merged', older_than => '3000-01-01'::timestamptz);
SELECT drop_chunks('service_call_hour_dependency_merged', older_than => '3000-01-01'::timestamptz);

-- Tier 4: daily aggregates
SELECT drop_chunks('service_call_day_entry', older_than => '3000-01-01'::timestamptz);
SELECT drop_chunks('service_call_day_dependency', older_than => '3000-01-01'::timestamptz);
SELECT drop_chunks('service_host_list_day_entry', older_than => '3000-01-01'::timestamptz);
SELECT drop_chunks('service_topology_relation_day_entry', older_than => '3000-01-01'::timestamptz);
SELECT drop_chunks('service_topology_relation_day_dependency', older_than => '3000-01-01'::timestamptz);