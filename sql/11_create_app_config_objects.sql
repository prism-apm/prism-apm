-- >>> Create alert rule table start
CREATE TABLE "topology_warning_rules"
(
    "id"            SERIAL                                      NOT NULL,
    "rule_name"     varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
    "rule_details"  json                                        NOT NULL,
    "user_id"       int4                                        NOT NULL,
    "enabled"       int4 DEFAULT 0,
    "service_name"  varchar(255) COLLATE "pg_catalog"."default",
    "created_at"    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    "updated_at"    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    "warning_level" int4 DEFAULT 1,
    PRIMARY KEY ("id") -- Use id as the primary key
);
-- >>> Create alert rule table end

-- >>> Create permission-related tables start
-- 1. Entity tables

-- User table (users)
CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       username VARCHAR(255) NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       enabled BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE users IS 'User table';
COMMENT ON COLUMN users.id IS 'Primary key ID';
COMMENT ON COLUMN users.username IS 'Username';
COMMENT ON COLUMN users.password IS 'Password (should store a hash value)';
COMMENT ON COLUMN users.enabled IS 'Whether the account is enabled';
COMMENT ON COLUMN users.created_at IS 'Record creation time';
COMMENT ON COLUMN users.updated_at IS 'Record last update time';


-- Role table (roles)
CREATE TABLE roles (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE roles IS 'Role table';
COMMENT ON COLUMN roles.id IS 'Primary key ID';
COMMENT ON COLUMN roles.name IS 'Role name';
COMMENT ON COLUMN roles.created_at IS 'Record creation time';
COMMENT ON COLUMN roles.updated_at IS 'Record last update time';


-- Permission table (permissions)
CREATE TABLE permissions (
                             id SERIAL PRIMARY KEY,
                             name VARCHAR(255) NOT NULL,
                             url VARCHAR(255) NOT NULL,
                             created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                             updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE permissions IS 'Permission table';
COMMENT ON COLUMN permissions.id IS 'Primary key ID';
COMMENT ON COLUMN permissions.name IS 'Permission name';
COMMENT ON COLUMN permissions.url IS 'Endpoint path';
COMMENT ON COLUMN permissions.created_at IS 'Record creation time';
COMMENT ON COLUMN permissions.updated_at IS 'Record last update time';


-- Project group table (project_groups)
CREATE TABLE project_groups (
                                id SERIAL PRIMARY KEY,
                                name VARCHAR(255) NOT NULL,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE project_groups IS 'Project group table';
COMMENT ON COLUMN project_groups.id IS 'Primary key ID';
COMMENT ON COLUMN project_groups.name IS 'Project group name';
COMMENT ON COLUMN project_groups.created_at IS 'Record creation time';
COMMENT ON COLUMN project_groups.updated_at IS 'Record last update time';


-- Project table (projects)
CREATE TABLE projects (
                          id SERIAL PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          display VARCHAR(255),
                          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE projects IS 'Project table';
COMMENT ON COLUMN projects.id IS 'Primary key ID';
COMMENT ON COLUMN projects.name IS 'Project name (used for internal identification)';
COMMENT ON COLUMN projects.display IS 'Project display name (used in the UI)';
COMMENT ON COLUMN projects.created_at IS 'Record creation time';
COMMENT ON COLUMN projects.updated_at IS 'Record last update time';


-- 2. Relationship tables (join tables)

-- Role-user relationship table (role_user)
CREATE TABLE role_user (
                           id SERIAL PRIMARY KEY,
                           role_id INTEGER NOT NULL,
                           user_id INTEGER NOT NULL,
                           created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE role_user IS 'Role-user relationship table (many-to-many)';
COMMENT ON COLUMN role_user.role_id IS 'ID referencing the role table';
COMMENT ON COLUMN role_user.user_id IS 'ID referencing the user table';
COMMENT ON COLUMN role_user.created_at IS 'Record creation time';
COMMENT ON COLUMN role_user.updated_at IS 'Record last update time';


-- Role-permission relationship table (role_permission)
CREATE TABLE role_permission (
                                 id SERIAL PRIMARY KEY,
                                 role_id INTEGER NOT NULL,
                                 permission_id INTEGER NOT NULL,
                                 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE role_permission IS 'Role-permission relationship table (many-to-many)';
COMMENT ON COLUMN role_permission.role_id IS 'ID referencing the role table';
COMMENT ON COLUMN role_permission.permission_id IS 'ID referencing the permission table';
COMMENT ON COLUMN role_permission.created_at IS 'Record creation time';
COMMENT ON COLUMN role_permission.updated_at IS 'Record last update time';


-- User-project-group relationship table (user_project_group)
CREATE TABLE user_project_group (
                                    id SERIAL PRIMARY KEY,
                                    user_id INTEGER NOT NULL,
                                    project_group_id INTEGER NOT NULL,
                                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE user_project_group IS 'User-project-group relationship table (many-to-many)';
COMMENT ON COLUMN user_project_group.user_id IS 'ID referencing the user table';
COMMENT ON COLUMN user_project_group.project_group_id IS 'ID referencing the project group table';
COMMENT ON COLUMN user_project_group.created_at IS 'Record creation time';
COMMENT ON COLUMN user_project_group.updated_at IS 'Record last update time';


-- Project-project-group relationship table (project_project_group)
CREATE TABLE project_project_group (
                                       id SERIAL PRIMARY KEY,
                                       project_id INTEGER NOT NULL,
                                       project_group_id INTEGER NOT NULL,
                                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE project_project_group IS 'Project-project-group relationship table (many-to-many)';
COMMENT ON COLUMN project_project_group.project_id IS 'ID referencing the project table';
COMMENT ON COLUMN project_project_group.project_group_id IS 'ID referencing the project group table';
COMMENT ON COLUMN project_project_group.created_at IS 'Record creation time';
COMMENT ON COLUMN project_project_group.updated_at IS 'Record last update time';
-- >>> Create permission-related tables end

-- >>> Initialize database table routing rules start
CREATE TABLE "table_routing_rules"
(
    "id"              SERIAL                                      NOT NULL,
    "min_seconds"     int8,                                       -- Corresponds to Long minSeconds (nullable)
    "max_seconds"     int8,                                       -- Corresponds to Long maxSeconds (nullable)
    "rule_type"       varchar(50) COLLATE "pg_catalog"."default", --
    "target_table"    varchar(255) COLLATE "pg_catalog"."default",
    "optimized_table" varchar(255) COLLATE "pg_catalog"."default",
    "chunk_lag_days"  int4 NOT NULL DEFAULT 0,
    "tag"             varchar(50) COLLATE "pg_catalog"."default", --
    "data_step"       int4 NOT NULL DEFAULT 0,
    "created_at"      TIMESTAMPTZ NOT NULL DEFAULT NOW(),         --
    "updated_at"      TIMESTAMPTZ NOT NULL DEFAULT NOW(),         --
    PRIMARY KEY ("id")
);

COMMENT ON TABLE "table_routing_rules" IS 'APM table routing rule configuration';
COMMENT ON COLUMN "table_routing_rules"."min_seconds" IS 'Minimum duration in seconds';
COMMENT ON COLUMN "table_routing_rules"."max_seconds" IS 'Maximum duration in seconds';
COMMENT ON COLUMN "table_routing_rules"."rule_type" IS 'Rule type: SIMPLE, COMPLEX';
COMMENT ON COLUMN "table_routing_rules"."target_table" IS 'Original target table name';
COMMENT ON COLUMN "table_routing_rules"."optimized_table" IS 'Optimized table name';
COMMENT ON COLUMN "table_routing_rules"."chunk_lag_days" IS 'Chunk lag in days';

-- <<< Initialize database table routing rules end



-- >>> Configure whether to process Trace data for each service start
create table service_parse_config
(
    id SERIAL PRIMARY KEY,
    service_name   text                     not null,
    parsed BOOLEAN,
    enabled BOOLEAN
);

COMMENT ON TABLE service_parse_config IS 'Service parsing configuration table';
COMMENT ON COLUMN service_parse_config.id IS 'Primary key ID';
COMMENT ON COLUMN service_parse_config.service_name IS 'Service name';
COMMENT ON COLUMN service_parse_config.parsed IS 'Whether to parse Trace data for this service';
COMMENT ON COLUMN service_parse_config.enabled IS 'Whether the configuration is enabled';
-- >>> Configure whether to process Trace data for each service end


-- >>> Configure field replacement rules for Trace data start
create table field_content_replacer_rule
(
    id SERIAL PRIMARY KEY,
    service_name   text                     not null,
    component      text                     not null,
    span_kind      SMALLINT                 not null, -- reserved, unused currently
    field_name     text                     not null,
    match_type     text                     not null,
    search_content text                     not null,
    replacement    text                     not null,
    enabled        BOOLEAN                  not null
);

COMMENT ON TABLE field_content_replacer_rule IS 'Field content replacement rule table';
COMMENT ON COLUMN field_content_replacer_rule.id IS 'Primary key ID';
COMMENT ON COLUMN field_content_replacer_rule.service_name IS 'Service name; supports the __ANY__ wildcard';
COMMENT ON COLUMN field_content_replacer_rule.component IS 'Component name; supports the __ANY__ wildcard';
COMMENT ON COLUMN field_content_replacer_rule.span_kind IS 'Span type; reserved and currently unused';
COMMENT ON COLUMN field_content_replacer_rule.field_name IS 'Field name whose content should be replaced';
COMMENT ON COLUMN field_content_replacer_rule.match_type IS 'Match type: regex, direct_return';
COMMENT ON COLUMN field_content_replacer_rule.search_content IS 'Match content or regular expression';
COMMENT ON COLUMN field_content_replacer_rule.replacement IS 'Replacement content';
COMMENT ON COLUMN field_content_replacer_rule.enabled IS 'Whether the rule is enabled';
-- >>> Configure field replacement rules for Trace data end
