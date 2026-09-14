-- ============================================================
-- Disable all scheduled policies (for demo environments)
-- Run after init_db_apm.sql to remove all automatic scheduled jobs,
-- avoiding unnecessary background jobs during demos.
-- ============================================================

-- service_call base table
CALL remove_columnstore_policy('service_call', if_exists => true);
SELECT remove_retention_policy('service_call', if_exists => true);

-- ==================== entry aggregates ====================
CALL remove_columnstore_policy('service_call_minute_entry', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_call_minute_entry', if_exists => true);
SELECT remove_retention_policy('service_call_minute_entry', if_exists => true);

CALL remove_columnstore_policy('service_call_minute_entry_merged', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_call_minute_entry_merged', if_exists => true);
SELECT remove_retention_policy('service_call_minute_entry_merged', if_exists => true);

CALL remove_columnstore_policy('service_call_5min_entry', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_call_5min_entry', if_exists => true);
SELECT remove_retention_policy('service_call_5min_entry', if_exists => true);

CALL remove_columnstore_policy('service_call_5min_entry_merged', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_call_5min_entry_merged', if_exists => true);
SELECT remove_retention_policy('service_call_5min_entry_merged', if_exists => true);

CALL remove_columnstore_policy('service_call_hour_entry', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_call_hour_entry', if_exists => true);
SELECT remove_retention_policy('service_call_hour_entry', if_exists => true);

CALL remove_columnstore_policy('service_call_hour_entry_merged', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_call_hour_entry_merged', if_exists => true);
SELECT remove_retention_policy('service_call_hour_entry_merged', if_exists => true);

CALL remove_columnstore_policy('service_call_day_entry', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_call_day_entry', if_exists => true);
SELECT remove_retention_policy('service_call_day_entry', if_exists => true);

-- ==================== dependency aggregates ====================
CALL remove_columnstore_policy('service_call_minute_dependency', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_call_minute_dependency', if_exists => true);
SELECT remove_retention_policy('service_call_minute_dependency', if_exists => true);

CALL remove_columnstore_policy('service_call_minute_dependency_merged', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_call_minute_dependency_merged', if_exists => true);
SELECT remove_retention_policy('service_call_minute_dependency_merged', if_exists => true);

CALL remove_columnstore_policy('service_call_5min_dependency', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_call_5min_dependency', if_exists => true);
SELECT remove_retention_policy('service_call_5min_dependency', if_exists => true);

CALL remove_columnstore_policy('service_call_5min_dependency_merged', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_call_5min_dependency_merged', if_exists => true);
SELECT remove_retention_policy('service_call_5min_dependency_merged', if_exists => true);

CALL remove_columnstore_policy('service_call_hour_dependency', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_call_hour_dependency', if_exists => true);
SELECT remove_retention_policy('service_call_hour_dependency', if_exists => true);

CALL remove_columnstore_policy('service_call_hour_dependency_merged', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_call_hour_dependency_merged', if_exists => true);
SELECT remove_retention_policy('service_call_hour_dependency_merged', if_exists => true);

CALL remove_columnstore_policy('service_call_day_dependency', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_call_day_dependency', if_exists => true);
SELECT remove_retention_policy('service_call_day_dependency', if_exists => true);

-- ==================== host_list aggregates ====================
CALL remove_columnstore_policy('service_host_list_minute_entry', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_host_list_minute_entry', if_exists => true);
SELECT remove_retention_policy('service_host_list_minute_entry', if_exists => true);

CALL remove_columnstore_policy('service_host_list_5min_entry', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_host_list_5min_entry', if_exists => true);
SELECT remove_retention_policy('service_host_list_5min_entry', if_exists => true);

CALL remove_columnstore_policy('service_host_list_hour_entry', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_host_list_hour_entry', if_exists => true);
SELECT remove_retention_policy('service_host_list_hour_entry', if_exists => true);

CALL remove_columnstore_policy('service_host_list_day_entry', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_host_list_day_entry', if_exists => true);
SELECT remove_retention_policy('service_host_list_day_entry', if_exists => true);

-- ==================== topology_relation aggregates ====================
CALL remove_columnstore_policy('service_topology_relation_minute_entry', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_topology_relation_minute_entry', if_exists => true);
SELECT remove_retention_policy('service_topology_relation_minute_entry', if_exists => true);

CALL remove_columnstore_policy('service_topology_relation_minute_dependency', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_topology_relation_minute_dependency', if_exists => true);
SELECT remove_retention_policy('service_topology_relation_minute_dependency', if_exists => true);

CALL remove_columnstore_policy('service_topology_relation_5min_entry', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_topology_relation_5min_entry', if_exists => true);
SELECT remove_retention_policy('service_topology_relation_5min_entry', if_exists => true);

CALL remove_columnstore_policy('service_topology_relation_5min_dependency', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_topology_relation_5min_dependency', if_exists => true);
SELECT remove_retention_policy('service_topology_relation_5min_dependency', if_exists => true);

CALL remove_columnstore_policy('service_topology_relation_hour_entry', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_topology_relation_hour_entry', if_exists => true);
SELECT remove_retention_policy('service_topology_relation_hour_entry', if_exists => true);

CALL remove_columnstore_policy('service_topology_relation_hour_dependency', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_topology_relation_hour_dependency', if_exists => true);
SELECT remove_retention_policy('service_topology_relation_hour_dependency', if_exists => true);

CALL remove_columnstore_policy('service_topology_relation_day_entry', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_topology_relation_day_entry', if_exists => true);
SELECT remove_retention_policy('service_topology_relation_day_entry', if_exists => true);

CALL remove_columnstore_policy('service_topology_relation_day_dependency', if_exists => true);
SELECT remove_continuous_aggregate_policy('service_topology_relation_day_dependency', if_exists => true);
SELECT remove_retention_policy('service_topology_relation_day_dependency', if_exists => true);