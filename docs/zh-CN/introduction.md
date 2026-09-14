---
created: 2026-05-19 15:02:35
modified: 2026-08-10 17:33:54
---

# 关于

PrismAPM 是一个基于 OpenTelemetry 的分布式应用性能监控。主要目的不是只是用来查看endpoint等调用信息，更想在处理复杂和棘手的问题时提供重要依据。

你可以使用 PrismAPM 提供以下功能：

- 服务/endpoint/数据库等调用信息
- 所有服务的拓扑图
- 查看应用内部调用情况
- 指标比较
- 提供上下游数据
- 提供细粒度指标


# 重点

## 查看应用内部调用情况

当 endpoint 延迟增加时，我们关心是因为什么使 endpoint 变慢的，DB、Redis、还是其它外部调用。PrismAPM 提供功能使用户能够查看 endpoint 内部调用情况，例如：耗时，耗时占用百分比等。


![内部详情 - 按请求分类](../assets/zh-CN/features/service-metrics/endpoint-internal-details-lower-by-type.png)

## 指标比较

在发生问题时，我们还可以对具体的指标进行比较。对于 endpoint 等可以按时间维度和主机维度进行比较。

![RED 对比 - 时间对比](../assets/zh-CN/features/service-metrics/endpoint-red-comparison-time.png)


## 提供上下游数据

对于 endpoint，系统可以提供上游和下游的调用数据。

- 上游：我们可以知道 endpoint 被哪些服务调用了，以及调用的次数。
- 下游：我们可以知道 endpoint 都调用了哪些服务。

![来源统计](../assets/zh-CN/features/service-metrics/endpoint-source-stats.png)


## 拓扑图

可以通过拓扑图来了解服务的架构和服务调用情况。

![拓扑图](../assets/zh-CN/quickstart/topology.png)



## 其它

### 细粒度指标的使用

除了 Endpoint 名称和次数等信息外，系统还记录了其它很有用的信息。你可以深入了解这些数据，来找出问题是因为某个组件，还是某个组件版本，还是主机等。

下面是数据库中包含的一些字段和具体值。

- `system_type`: `http`, `kafka`, `redis`, `postgresql`...
- `component`: `io.opentelemetry.jdbc`,  `io.opentelemetry.apache-httpclient-2.0`, `io.opentelemetry.apache-httpclient-4.0`...
- `action_name`: `PUT`, `POST`, `SETEX`, `BATCH INSERT`...
- `host_name`: `your-server-prod-o-59595cdc5f-hkhz9`...
- `remote_addr`: `11.73.52.114`, `${public_ip}`...


### 查看逻辑分支调用情况

如果对逻辑分析代码加了 trace 输出，可以通过内部详情功能来查看调用各个分支的调用次数。或者通过是否调用了中间件或其它服务来统计分支的调用情况。


### 查看缓存调用情况

我们可以通过数据库统计功能，来查看某个时间段内缓存被调用的次数。



# 不能做什么

当前不适合用于下面这些场景：

- 分析超过系统等待时间或连续聚合窗口的超长调用。过长的 duration 可能无法在所有聚合视图中准确展示。
	- 例如：分数级数据的聚合时间是从当前时间到前 10 分钟的数据。如果 `14:00:00` 产生的数据执行了 15 分钟，`14:15:00` 才被处理。那这条数据不会被聚合。你也可以调用分钟数据的聚合间隔，但 5 分钟、小时、天级数据的聚合间隔可能会需要相应调整。



# 后继功能

- 支持 MCP
- ...



# 下一步

- Quickstart
- Feature 
- Architecture
- Configuration
