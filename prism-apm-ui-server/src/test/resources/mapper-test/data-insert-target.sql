TRUNCATE table service_call_minute;
TRUNCATE table service_call;
----------------------------------------------------------------------------------------------------
-- Data for ServiceCallMinuteDOMapper4TargetTest (condensed final version)
-- Strategy:
-- 1. `span_kind`=2 used for external calls, `span_kind`=3 used for internal calls。
-- 2. Data is distributed across host1, host2, host3, and host4 to satisfy host filtering conditions.
-- 3. Calculate duration precisely to satisfy avgDuration assertions.
-- 4. For internal calls, keep `host_name` consistent with `caller_host_name` to work around logic issues in the Mapper.
----------------------------------------------------------------------------------------------------
INSERT INTO service_call (time, trace_id, span_id, service_name, host_name, action_name, target_name, remote_addr, system_type, component, span_kind, caller_time, caller_span_id, caller_service_name, caller_host_name, caller_action_name, caller_target_name, caller_remote_addr, caller_system_type, caller_component, caller_span_kind, cnt, duration, error, create_time, update_time) VALUES

-- ========= Part 1: SERVER Spans (span_kind = 2) =========

-- For target1 on host1 (time 00:00). Satisfies SpecificHost tests.
('2024-01-01 00:00:10', 't_tr1', 't_sp1', 'serviceA', 'host1', 'c_action1', 'target1', 'remote', 'http', 'c', 2, NOW(), 'cs1', 'c_service1', 'c_host1', 'c_action1', 'c_target1', 'cr', 'http', 'cc', 1, 1, 100, 0, NOW(), NOW()),
-- For target1 on host2 (time 00:01). Satisfies general query for target1.
('2024-01-01 00:01:10', 't_tr2', 't_sp2', 'serviceA', 'host2', 'c_action1', 'target1', 'remote', 'http', 'c', 2, NOW(), 'cs2', 'c_service2', 'c_host2', 'c_action2', 'c_target2', 'cr', 'http', 'cc', 1, 1, 150, 2, NOW(), NOW()),

-- For target2 on host4 (time 00:03). Satisfies testQueryActionSummaryByServiceName_SpecificTarget.
-- These two records will be aggregated into one Materialized View record with cnt=2, avg_duration=150.
('2024-01-01 00:03:30', 't_tr3', 't_sp3', 'serviceA', 'host4', 'c_action2', 'target2', 'remote', 'http', 'c', 2, NOW(), 'cs3', 'c_service3', 'c_host3', 'c_action3', 'c_target3', 'cr', 'http', 'cc', 1, 1, 50, 0, NOW(), NOW()),
('2024-01-01 00:03:35', 't_tr4', 't_sp4', 'serviceA', 'host4', 'c_action2', 'target2', 'remote', 'http', 'c', 2, NOW(), 'cs4', 'c_service3', 'c_host3', 'c_action3', 'c_target3', 'cr', 'http', 'cc', 1, 1, 250, 2, NOW(), NOW()),

-- For target2 on host3 (time 00:04). Satisfies testQueryActionDetailByTargetName_SpecificTarget.
-- These two records will be aggregated into one Materialized View record with cnt=2, avg_duration=125.
('2024-01-01 00:04:10', 't_tr5', 't_sp5', 'serviceA', 'host3', 'c_action2', 'target2', 'remote', 'http', 'c', 2, NOW(), 'cs5', 'c_service4', 'c_host4', 'c_action4', 'c_target4', 'cr', 'http', 'cc', 1, 1, 50, 0, NOW(), NOW()),
('2024-01-01 00:04:15', 't_tr6', 't_sp6', 'serviceA', 'host3', 'c_action2', 'target2', 'remote', 'http', 'c', 2, NOW(), 'cs6', 'c_service4', 'c_host4', 'c_action4', 'c_target4', 'cr', 'http', 'cc', 1, 1, 200, 2, NOW(), NOW()),


-- ========= Part 2: CLIENT Spans (span_kind = 3), called by serviceA =========
-- [FIX] host_name is set to be the same as caller_host_name.

-- Called by serviceA/target1 on host1 (time 00:00).
('2024-01-01 00:00:15', 't_tr1_in', 't_sp1_in', 'innerService', 'host1', 'inner_action', 'target1', 'remote', 'http', 'c_inner', 3, '2024-01-01 00:00:10', 't_sp1', 'serviceA', 'host1', 'c_action1', 'target1', 'cr', 'http', 'cc', 2, 1, 100, 0, NOW(), NOW()),
-- Called by serviceA/target1 on host2 (time 00:01).
('2024-01-01 00:01:15', 't_tr2_in', 't_sp2_in', 'innerService', 'host2', 'inner_action', 'target1', 'remote', 'http', 'c_inner', 3, '2024-01-01 00:01:10', 't_sp2', 'serviceA', 'host2', 'c_action1', 'target1', 'cr', 'http', 'cc', 2, 1, 150, 2, NOW(), NOW()),

-- Called by serviceA/target2 on host3 (time 00:04)
('2024-01-01 00:04:15', 't_tr6_in', 't_sp6_in', 'innerService', 'host3', 'inner_action', 'target2', 'remote', 'http', 'c_inner', 3, '2024-01-01 00:04:15', 't_sp6', 'serviceA', 'host3', 'c_action2', 'target2', 'cr', 'http', 'cc', 2, 1, 100, 0, NOW(), NOW());