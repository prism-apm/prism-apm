TRUNCATE table service_call_minute;
TRUNCATE table service_call;
----------------------------------------------------------------------------------------------------
-- Data for testQueryServiceSummaryByServiceNames... test series
----------------------------------------------------------------------------------------------------
INSERT INTO service_call (time, trace_id, span_id, service_name, host_name, action_name, target_name, remote_addr, system_type, component, span_kind, caller_time, caller_span_id, caller_service_name, caller_host_name, caller_action_name, caller_target_name, caller_remote_addr, caller_system_type, caller_component, caller_span_kind, cnt, duration, error, create_time, update_time) VALUES
-- for serviceNameA at 10:23.
('2024-10-31 10:23:00', 'trace1', 'span1', 'serviceNameA', 'hostA', 'actionA', 'targetA', 'remoteA', 'http', 'componentA', 2, '2024-10-31 10:23:00', 'caller_span1', 'caller_serviceA', 'caller_hostA', 'caller_actionA', 'caller_targetA', 'caller_remoteA', 'http', 'caller_componentA', 1, 1, 100, 0, NOW(), NOW()),
('2024-10-31 10:23:00', 'trace2', 'span2', 'serviceNameA', 'hostA', 'actionA', 'targetA', 'remoteA', 'http', 'componentA', 2, '2024-10-31 10:23:00', 'caller_span2', 'caller_serviceA', 'caller_hostA', 'caller_actionA', 'caller_targetA', 'caller_remoteA', 'http', 'caller_componentA', 1, 1, 200, 1, NOW(), NOW()),
-- for serviceNameA at 10:24.
('2024-10-31 10:24:00', 'trace3', 'span3', 'serviceNameA', 'hostA', 'actionA', 'targetA', 'remoteA', 'http', 'componentA', 2, '2024-10-31 10:24:00', 'caller_span3', 'caller_serviceA', 'caller_hostA', 'caller_actionA', 'caller_targetA', 'caller_remoteA', 'http', 'caller_componentA', 1, 1, 300, 0, NOW(), NOW()),
-- for serviceNameB at 10:23 and 10:24.
('2024-10-31 10:23:00', 'trace4', 'span4', 'serviceNameB', 'hostB', 'actionB', 'targetB', 'remoteB', 'http', 'componentB', 2, '2024-10-31 10:23:00', 'caller_span4', 'caller_serviceB', 'caller_hostB', 'caller_actionB', 'caller_targetB', 'caller_remoteB', 'http', 'caller_componentB', 1, 1, 100, 1, NOW(), NOW()),
('2024-10-31 10:24:00', 'trace5', 'span5', 'serviceNameB', 'hostB', 'actionB', 'targetB', 'remoteB', 'http', 'componentB', 2, '2024-10-31 10:24:00', 'caller_span5', 'caller_serviceB', 'caller_hostB', 'caller_actionB', 'caller_targetB', 'caller_remoteB', 'http', 'caller_componentB', 1, 1, 200, 1, NOW(), NOW()),

----------------------------------------------------------------------------------------------------
-- Data for testQueryMachineByServiceName AND testQueryServiceDetailByServiceName... test series
-- Core fix logic:
-- 1. All serviceA data points must be distributed across only two 5-minute time buckets.
--    - Choose the two time buckets .00:00:00. and .00:05:00.
-- 2. To achieve goal 1, move the host4 record originally at .03:00:00. into the .00:05:00. time bucket (for example, change it to .00:07:00.).
-- 3. This constrains all serviceA data (.host1., .host2., .host4.) to these two time buckets, satisfying the assertion in `testQueryServiceDetailByServiceName` (assertEquals(2, ...)).
-- 4. At the same time, all .host1. data remains in the single .00:00:00. time bucket, satisfying the assertion in `testQueryServiceDetailByServiceName_SpecificHost` (assertEquals(1, ...)).
-- 5. The three host names .host1., .host2., and .host4. still exist, so the assertion in `testQueryMachineByServiceName` (assertEquals(3, ...)) is also satisfied.
----------------------------------------------------------------------------------------------------
-- Bucket 1: '2024-01-01 00:00:00'
('2024-01-01 00:01:00', 'trace10', 'span10', 'serviceA', 'host1', 'a', 't', 'r', 'http', 'c', 2, '2024-01-01 00:01:00', 'cs10', 'cs', 'ch', 'ca', 'ct', 'cr', 'http', 'cc', 1, 2, 100, 1, NOW(), NOW()),
('2024-01-01 00:02:00', 'trace6', 'span6', 'serviceA', 'host1', 'action', 'target', 'remote', 'http', 'component', 2, '2024-01-01 01:00:00', 'c_span6', 'c_service', 'c_host', 'c_action', 'c_target', 'c_remote', 'http', 'c_component', 1, 1, 100, 0, NOW(), NOW()),
('2024-01-01 00:03:00', 'trace9', 'span9', 'serviceA', 'host1', 'action', 'target', 'remote', 'http', 'component', 2, '2024-01-01 04:00:00', 'c_span9', 'c_service', 'c_host', 'c_action', 'c_target', 'c_remote', 'http', 'c_component', 1, 1, 100, 0, NOW(), NOW()),

-- Bucket 2: '2024-01-01 00:05:00'
('2024-01-01 00:06:00', 'trace7', 'span7', 'serviceA', 'host2', 'action', 'target', 'remote', 'http', 'component', 2, '2024-01-01 02:00:00', 'c_span7', 'c_service', 'c_host', 'c_action', 'c_target', 'c_remote', 'http', 'c_component', 1, 1, 100, 0, NOW(), NOW()),
-- (FIXED) trace8: host4 data: changed time from 03:00:00 to 00:07:00 so it falls into the second time bucket
('2024-01-01 00:07:00', 'trace8', 'span8', 'serviceA', 'host4', 'action', 'target', 'remote', 'http', 'component', 2, '2024-01-01 03:00:00', 'c_span8', 'c_service', 'c_host', 'c_action', 'c_target', 'c_remote', 'http', 'c_component', 1, 1, 100, 0, NOW(), NOW());