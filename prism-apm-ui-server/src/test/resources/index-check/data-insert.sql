TRUNCATE table service_call_minute;
TRUNCATE table service_call;

-- Step 3: Insert a large amount of [raw] data into the service_call table
-- Note: we insert into service_call, not service_call_minute
INSERT INTO service_call (
    time, trace_id, span_id, service_name, host_name, action_name, target_name, remote_addr, system_type, component, span_kind,
    caller_time, caller_span_id, caller_service_name, caller_host_name, caller_action_name, caller_target_name, caller_remote_addr, caller_system_type, caller_component, caller_span_kind,
    cnt, duration, error, create_time, update_time
)
SELECT
    -- Generate a random time within the past day
    now() - (random() * interval '1 day') AS time,
    md5(random()::text) AS trace_id, -- Random trace_id
    md5(random()::text) AS span_id, -- Random span_id
    'service_' || (1 + floor(random() * 10))::text AS service_name,
    'host_' || (1 + floor(random() * 5))::text AS host_name,
    'action_' || (1 + floor(random() * 20))::text AS action_name,
    'target_' || (1 + floor(random() * 10))::text AS target_name,
    '192.168.1.' || (1 + floor(random() * 254))::text AS remote_addr,
    'OS' AS system_type,
    (ARRAY['java-web-servlet', 'database', 'cache', 'other'])[floor(random() * 4 + 1)]::text AS component,
    (ARRAY[3, 3, 4, 5])[floor(random() * 4 + 1)]::smallint AS span_kind,
    -- Caller information
    now() - (random() * interval '1 day') AS caller_time,
    md5(random()::text) AS caller_span_id,
    'caller_service_' || (1 + floor(random() * 10))::text AS caller_service_name,
    'caller_host_' || (1 + floor(random() * 5))::text AS caller_host_name,
    'caller_action_' || (1 + floor(random() * 20))::text AS caller_action_name,
    'caller_target_' || (1 + floor(random() * 10))::text AS caller_target_name,
    '10.0.0.' || (1 + floor(random() * 254))::text AS caller_remote_addr,
    'Container' AS caller_system_type,
    (ARRAY['java-web-servlet', 'database', 'cache', 'other'])[floor(random() * 4 + 1)]::text AS caller_component,
    (ARRAY[2, 3, 4, 5])[floor(random() * 4 + 1)]::smallint AS caller_span_kind,
    -- Metrics
    1 AS cnt, -- Raw data; cnt is always 1
    floor(random() * 5000) AS duration, -- Random duration
    (CASE WHEN random() < 0.1 THEN 2 ELSE 0 END) AS error, -- 10% probability of error
    now() AS create_time,
    now() AS update_time
FROM generate_series(1, 10000); -- Generate 10,000 rows of test data