TRUNCATE table service_call_minute;
TRUNCATE table service_call;
----------------------------------------------------------------------------------------------------
-- Data for ServiceCallMinuteDOMapper4DBTest
-- Core fix: change span_kind from 2 to 3 for all records to match SpanKindEnum.CLIENT.getValue()
----------------------------------------------------------------------------------------------------
INSERT INTO service_call (time, trace_id, span_id, service_name, host_name, action_name, target_name, remote_addr, system_type, component, span_kind, caller_time, caller_span_id, caller_service_name, caller_host_name, caller_action_name, caller_target_name, caller_remote_addr, caller_system_type, caller_component, caller_span_kind, cnt, duration, error, create_time, update_time) VALUES
-- Data for target_name='action1' on host_name='host1'
-- Time Bucket: 2024-01-01 00:00:00
('2024-01-01 00:00:30', 'db_trace1', 'db_span1', 'serviceA', 'host1', 'callerAction1', 'action1', 'remote', 'mysql', 'db_component', 3, NOW(), 'cs1', 'cs', 'ch', 'ca', 'callerAction1', 'cr', 'http', 'cc', 1, 1, 100, 0, NOW(), NOW()),
-- Time Bucket: 2024-01-01 00:01:00
('2024-01-01 00:01:30', 'db_trace2', 'db_span2', 'serviceA', 'host1', 'callerAction1', 'action1', 'remote', 'mysql', 'db_component', 3, NOW(), 'cs2', 'cs', 'ch', 'ca', 'callerAction1', 'cr', 'http', 'cc', 1, 1, 100, 0, NOW(), NOW()),
('2024-01-01 00:01:35', 'db_trace3', 'db_span3', 'serviceA', 'host1', 'callerAction1', 'action1', 'remote', 'mysql', 'db_component', 3, NOW(), 'cs3', 'cs', 'ch', 'ca', 'callerAction1', 'cr', 'http', 'cc', 1, 1, 200, 1, NOW(), NOW()),

-- Data for target_name='action2' on host_name='host2'
-- Time Bucket: 2024-01-01 00:02:00
('2024-01-01 00:02:10', 'db_trace4', 'db_span4', 'serviceA', 'host2', 'callerAction2', 'action2', 'remote', 'mysql', 'db_component', 3, NOW(), 'cs4', 'cs', 'ch', 'ca', 'callerAction2', 'cr', 'http', 'cc', 1, 1, 200, 1, NOW(), NOW()),
('2024-01-01 00:02:20', 'db_trace5', 'db_span5', 'serviceA', 'host2', 'callerAction2', 'action2', 'remote', 'mysql', 'db_component', 3, NOW(), 'cs5', 'cs', 'ch', 'ca', 'callerAction2', 'cr', 'http', 'cc', 1, 1, 200, 1, NOW(), NOW()),
('2024-01-01 00:02:30', 'db_trace6', 'db_span6', 'serviceA', 'host2', 'callerAction2', 'action2', 'remote', 'mysql', 'db_component', 3, NOW(), 'cs6', 'cs', 'ch', 'ca', 'callerAction2', 'cr', 'http', 'cc', 1, 1, 200, 0, NOW(), NOW()),
-- Time Bucket: 2024-01-01 00:03:00
('2024-01-01 00:03:10', 'db_trace7', 'db_span7', 'serviceA', 'host2', 'callerAction2', 'action2', 'remote', 'mysql', 'db_component', 3, NOW(), 'cs7', 'cs', 'ch', 'ca', 'callerAction2', 'cr', 'http', 'cc', 1, 1, 250, 1, NOW(), NOW()),
('2024-01-01 00:03:20', 'db_trace8', 'db_span8', 'serviceA', 'host2', 'callerAction2', 'action2', 'remote', 'mysql', 'db_component', 3, NOW(), 'cs8', 'cs', 'ch', 'ca', 'callerAction2', 'cr', 'http', 'cc', 1, 1, 250, 1, NOW(), NOW()),
('2024-01-01 00:03:30', 'db_trace9', 'db_span9', 'serviceA', 'host2', 'callerAction2', 'action2', 'remote', 'mysql', 'db_component', 3, NOW(), 'cs9', 'cs', 'ch', 'ca', 'callerAction2', 'cr', 'http', 'cc', 1, 1, 250, 1, NOW(), NOW()),
('2024-01-01 00:03:40', 'db_trace10', 'db_span10', 'serviceA', 'host2', 'callerAction2', 'action2', 'remote', 'mysql', 'db_component', 3, NOW(), 'cs10', 'cs', 'ch', 'ca', 'callerAction2', 'cr', 'http', 'cc', 1, 1, 250, 0, NOW(), NOW()),

-- Data for target_name='action3' on host_name='host3'
-- Time Bucket: 2024-01-01 00:04:00
('2024-01-01 00:04:10', 'db_trace11', 'db_span11', 'serviceA', 'host3', 'callerAction3', 'action3', 'remote', 'mysql', 'db_component', 3, NOW(), 'cs11', 'cs', 'ch', 'ca', 'callerAction3', 'cr', 'http', 'cc', 1, 1, 300, 1, NOW(), NOW()),
('2024-01-01 00:04:15', 'db_trace12', 'db_span12', 'serviceA', 'host3', 'callerAction3', 'action3', 'remote', 'mysql', 'db_component', 3, NOW(), 'cs12', 'cs', 'ch', 'ca', 'callerAction3', 'cr', 'http', 'cc', 1, 1, 300, 1, NOW(), NOW()),
('2024-01-01 00:04:20', 'db_trace13', 'db_span13', 'serviceA', 'host3', 'callerAction3', 'action3', 'remote', 'mysql', 'db_component', 3, NOW(), 'cs13', 'cs', 'ch', 'ca', 'callerAction3', 'cr', 'http', 'cc', 1, 1, 300, 1, NOW(), NOW()),
('2024-01-01 00:04:25', 'db_trace14', 'db_span14', 'serviceA', 'host3', 'callerAction3', 'action3', 'remote', 'mysql', 'db_component', 3, NOW(), 'cs14', 'cs', 'ch', 'ca', 'callerAction3', 'cr', 'http', 'cc', 1, 1, 300, 1, NOW(), NOW()),
('2024-01-01 00:04:30', 'db_trace15', 'db_span15', 'serviceA', 'host3', 'callerAction3', 'action3', 'remote', 'mysql', 'db_component', 3, NOW(), 'cs15', 'cs', 'ch', 'ca', 'callerAction3', 'cr', 'http', 'cc', 1, 1, 300, 0, NOW(), NOW()),
-- Time Bucket: 2024-01-01 00:05:00
('2024-01-01 00:05:10', 'db_trace16', 'db_span16', 'serviceA', 'host3', 'callerAction3', 'action3', 'remote', 'mysql', 'db_component', 3, NOW(), 'cs16', 'cs', 'ch', 'ca', 'callerAction3', 'cr', 'http', 'cc', 1, 1, 350, 1, NOW(), NOW()),
('2024-01-01 00:05:15', 'db_trace17', 'db_span17', 'serviceA', 'host3', 'callerAction3', 'action3', 'remote', 'mysql', 'db_component', 3, NOW(), 'cs17', 'cs', 'ch', 'ca', 'callerAction3', 'cr', 'http', 'cc', 1, 1, 350, 1, NOW(), NOW()),
('2024-01-01 00:05:20', 'db_trace18', 'db_span18', 'serviceA', 'host3', 'callerAction3', 'action3', 'remote', 'mysql', 'db_component', 3, NOW(), 'cs18', 'cs', 'ch', 'ca', 'callerAction3', 'cr', 'http', 'cc', 1, 1, 350, 1, NOW(), NOW()),
('2024-01-01 00:05:25', 'db_trace19', 'db_span19', 'serviceA', 'host3', 'callerAction3', 'action3', 'remote', 'mysql', 'db_component', 3, NOW(), 'cs19', 'cs', 'ch', 'ca', 'callerAction3', 'cr', 'http', 'cc', 1, 1, 350, 1, NOW(), NOW()),
('2024-01-01 00:05:30', 'db_trace20', 'db_span20', 'serviceA', 'host3', 'callerAction3', 'action3', 'remote', 'mysql', 'db_component', 3, NOW(), 'cs20', 'cs', 'ch', 'ca', 'callerAction3', 'cr', 'http', 'cc', 1, 1, 350, 1, NOW(), NOW()),
('2024-01-01 00:05:35', 'db_trace21', 'db_span21', 'serviceA', 'host3', 'callerAction3', 'action3', 'remote', 'mysql', 'db_component', 3, NOW(), 'cs21', 'cs', 'ch', 'ca', 'callerAction3', 'cr', 'http', 'cc', 1, 1, 350, 0, NOW(), NOW());