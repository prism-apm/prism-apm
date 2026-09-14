---
created: 2026-05-28 14:16:22
modified: 2026-08-10 17:21:13
---

# 使用扩展

在本项目中使用了 TimescaleDB 作为数据库。使用到了以下扩展：

- timescaledb
- timescaledb_toolkit: 使用 rollup 和 percentile_agg 来计算百分位数。
- pg_prewarm: 在一段时间不使用后，第一次查询会变慢。使用些扩展，每隔一段时间对核心数据进行预热。



# Timescaledb 的建议配置


可以使用下面的命令来查看 timescale 的建议配置：

```shell
timescaledb-tune --quiet --yes
```

# 配置例子

以下是 16C32G 机器的配置：

```
# ==========================================
# TimescaleDB & PostgreSQL 优化配置
# 硬件环境: 16 CPU, 32GB RAM, SSD
# ==========================================

# --- 1. 基础加载项 ---
shared_preload_libraries = 'timescaledb'  # 必须包含

# --- 2. 内存分配 (Memory) ---
# 保持 tune 计算的 25% 内存，用于共享缓冲
shared_buffers = 8022MB

# 告诉优化器 OS 有多少缓存可用 (75% RAM)，
effective_cache_size = 24067MB

# 增大工作内存 
work_mem = 128MB

# 维护内存，用于 VACUUM 和索引创建
maintenance_work_mem = 2047MB

# --- 3. 写入性能与 WAL (Write Ahead Log) ---
# 关闭同步提交 (原 on -> off)
# 牺牲极小的数据安全性(崩溃时丢失<1秒数据)换取 2-3倍 写入性能提升
synchronous_commit = off

# 增大 WAL 日志上限 (原 1GB -> 16GB)
# 减少 Checkpoint 频率，大幅降低 I/O 抖动
max_wal_size = 16GB
min_wal_size = 4GB

# 保持平滑刷盘
checkpoint_completion_target = 0.9
wal_buffers = 16MB

# --- 4. 并行处理与 Worker (Parallelism) ---
# 保持 tune 计算结果：CPU(16) + Timescale后台(16) + 预留 ≈ 35
max_worker_processes = 35

# 并行查询最大进程数 (限制为 CPU 核数)
max_parallel_workers = 16

# 单个查询最大并行度 (限制为半数核)
max_parallel_workers_per_gather = 8

# 激进的并行查询成本模型
# 降低并行启动成本阈值，极大鼓励优化器选择并行计划
parallel_setup_cost = 10
# 降低元组传递成本，减少进程间通信开销的惩罚计算
parallel_tuple_cost = 0.001
# 降低触发并行扫描的表大小阈值，让小表/小块也能并行扫描
min_parallel_table_scan_size = 1MB

# TimescaleDB 后台任务数 (用于压缩、持续聚合等)
timescaledb.max_background_workers = 16

# --- 5. 存储与 I/O 优化 (SSD) ---
# 告诉优化器随机读很快 (SSD 特性)
random_page_cost = 1.1

# 允许高并发 I/O 请求
effective_io_concurrency = 256

# 长文本压缩算法 (如果 PG 版本 >= 14)
default_toast_compression = lz4

# --- 6. 自动维护与锁 ---
# 增加自动清理的工人数量
autovacuum_max_workers = 10
autovacuum_naptime = 10

# [重要] 适应 TimescaleDB 多子表的锁需求
max_locks_per_transaction = 512

# --- 7. 其他 ---
# 关闭 JIT 以提高混合负载下的响应速度
jit = off
default_statistics_target = 100
max_connections = 100

# checkpoint_timeout 是 PostgreSQL 中控制自动 WAL (Write-Ahead Logging) 检查点发生频率的关键参数。它定义了在强制执行检查点之前，系统可以等待的最长时间。
checkpoint_timeout = 30min

# ----------------------------------------
# Background Writer (后台写进程) 优化
# 目标：让后台进程更勤快地刷脏页，避免聚合任务被阻塞
# ----------------------------------------

# 巡检频率：从默认的 200ms 缩短到 10ms
bgwriter_delay = 10ms

# 单次工作量：从默认的 100页 增加到 1000页 (约 8MB)
bgwriter_lru_maxpages = 1000

# 预测激进程度：从默认的 2.0 提升到 4.0
bgwriter_lru_multiplier = 4.0

# 开启此选项以解决从节点报错: "canceling statement due to conflict with recovery"
hot_standby_feedback = on

# plan 中显示 io 执行时间。
track_io_timing = on

# ==========================================
# --- 8. 查询计划与缓存 (Query Planning & Cache) ---
# ==========================================

# 强制使用自定义执行计划
# 解决预编译语句 (Prepared Statements) 导致 TimescaleDB 无法进行分块裁剪 (Chunk Exclusion) 的问题。
plan_cache_mode = 'force_custom_plan'
```

查看设置是否生效：

```sql
SELECT 
    name AS "Parameter",
    setting AS "Current Value",
    unit AS "Unit",
    source AS "Source",
    pending_restart AS "Restart Pending",
    CASE 
        WHEN name IN ('shared_preload_libraries') THEN '1. Basic Loading'
        WHEN name IN ('shared_buffers', 'effective_cache_size', 'work_mem', 'maintenance_work_mem') THEN '2. Memory'
        WHEN name IN ('synchronous_commit', 'max_wal_size', 'min_wal_size', 'checkpoint_completion_target', 'wal_buffers', 'checkpoint_timeout') THEN '3. Write & WAL'
        WHEN name IN ('max_worker_processes', 'max_parallel_workers', 'max_parallel_workers_per_gather', 'timescaledb.max_background_workers', 'parallel_setup_cost', 'parallel_tuple_cost', 'min_parallel_table_scan_size') THEN '4. Parallelism & Workers'
        WHEN name IN ('random_page_cost', 'effective_io_concurrency', 'default_toast_compression', 'track_io_timing') THEN '5. Storage & IO'
        WHEN name IN ('autovacuum_max_workers', 'autovacuum_naptime', 'max_locks_per_transaction') THEN '6. Maintenance & Locks'
        WHEN name IN ('jit', 'default_statistics_target', 'max_connections', 'hot_standby_feedback') THEN '7. Misc & Replication'
        WHEN name IN ('bgwriter_delay', 'bgwriter_lru_maxpages', 'bgwriter_lru_multiplier') THEN '8. Background Writer'
        WHEN name IN ('plan_cache_mode') THEN '9. Query Planning & Cache'
        ELSE 'Uncategorized'
    END AS "Category"
FROM 
    pg_settings
WHERE 
    name IN (
        -- 1. Basic
        'shared_preload_libraries',
        -- 2. Memory
        'shared_buffers', 'effective_cache_size', 'work_mem', 'maintenance_work_mem',
        -- 3. WAL
        'synchronous_commit', 'max_wal_size', 'min_wal_size', 'checkpoint_completion_target', 'wal_buffers', 'checkpoint_timeout',
        -- 4. Parallelism 
        'max_worker_processes', 'max_parallel_workers', 'max_parallel_workers_per_gather', 'timescaledb.max_background_workers',
        'parallel_setup_cost', 'parallel_tuple_cost', 'min_parallel_table_scan_size',
        -- 5. IO
        'random_page_cost', 'effective_io_concurrency', 'default_toast_compression', 'track_io_timing',
        -- 6. Maintenance
        'autovacuum_max_workers', 'autovacuum_naptime', 'max_locks_per_transaction',
        -- 7. Misc
        'jit', 'default_statistics_target', 'max_connections', 'hot_standby_feedback',
        -- 8. BgWriter
        'bgwriter_delay', 'bgwriter_lru_maxpages', 'bgwriter_lru_multiplier',
        -- 9. Query Planning & Cache
        'plan_cache_mode'
    )
ORDER BY 
    "Category", "Parameter";
```

# 优化配置

以下配置主要面向生产环境。调整前建议先确认当前写入量、查询并发、磁盘类型、是否有读副本，以及 Parser 写入延迟是否已经受数据库侧限制。

## 存储拆分

安装 TimescaleDB 后，建议尽量把以下数据放到不同的磁盘或不同的高性能存储卷上：

- 数据目录：PostgreSQL/TimescaleDB 主数据目录，包含表、索引、chunk、连续聚合物化数据等。
- WAL 日志：PostgreSQL 写前日志，写入高峰时会持续顺序写入。
- WAL archive：归档 WAL，用于备份、恢复或主从复制场景。

建议：

- 数据目录使用容量较大的 SSD/NVMe。
- WAL 日志使用低延迟、高写入性能的 SSD/NVMe。
- WAL archive 使用容量更大的独立磁盘或对象存储挂载目录，避免归档写入挤占主数据盘 I/O。
- 如果使用 Docker 或 Kubernetes，建议为数据目录和 WAL 目录配置独立 volume。
- 不要把 WAL archive 放在主数据目录内部，否则磁盘打满时会同时影响数据库运行和恢复能力。

常见目录示例：

```text
/pgdata/data        # PostgreSQL/TimescaleDB 数据目录
/pgdata/wal         # pg_wal 独立磁盘或独立 volume
/pgdata/wal_archive # WAL archive 独立磁盘、独立 volume 或归档挂载目录
```


## 读副本查询与 hot_standby_feedback

如果 UI Server 使用 PostgreSQL/TimescaleDB 读副本查询，长时间查询或连续聚合查询可能遇到下面的错误：

```text
canceling statement due to conflict with recovery
```

这是因为读副本需要持续回放主库 WAL。当读副本上的查询仍在读取旧版本数据，而主库的 VACUUM 已经清理了这些旧版本时，副本为了继续追上 WAL 回放，可能会取消正在执行的查询。

可以在读副本上开启：

```conf
hot_standby_feedback = on
```

开启后，读副本会把仍在使用的事务可见性信息反馈给主库，主库会尽量保留副本查询仍需要的旧版本数据，从而降低读副本查询被取消的概率。

注意：

- `hot_standby_feedback` 主要用于读副本查询冲突场景，不是提升 Parser 写入吞吐的参数。
- 开启后，主库可能因为保留更多旧版本行而产生表膨胀，特别是在读副本上存在长事务或长时间查询时更明显。
- 需要同时监控主库 autovacuum、表膨胀、磁盘空间和副本复制延迟。
- 如果读副本经常执行很长时间的查询，还应优化查询时间范围、索引、连续聚合刷新策略，避免只依赖 `hot_standby_feedback`。

查看当前设置：

```sql
SHOW hot_standby_feedback;
```

