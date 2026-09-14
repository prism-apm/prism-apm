-- ==================================================================
-- Manually refresh all continuous aggregate views (run after initial creation WITH NO DATA)
-- Refresh by dependency topology layers to ensure source layers are refreshed before upper layers
-- ==================================================================


-- ==================================================================
-- Tier 1: minute-level aggregates (directly based on the raw service_call table)
-- ==================================================================

CALL refresh_continuous_aggregate('service_call_minute_entry', NULL, NULL);
CALL refresh_continuous_aggregate('service_call_minute_dependency', NULL, NULL);
CALL refresh_continuous_aggregate('service_host_list_minute_entry', NULL, NULL);
CALL refresh_continuous_aggregate('service_topology_relation_minute_entry', NULL, NULL);
CALL refresh_continuous_aggregate('service_topology_relation_minute_dependency', NULL, NULL);

-- ==================================================================
-- Tier 2a: minute-level archives (based on minute-level aggregates)
-- ==================================================================

CALL refresh_continuous_aggregate('service_call_minute_entry_merged', NULL, NULL);
CALL refresh_continuous_aggregate('service_call_minute_dependency_merged', NULL, NULL);

-- ==================================================================
-- Tier 2b: 5-minute aggregates (based on minute-level aggregates)
-- ==================================================================

CALL refresh_continuous_aggregate('service_call_5min_entry', NULL, NULL);
CALL refresh_continuous_aggregate('service_call_5min_dependency', NULL, NULL);
CALL refresh_continuous_aggregate('service_host_list_5min_entry', NULL, NULL);
CALL refresh_continuous_aggregate('service_topology_relation_5min_entry', NULL, NULL);
CALL refresh_continuous_aggregate('service_topology_relation_5min_dependency', NULL, NULL);

-- ==================================================================
-- Tier 2c: 5-minute archives (based on 5-minute aggregates)
-- ==================================================================

CALL refresh_continuous_aggregate('service_call_5min_entry_merged', NULL, NULL);
CALL refresh_continuous_aggregate('service_call_5min_dependency_merged', NULL, NULL);

-- ==================================================================
-- Tier 3a: hourly aggregates (based on 5-minute aggregates)
-- ==================================================================

CALL refresh_continuous_aggregate('service_call_hour_entry', NULL, NULL);
CALL refresh_continuous_aggregate('service_call_hour_dependency', NULL, NULL);
CALL refresh_continuous_aggregate('service_host_list_hour_entry', NULL, NULL);
CALL refresh_continuous_aggregate('service_topology_relation_hour_entry', NULL, NULL);
CALL refresh_continuous_aggregate('service_topology_relation_hour_dependency', NULL, NULL);

-- ==================================================================
-- Tier 3b: hourly archives (based on hourly aggregates)
-- ==================================================================

CALL refresh_continuous_aggregate('service_call_hour_entry_merged', NULL, NULL);
CALL refresh_continuous_aggregate('service_call_hour_dependency_merged', NULL, NULL);

-- ==================================================================
-- Tier 4: daily aggregates (based on hourly aggregates)
-- ==================================================================

CALL refresh_continuous_aggregate('service_call_day_entry', NULL, NULL);
CALL refresh_continuous_aggregate('service_call_day_dependency', NULL, NULL);
CALL refresh_continuous_aggregate('service_host_list_day_entry', NULL, NULL);
CALL refresh_continuous_aggregate('service_topology_relation_day_entry', NULL, NULL);
CALL refresh_continuous_aggregate('service_topology_relation_day_dependency', NULL, NULL);
