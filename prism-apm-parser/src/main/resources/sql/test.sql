-- First use select now() from service_call; to view the current system time.
-- Then change the data time below to a time before the current system time. The changed time should satisfy: "system time - add_continuous_aggregate_policy start_offset" < changed time < system time





-- Insert data into the service_call table. All records are in the same minute but have different seconds
-- Only 1 record exists after aggregation.
-- Confirm whether aggregate fields are correct
INSERT INTO service_call (time, service_name, host_name, action_name, component, span_kind, caller_service_name, caller_host_name, caller_action_name, caller_component, caller_span_kind, duration, error, caller_time)
VALUES
    ('2024-09-11 04:57:05', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 100, 0, '2024-09-10 11:00:05'),
    ('2024-09-11 04:57:05', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 110, 0, '2024-09-10 11:00:05'),
    ('2024-09-11 04:57:05', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 120, 0, '2024-09-10 11:00:05'),
    ('2024-09-11 04:57:05', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 130, 0, '2024-09-10 11:00:05'),
    ('2024-09-11 04:57:05', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 140, 0, '2024-09-10 11:00:05'),
    ('2024-09-11 04:57:05', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 150, 0, '2024-09-10 11:00:05'),
    ('2024-09-11 04:57:05', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 160, 0, '2024-09-10 11:00:05'),
    ('2024-09-11 04:57:10', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 170, 0, '2024-09-10 11:00:05'),
    ('2024-09-11 04:57:20', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 180, 1, '2024-09-10 11:00:05'),
    ('2024-09-11 04:57:30', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 190, 1, '2024-09-10 11:00:05'),
    ('2024-09-11 04:57:45', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 200, 0, '2024-09-10 11:00:05');






-- Similar to the above, but with data across two minutes.
-- After aggregation, there are two records.
INSERT INTO service_call (time, service_name, host_name, action_name, component, span_kind, caller_service_name, caller_host_name, caller_action_name, caller_component, caller_span_kind, duration, error, caller_time)
VALUES
    ('2024-09-10 04:18:05', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 100, 0, '2024-09-10 11:00:05'),
    ('2024-09-10 04:18:10', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 110, 0, '2024-09-10 11:00:05'),
    ('2024-09-10 04:18:20', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 130, 1, '2024-09-10 11:00:05'),
    ('2024-09-10 04:18:30', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 160, 1, '2024-09-10 11:00:05'),
    ('2024-09-10 04:18:45', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 200, 0, '2024-09-10 11:00:05'),

    ('2024-09-10 04:19:05', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 300, 1, '2024-09-10 11:00:05'),
    ('2024-09-10 04:19:10', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 310, 1, '2024-09-10 11:00:05'),
    ('2024-09-10 04:19:20', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 330, 0, '2024-09-10 11:00:05'),
    ('2024-09-10 04:19:30', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 360, 0, '2024-09-10 11:00:05'),
    ('2024-09-10 04:19:45', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 400, 1, '2024-09-10 11:00:05');






-- Insert 14 records where every group-by field has a different value
-- After aggregation, there are 14 records.
INSERT INTO service_call (time, service_name, host_name, action_name, component, span_kind, caller_service_name, caller_host_name, caller_action_name, caller_component, caller_span_kind, duration, error, caller_time)
VALUES
    ('2024-09-10 04:34:00', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 100, 0, '2024-09-10 12:00:00'),
    ('2024-09-10 04:34:00', 'serviceB', 'host2', 'action2', 'component2', 'CLIENT', 'caller_serviceB', 'caller_host2', 'caller_action2', 'caller_component2', 'SERVER', 100, 0, '2024-09-10 12:00:00'),
    ('2024-09-10 04:34:00', 'serviceC', 'host3', 'action3', 'component3', 'SERVER', 'caller_serviceC', 'caller_host3', 'caller_action3', 'caller_component3', 'CLIENT', 100, 0, '2024-09-10 12:00:00'),
    ('2024-09-10 04:34:00', 'serviceD', 'host4', 'action4', 'component4', 'CLIENT', 'caller_serviceD', 'caller_host4', 'caller_action4', 'caller_component4', 'SERVER', 100, 0, '2024-09-10 12:00:00'),
    ('2024-09-10 04:34:00', 'serviceE', 'host5', 'action5', 'component5', 'SERVER', 'caller_serviceE', 'caller_host5', 'caller_action5', 'caller_component5', 'CLIENT', 100, 0, '2024-09-10 12:00:00'),
    ('2024-09-10 04:34:00', 'serviceF', 'host6', 'action6', 'component6', 'CLIENT', 'caller_serviceF', 'caller_host6', 'caller_action6', 'caller_component6', 'SERVER', 100, 0, '2024-09-10 12:00:00'),
    ('2024-09-10 04:34:00', 'serviceG', 'host7', 'action7', 'component7', 'SERVER', 'caller_serviceG', 'caller_host7', 'caller_action7', 'caller_component7', 'CLIENT', 100, 0, '2024-09-10 12:00:00'),
    ('2024-09-10 04:34:00', 'serviceH', 'host8', 'action8', 'component8', 'CLIENT', 'caller_serviceH', 'caller_host8', 'caller_action8', 'caller_component8', 'SERVER', 100, 0, '2024-09-10 12:00:00'),
    ('2024-09-10 04:34:00', 'serviceI', 'host9', 'action9', 'component9', 'SERVER', 'caller_serviceI', 'caller_host9', 'caller_action9', 'caller_component9', 'CLIENT', 100, 0, '2024-09-10 12:00:00'),
    ('2024-09-10 04:34:00', 'serviceJ', 'host10', 'action10', 'component10', 'CLIENT', 'caller_serviceJ', 'caller_host10', 'caller_action10', 'caller_component10', 'SERVER', 100, 0, '2024-09-10 12:00:00'),
    ('2024-09-10 04:34:00', 'serviceK', 'host11', 'action11', 'component11', 'SERVER', 'caller_serviceK', 'caller_host11', 'caller_action11', 'caller_component11', 'CLIENT', 100, 0, '2024-09-10 12:00:00'),
    ('2024-09-10 04:34:00', 'serviceL', 'host12', 'action12', 'component12', 'CLIENT', 'caller_serviceL', 'caller_host12', 'caller_action12', 'caller_component12', 'SERVER', 100, 0, '2024-09-10 12:00:00'),
    ('2024-09-10 04:34:00', 'serviceM', 'host13', 'action13', 'component13', 'SERVER', 'caller_serviceM', 'caller_host13', 'caller_action13', 'caller_component13', 'CLIENT', 100, 0, '2024-09-10 12:00:00'),
    ('2024-09-10 04:34:00', 'serviceN', 'host14', 'action14', 'component14', 'CLIENT', 'caller_serviceN', 'caller_host14', 'caller_action14', 'caller_component14', 'SERVER', 100, 0, '2024-09-10 12:00:00');













INSERT INTO service_call (time, service_name, host_name, action_name, component, span_kind, caller_service_name, caller_host_name, caller_action_name, caller_component, caller_span_kind, duration, error, caller_time)
VALUES
    ('2024-09-11 04:48:05', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 100, 0, '2024-09-10 11:00:05'),
    ('2024-09-11 04:58:05', 'serviceA', 'host1', 'action1', 'component1', 'SERVER', 'caller_serviceA', 'caller_host1', 'caller_action1', 'caller_component1', 'CLIENT', 110, 0, '2024-09-10 11:00:05'),
