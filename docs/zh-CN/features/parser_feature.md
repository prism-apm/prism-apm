---
created: 2026-07-29 15:04:09
modified: 2026-08-11 15:02:01
---

# 组件解析规则

## 作用

组件解析规则用于配置 Trace 数据与数据库字段之间的映射。

不同组件、Instrumentation 和 OpenTelemetry Semantic Conventions 版本使用的属性可能不同。例如，数据库类型可能来自 `db.system` 或 `db.system.name`，接口名称也可能来自不同的 Span Attribute。

将映射关系配置化后，可以：

- 调整字段取值来源，无需修改 Parser 代码。
- 兼容不同版本的 OpenTelemetry 属性。
- 为 HTTP、数据库、消息队列等组件定义不同的解析方式。
- 根据实际接入数据增加或修改自定义规则。


关于 `Parser` 的处理逻辑请参看 [Parser 架构](../architecture/parser_architecture.md)。

## 配置文件

OpenTelemetry 组件解析规则定义在：

```text
prism-apm-parser/src/main/resources/parser-component-config.yml
```

配置结构示例：

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

## 结构

### 必填与可选字段

配置可以分为三层：顶层配置、组件规则、输出字段规则。

顶层配置：

| 字段                            | 要求      | 说明                                    |
| ----------------------------- | ------- | ------------------------------------- |
| `version`                     | must    | 配置文件版本。                               |
| `parser`                      | must    | Parser 规则配置入口。                        |
| `parser.componentDefinitions` | 条件 must | 组件规则列表。和 `defaults` 至少配置一个。           |
| `parser.defaults`             | 条件 must | 默认规则。和 `componentDefinitions` 至少配置一个。 |

组件规则：

| 字段        | 要求       | 说明                                           |
| --------- | -------- | -------------------------------------------- |
| `name`    | must     | 规则名称。不可为空，不可重复。                              |
| `match`   | must     | 规则匹配条件。用于判断当前 Span 是否使用该规则。                  |
| `outputs` | optional | 字段输出规则。可以只配置当前组件需要覆盖的字段，其余字段由 `defaults` 补齐。 |

`match` 中的每一项：

| 部分    | 要求   | 说明                                     |
| ----- | ---- | -------------------------------------- |
| Key   | must | `Trace 属性路径`，例如 ` spans-scope__name `。 |
| Value | must | 正则表达式。                                 |

`outputs` 中的每个字段：

| 字段           | 要求       | 说明                  |
| ------------ | -------- | ------------------- |
| 输出字段名        | must     | 必须是支持的输出字段名。        |
| `vals`       | optional | 从 Trace 中取值的路径列表。   |
| `defaultVal` | optional | `vals` 取不到值时使用的默认值。 |

每个输出字段建议至少配置 `vals` 或 `defaultVal` 其中一个。

最终需要生成有效 SpanInfo 时，以下字段必须能取到有效值：

| 字段            | 要求       | 说明                       |
| ------------- | -------- | ------------------------ |
| `serviceName` | must     | 服务名称。不能为空。               |
| `hostName`    | must     | 主机名称。不能为 `null`，建议配置默认值。 |
| `spanKind`    | must     | Span 类型。必须是数字。           |
| `actionName`  | must     | 操作名称。不能为 `null`，可以为空字符串。 |
| `targetName`  | must     | 目标名称。不能为空。               |
| `component`   | must     | 组件名称。不能为空。               |
| `systemType`  | must     | 系统类型。不能为 `null`，建议配置默认值。 |
| `error`       | optional | 错误标记。取不到时按 `0` 处理。       |

### `componentDefinitions`

`componentDefinitions` 定义不同组件的解析规则，例如 HTTP、Redis、数据库和消息队列。

```yaml
componentDefinitions:
  - name: "redis"
    # ...
  - name: "database"
    # ...
  - name: "http"
    # ...
```

Parser 按配置顺序匹配：

- 第一个命中的规则生效，后续规则不再检查。
- 更具体的规则应放在通用规则之前。

例如，通用数据库匹配规则比较宽泛，如果把它放到 Redis 的规则前面，它会被先匹配上。因此 Redis 规则应放在通用数据库规则之前。

### 规则 (rule)

为每种组件定义字段映射规则。

以 Spring Schedule 为例：

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

规则的名称，不可重复。

```yaml
name: "spring-schedule"
```

`name` 用于标识规则和检查重复配置，不会直接写入数据库。

#### `match`

`match` 定义规则的匹配条件。可以设置多个匹配条件，从上向下进行匹配。任意一个条件命中，整条规则即为命中。

```yaml
match:
  - spans-scope__name: "io.opentelemetry.spring-scheduling-.+"
```

其中：
- Key：`Trace 属性路径`
- Value：正则表达式。

上面的配置表示：当 Instrumentation Scope 名称匹配 `io.opentelemetry.spring-scheduling-.+` 时，使用该规则。

> 注意：`io.opentelemetry.spring-scheduling` 中的 `.` 在 trace 信息的中实际上是普通的字符。但像这里这么写，其实被当成了正则中的通配符。


#### `outputs`

`outputs` 定义 Trace 数据与 Parser 输出字段之间的映射：

```yaml
outputs:
  targetName:
    vals:
      - "span__name"
```

上面的配置表示：从 Span 的 `name` 字段读取值，写入 `SpanInfo.targetName`，最终对应数据库中的 `target_name`。

`outputs` 支持配置以下字段：

| 配置字段          | `service_call` 字段 |
| ------------- | ----------------- |
| `serviceName` | `service_name`    |
| `hostName`    | `host_name`       |
| `spanKind`    | `span_kind`       |
| `actionName`  | `action_name`     |
| `targetName`  | `target_name`     |
| `component`   | `component`       |
| `systemType`  | `system_type`     |
| `error`       | `error`           |


#### `vals`

`vals` 定义当前字段值从哪个 `Trace 属性路径` 中取得。

可以设置多个值。然后按顺序读取，并且使用第一个非空值。

```yaml
targetName:
  vals:
    - "span-attributes__http.route"
    - "span-attributes__url.path"
    - "span__name"
```

#### `defaultVal`

当 `vals` 中的属性全部不存在时，使用 `defaultVal` 的值。这个值会原样设置到对应的字段上。

```yaml
systemType:
  vals:
    - "span-attributes__NON_EXIST_ATTRIBUTE"
  defaultVal: "spring-schedule"
```

注意，如果这里你想使用 `defaultVal` 设置一个固定值，但因为 `defaults` 里的设置而无法实现的话，你可以为 `vals` 指定一个不存在的路径。例如：`span-attributes__NON_EXIST_ATTRIBUTE`。

这里的 `NON_EXIST_ATTRIBUTE` 是刻意指定的不存在属性，用于跳过 Trace 取值并使用 ` defaultVal `。这个你也可以使用其它


### `defaults`

`defaults` 用来设置默认规则。当匹配不上 `componentDefinitions` 中定义的规则的情况下，就会用 `defaults` 中的规则来处理。

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

`outputs` 的定义和 `规则` 中的 `outputs` 定义一致。项目启动时会将 `defaults` 合并到每一条组件规则中：

- 组件规则未定义某个字段时，使用完整的默认规则。
- 组件规则定义了字段但缺少 `vals` 时，会使用此 `vals`。
- 组件规则缺少 `defaultVal` 时，会使用此 `defaultVal`。
- 组件规则已经定义 `vals` 和 `defaultVal` 和的内容不会被覆盖。


## 其它

### `Trace 属性路径`

属性路径使用 `__` 分隔来源和字段。

每个路径的第 1 个字段只能是下面这 4 个字段其中一个。从第 2 个字段开始，就是对应的属性名称。

第 1 字段列表：

| 前缀                    | 对应属性                                |
| --------------------- | ----------------------------------- |
| `resource-attributes` | `resourceSpans.resource.attributes` |
| `span`                | `scopeSpans.spans[i]`               |
| `spans-scope`         | `scopeSpans.scope`                  |
| `span-attributes`     | `scopeSpans.spans[i].attributes`    |

例如：

```text
resource-attributes__service.name
span-attributes__http.request.method
```

对应的 json：


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




# 字段替换规则

`field_content_replacer_rule` 表用于根据自定义规则，来修改 trace 中的内容。此规则用于把 trace 信息提取到 SpanInfo 之前，对信息进行修改。

此功能暂没有画面进行录入，需要直接向数据库里插入数据。

替换规则在启动时加载，并且每分钟刷新一次。刷新失败时继续使用上一次有效配置。

## 可被修改的字段

当前可被修改的字段包括：

- `actionName`
- `targetName`
- `remoteAddr`


## 定位条件

规则通过以下字段定位：

- `service_name`
- `component`


`service_name` 和 `component` 支持 `__ANY__` 通配符。

匹配优先级：

1. 服务名和组件精确匹配。
2. 服务名为 `__ANY__`。
3. 组件为 `__ANY__`。

当前实现中，如果同时存在服务名通配规则和组件通配规则，只会尝试服务名通配规则。后面会进行修改。


### 注意

如果一条 span 匹配两条规则，其中一条的 `service_name` 设置了 `__ANY__`，另一条的 `component` 设置了 `__ANY__`。这种情况下，会优先匹配 `service_name` 设置了 `__ANY__` 的那条规则。

例如，有下面有一条数据和两条规则。数据会匹配 `rule 1`。
- Data: 
	- service name: `order` 
	- component: `io.opentelemetry.okhttp-3.0`
- rule 1: service name: `__ANY__`, component: `io.opentelemetry.okhttp-3.0` 
- rule 2: service name: order, component: `__ANY__`

## 替换类型

支持两种替换类型：

| 类型              | 行为             |
| --------------- | -------------- |
| `regex`         | 使用正则表达式替换字段内容。 |
| `direct_return` | 忽略原值，直接返回配置值。  |

## 例子

下面的规则用于处理 `io.opentelemetry.http-url-connection` 组件的 `targetName` 字段：

```sql
INSERT INTO field_content_replacer_rule ("service_name", "component", "span_kind", "field_name", "match_type", "search_content", "replacement", "enabled")
VALUES ('__ANY__', 'io.opentelemetry.http-url-connection', 0, 'targetName', 'regex', '(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})(:\d+)?', '${ip}:${port}', 't');
```

规则含义：

| 字段               | 值                                             | 含义                        |
| ---------------- | --------------------------------------------- | ------------------------- |
| `service_name`   | `__ANY__`                                     | 匹配任意服务。                   |
| `component`      | `io.opentelemetry.http-url-connection`        | 只匹配该组件产生的 Span。           |
| `span_kind`      | `0`                                           | 此字段暂未使用。                  |
| `field_name`     | `targetName`                                  | 修改 `SpanInfo.targetName`。 |
| `match_type`     | `regex`                                       | 使用正则替换。                   |
| `search_content` | `(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})(:\d+)?` | 匹配 IP 和可选端口。              |
| `replacement`    | `${ip}:${port}`                               | 替换后的内容。                   |
| `enabled`        | `t`                                           | 启用规则。                     |

处理效果：

- 当 `targetName` 中包含 `192.168.1.10:8080` 这类地址时，会按正则命中。
- 命中后，Parser 将原始 `targetName` 替换为配置的 `replacement`。
- 该规则适合把不稳定的 URL、主机地址或路径内容收敛成统一格式，减少数据库中的目标资源基数。





# 服务过滤

`service_parse_config` 表控制指定服务是否参与解析。

表结构请参考 `sql/11_create_app_config_objects.sql` 中的 `service_parse_config`。

字段说明：

| 字段             | 说明                                                  |
| -------------- | --------------------------------------------------- |
| `id`           | 主键 ID。                                              |
| `service_name` | 服务名，对应 Trace Resource Attributes 中的 `service.name`。 |
| `parsed`       | 是否解析该服务的链路数据。                                       |
| `enabled`      | 配置是否启用。                                             |

此功能暂没有画面进行录入，需要直接向数据库里插入数据。服务过滤配置当前只在 Parser 启动时加载，修改后需要重启 Parser。


服务过滤默认关闭：

```properties
service.parse.check=false
```

启用后：

- 未配置的服务默认解析。
- `parsed=true` 的服务正常解析。
- `parsed=false` 或 `parsed` 为空的服务被跳过。
