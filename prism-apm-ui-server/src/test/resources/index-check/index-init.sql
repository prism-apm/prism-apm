-- Step 4: Create indexes
CREATE INDEX prism_callee_idx ON service_call_minute(service_name, span_kind, hashtext(target_name), system_type, host_name, time);
CREATE INDEX prism_caller_idx ON service_call_minute(caller_service_name, caller_span_kind, span_kind, hashtext(caller_target_name), time);