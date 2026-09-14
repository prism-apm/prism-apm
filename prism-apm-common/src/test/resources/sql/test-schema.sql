create table IF NOT EXISTS service_parse_config
(
    id SERIAL PRIMARY KEY,
    service_name   text                     not null,
    parsed BOOLEAN,
    enabled BOOLEAN
);


create table IF NOT EXISTS field_content_replacer_rule
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
