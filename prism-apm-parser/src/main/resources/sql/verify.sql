-- View scheduling details for all background jobs, including compression policies
SELECT
    job.hypertable_name AS view_name,          -- View name
    job.schedule_interval,                     -- Schedule interval (for example 1 day)

    -- Next execution time (converted to Beijing time for display)
    job.next_start AT TIME ZONE 'Asia/Shanghai' AS next_run_beijing_time,

    -- Compression policy configuration: how old data must be before compression (for example 7 days, 14 days, 30 days)
    (job.config->>'compress_after')::interval AS compress_older_than

FROM timescaledb_information.jobs job
WHERE
  -- Filter task type as 'compression policy'
    job.proc_name = 'policy_compression'
  AND
  -- Filter related table/view names
    job.hypertable_name IN (
                            'service_call_minute_entry',
                            'service_call_minute_dependency',
                            'service_call_minute_entry_dailychunk',
                            'service_call_minute_dependency_dailychunk',
                            'service_call_5min_entry',
                            'service_call_5min_entry',
                            'service_call_5min_dependency',
                            'service_call_hour_entry',
                            'service_call_hour_dependency',
                            'service_call_day_entry',
                            'service_call_day_dependency'
        )
ORDER BY next_run_beijing_time;



