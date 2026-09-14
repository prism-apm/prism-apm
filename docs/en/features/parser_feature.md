---
created: 2026-07-29 15:04:09
modified: 2026-08-11 15:02:01
---

# Component Parsing Rules

## Purpose

Component parsing rules configure the mapping between Trace data and database fields.

Different components, instrumentation libraries, and OpenTelemetry Semantic Conventions versions may use different attributes. For example, database type may come from `db.system` or `db.system.name`, and endpoint names may also come from different Span Attributes.

After the mapping is configurable, you can:

- Adjust field value sources without changing Parser code.
- Support OpenTelemetry attribute differences across versions.
- Define different parsing methods for HTTP, databases, message queues, and other components.
- Add or modify custom rules according to actual ingested data.

For Parser processing logic, see [Parser architecture](../architecture/parser_architecture.md).

## Configuration File

OpenTelemetry component parsing rules are defined in:

```text
prism-apm-parser/src/main/resources/parser-component-config.yml
```

Configuration structure example:

```yaml
version: 1.0
parser:
  componentDefinitions:
    - name: "http"
      match:
        - span-attributes__http.request.method: ".*"
      outputs:
        actionName:
          vals:
            - "span-attributes__http.request.method"
        targetName:
          vals:
            - "span-attributes__http.route"
            - "span-attributes__url.path"
        systemType:
          defaultVal: "http"

  defaults:
    outputs:
      serviceName:
        vals:
          - "resource-attributes__service.name"
```

## Structure

### Required and Optional Fields

Configuration has three levels: top-level configuration, component rules, and output field rules.

Top-level configuration:

| Field | Requirement | Description |
| --- | --- | --- |
| `version` | must | Configuration file version. |
| `parser` | must | Parser rule configuration entry. |
| `parser.componentDefinitions` | conditionally must | Component rule list. At least one of this and `defaults` must be configured. |
| `parser.defaults` | conditionally must | Default rule. At least one of this and `componentDefinitions` must be configured. |

Component rule:

| Field | Requirement | Description |
| --- | --- | --- |
| `name` | must | Rule name. Must not be empty or duplicated. |
| `match` | must | Rule matching conditions, used to decide whether the current Span uses this rule. |
| `outputs` | optional | Field output rules. Configure only fields overridden by the current component; the remaining fields are filled by `defaults`. |

Each item in `match`:

| Part | Requirement | Description |
| --- | --- | --- |
| Key | must | `Trace attribute path`, for example `spans-scope__name`. |
| Value | must | Regular expression. |

Each field in `outputs`:

| Field | Requirement | Description |
| --- | --- | --- |
| Output field name | must | Must be a supported output field name. |
| `vals` | optional | List of paths to read values from Trace. |
| `defaultVal` | optional | Default value used when `vals` cannot provide a value. |

Each output field should configure at least one of `vals` or `defaultVal`.

To generate a valid `SpanInfo`, these fields must resolve to valid values:

| Field | Requirement | Description |
| --- | --- | --- |
| `serviceName` | must | Service name. Must not be empty. |
| `hostName` | must | Host name. Must not be `null`; configuring a default is recommended. |
| `spanKind` | must | Span kind. Must be a number. |
| `actionName` | must | Operation name. Must not be `null`; can be an empty string. |
| `targetName` | must | Target name. Must not be empty. |
| `component` | must | Component name. Must not be empty. |
| `systemType` | must | System type. Must not be `null`; configuring a default is recommended. |
| `error` | optional | Error flag. Treated as `0` when unavailable. |

### `componentDefinitions`

`componentDefinitions` defines parsing rules for different components, such as HTTP, Redis, databases, and message queues.

```yaml
componentDefinitions:
  - name: "redis"
    # ...
  - name: "database"
    # ...
  - name: "http"
    # ...
```

Parser matches rules in configuration order:

- The first matched rule takes effect, and later rules are not checked.
- More specific rules should be placed before more general rules.

For example, a generic database matching rule is broad. If it is placed before the Redis rule, it may match Redis first. Therefore, Redis rules should be placed before generic database rules.

### Rule

Define field mapping rules for each component.

Using Spring Schedule as an example:

```yaml
- name: "spring-schedule"
  match:
    - spans-scope__name: "io.opentelemetry.spring-scheduling-.+"
  outputs:
    actionName:
      vals:
        - "span-attributes__NON_EXIST_ATTRIBUTE"
      defaultVal: ""
    targetName:
      vals:
        - "span__name"
    systemType:
      vals:
        - "span-attributes__NON_EXIST_ATTRIBUTE"
      defaultVal: "spring-schedule"
```

#### `name`

Rule name. It must be unique.

```yaml
name: "spring-schedule"
```

`name` identifies the rule and checks for duplicate configuration. It is not written directly to the database.

#### `match`

`match` defines rule matching conditions. Multiple conditions can be configured and are matched from top to bottom. If any condition matches, the whole rule is considered matched.

```yaml
match:
  - spans-scope__name: "io.opentelemetry.spring-scheduling-.+"
```

Where:

- Key: `Trace attribute path`
- Value: regular expression.

The configuration above means: when the Instrumentation Scope name matches `io.opentelemetry.spring-scheduling-.+`, use this rule.

> Note: the `.` characters in `io.opentelemetry.spring-scheduling` are ordinary characters in trace data. Written this way, however, they are treated as regex wildcards.

#### `outputs`

`outputs` defines the mapping between Trace data and Parser output fields:

```yaml
outputs:
  targetName:
    vals:
      - "span__name"
```

This configuration reads the value from the Span `name` field, writes it to `SpanInfo.targetName`, and finally maps it to `target_name` in the database.

`outputs` supports these fields:

| Configuration field | `service_call` field |
| --- | --- |
| `serviceName` | `service_name` |
| `hostName` | `host_name` |
| `spanKind` | `span_kind` |
| `actionName` | `action_name` |
| `targetName` | `target_name` |
| `component` | `component` |
| `systemType` | `system_type` |
| `error` | `error` |

#### `vals`

`vals` defines which `Trace attribute path` is used to get the current field value.

Multiple paths can be configured. Parser reads them in order and uses the first non-empty value.

```yaml
targetName:
  vals:
    - "span-attributes__http.route"
    - "span-attributes__url.path"
    - "span__name"
```

#### `defaultVal`

When all attributes in `vals` do not exist, `defaultVal` is used. The value is assigned to the corresponding field as-is.

```yaml
systemType:
  vals:
    - "span-attributes__NON_EXIST_ATTRIBUTE"
  defaultVal: "spring-schedule"
```

If you want to set a fixed value through `defaultVal` but cannot because of settings in `defaults`, specify a non-existent path in `vals`, for example `span-attributes__NON_EXIST_ATTRIBUTE`.

`NON_EXIST_ATTRIBUTE` is intentionally configured as a missing attribute to skip Trace value extraction and use `defaultVal`. You can use another non-existent path as well.

### `defaults`

`defaults` sets default rules. When none of the rules in `componentDefinitions` match, the rules in `defaults` are used.

```yaml
defaults:
  outputs:
    serviceName:
      vals:
        - "resource-attributes__service.name"
    hostName:
      vals:
        - "resource-attributes__host.name"
      defaultVal: "unknown"
```

The definition of `outputs` is the same as `outputs` in a component rule. At project startup, `defaults` is merged into each component rule:

- If a component rule does not define a field, the complete default rule is used.
- If a component rule defines a field but lacks `vals`, the default `vals` is used.
- If a component rule lacks `defaultVal`, the default `defaultVal` is used.
- If a component rule already defines both `vals` and `defaultVal`, they are not overwritten.

## More

### `Trace Attribute Path`

Attribute paths use `__` to separate source and field.

The first part of each path can only be one of the following four prefixes. Starting from the second part, the value is the corresponding attribute name.

First-part list:

| Prefix | Corresponding attribute |
| --- | --- |
| `resource-attributes` | `resourceSpans.resource.attributes` |
| `span` | `scopeSpans.spans[i]` |
| `spans-scope` | `scopeSpans.scope` |
| `span-attributes` | `scopeSpans.spans[i].attributes` |

Examples:

```text
resource-attributes__service.name
span-attributes__http.request.method
```

Corresponding JSON:

```json
{
  "resourceSpans":
  [
    {
      "resource":
      {
        "attributes":
        [
          ...
          {
            "key": "service.name",
            "value":
            {
              "stringValue": "hello-service"
            }
          },
          ...
        ]
      },
      "scopeSpans":
      [
        {
          "scope":
          {
            "name": "io.opentelemetry.okhttp-3.0",
            "version": "2.18.1-alpha"
          },
          "spans":
          [
            {
              "traceId": "dcb0e2066c725da59bcba78ef8941974",
              "spanId": "d641e6b3213b2509",
              "parentSpanId": "54f50705be4e7303",
              "flags": 257,
              "name": "GET",
              "kind": 3,
              "startTimeUnixNano": "1755761181557970619",
              "endTimeUnixNano": "1755761181799061088",
              "attributes":
              [
                ...
                {
                  "key": "http.request.method",
                  "value":
                  {
                    "stringValue": "GET"
                  }
                },
                ...
              ],
              "status":
              {}
            },
          ]
        },
        ...
      ],
      "schemaUrl": "https://opentelemetry.io/schemas/1.24.0"
    }
  ]
}
```

# Field Replacement Rules

The `field_content_replacer_rule` table modifies trace content according to custom rules. This rule is applied before trace information is extracted into `SpanInfo`.

There is currently no UI for entering this feature; data must be inserted directly into the database.

Replacement rules are loaded at startup and refreshed once per minute. If refresh fails, the previous valid configuration continues to be used.

## Modifiable Fields

Current modifiable fields:

- `actionName`
- `targetName`
- `remoteAddr`

## Matching Conditions

Rules are located by these fields:

- `service_name`
- `component`

`service_name` and `component` support the `__ANY__` wildcard.

Matching priority:

1. Exact match on service name and component.
2. Service name is `__ANY__`.
3. Component is `__ANY__`.

In the current implementation, if both a service-name wildcard rule and a component wildcard rule exist, only the service-name wildcard rule is tried. This will be changed later.

### Note

If one Span matches two rules, one with `service_name` set to `__ANY__` and the other with `component` set to `__ANY__`, the rule whose `service_name` is `__ANY__` takes priority.

For example, given this data and two rules, the data matches `rule 1`.

- Data:
  - service name: `order`
  - component: `io.opentelemetry.okhttp-3.0`
- rule 1: service name: `__ANY__`, component: `io.opentelemetry.okhttp-3.0`
- rule 2: service name: `order`, component: `__ANY__`

## Replacement Types

Two replacement types are supported:

| Type | Behavior |
| --- | --- |
| `regex` | Replaces field content using a regular expression. |
| `direct_return` | Ignores the original value and directly returns the configured value. |

## Example

The following rule handles the `targetName` field for the `io.opentelemetry.http-url-connection` component:

```sql
INSERT INTO field_content_replacer_rule ("service_name", "component", "span_kind", "field_name", "match_type", "search_content", "replacement", "enabled")
VALUES ('__ANY__', 'io.opentelemetry.http-url-connection', 0, 'targetName', 'regex', '(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})(:\d+)?', '${ip}:${port}', 't');
```

Rule meaning:

| Field | Value | Meaning |
| --- | --- | --- |
| `service_name` | `__ANY__` | Matches any service. |
| `component` | `io.opentelemetry.http-url-connection` | Matches only Spans produced by this component. |
| `span_kind` | `0` | This field is not used yet. |
| `field_name` | `targetName` | Modifies `SpanInfo.targetName`. |
| `match_type` | `regex` | Uses regex replacement. |
| `search_content` | `(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})(:\d+)?` | Matches IP and optional port. |
| `replacement` | `${ip}:${port}` | Replacement content. |
| `enabled` | `t` | Enables the rule. |

Effect:

- When `targetName` contains an address such as `192.168.1.10:8080`, the regex matches it.
- After a match, Parser replaces the original `targetName` with the configured `replacement`.
- This rule is suitable for normalizing unstable URLs, host addresses, or path contents into a unified format and reducing target resource cardinality in the database.

# Service Filtering

The `service_parse_config` table controls whether specific services participate in parsing.

See `service_parse_config` in `sql/11_create_app_config_objects.sql` for the table structure.

Fields:

| Field | Description |
| --- | --- |
| `id` | Primary key ID. |
| `service_name` | Service name, corresponding to `service.name` in Trace Resource Attributes. |
| `parsed` | Whether to parse trace data for this service. |
| `enabled` | Whether the configuration is enabled. |

There is currently no UI for entering this feature; data must be inserted directly into the database. Service filtering configuration is currently loaded only when Parser starts, so Parser must be restarted after changes.

Service filtering is disabled by default:

```properties
service.parse.check=false
```

After enabling it:

- Unconfigured services are parsed by default.
- Services with `parsed=true` are parsed normally.
- Services with `parsed=false` or empty `parsed` are skipped.
