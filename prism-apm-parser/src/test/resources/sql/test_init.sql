


-- for TestNGDAOTestBase.java

CREATE TABLE IF NOT EXISTS service_call
(
    -- Callee information
    time                  TIMESTAMP WITH TIME ZONE NOT NULL,
    trace_id              TEXT                     NOT NULL,
    span_id               TEXT                     NOT NULL,
    service_name          TEXT                     NOT NULL,
    host_name             TEXT                     NOT NULL,
    action_name           TEXT                     NOT NULL,
    target_name        UUID                     NOT NULL,
    remote_addr           TEXT                     NOT NULL,
    system_type           TEXT                     NOT NULL,
    component             TEXT                     NOT NULL,
    span_kind             SMALLINT                 NOT NULL,

    -- Caller information
    caller_time           TIMESTAMP WITH TIME ZONE NOT NULL,
    caller_span_id        TEXT                     NOT NULL,
    caller_service_name   TEXT                     NOT NULL,
    caller_host_name      TEXT                     NOT NULL,
    caller_action_name    TEXT                     NOT NULL,
    caller_target_name UUID                     NOT NULL,
    caller_remote_addr    TEXT                     NOT NULL,
    caller_system_type    TEXT                     NOT NULL,
    caller_component      TEXT                     NOT NULL,
    caller_span_kind      SMALLINT                 NOT NULL,

    -- Metrics and Timestamps
    cnt                   BIGINT                   NOT NULL,
    duration              BIGINT                   NOT NULL,
    error                 BIGINT                   NOT NULL,
    create_time           TIMESTAMP WITH TIME ZONE NOT NULL,
    update_time           TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Primary Key Constraint
    CONSTRAINT service_call_pkey PRIMARY KEY (time, span_id)
);

