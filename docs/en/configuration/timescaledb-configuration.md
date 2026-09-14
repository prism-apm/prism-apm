---
created: 2026-05-28 14:16:22
modified: 2026-08-10 17:21:13
---

# Extensions

This project uses TimescaleDB as its database. It uses these extensions:

- `timescaledb`
- `timescaledb_toolkit`: uses `rollup` and `percentile_agg` to calculate percentiles.
- `pg_prewarm`: the first query can become slower after data has not been accessed for a while. This extension can periodically prewarm core data.

# Recommended TimescaleDB Configuration

Use this command to view TimescaleDB's recommended configuration:

```shell
timescaledb-tune --quiet --yes
```

# Configuration Example

The following example is for a 16-core, 32 GB RAM machine:

```conf
# ==========================================
# TimescaleDB & PostgreSQL optimized configuration
# Hardware: 16 CPU, 32GB RAM, SSD
# ==========================================

# --- 1. Basic preload ---
shared_preload_libraries = 'timescaledb'  # required

# --- 2. Memory ---
# Keep the 25% memory calculated by tune for shared buffers.
shared_buffers = 8022MB

# Tell the optimizer how much OS cache is available (75% RAM).
effective_cache_size = 24067MB

# Increase work memory.
work_mem = 128MB

# Maintenance memory for VACUUM and index creation.
maintenance_work_mem = 2047MB

# --- 3. Write performance and WAL ---
# Disable synchronous commit (from on to off).
# Trades a very small amount of data safety (less than 1 second of data can be lost on crash)
# for a 2-3x write-performance improvement.
synchronous_commit = off

# Increase WAL size limit (from 1GB to 16GB).
# Reduces checkpoint frequency and greatly reduces I/O jitter.
max_wal_size = 16GB
min_wal_size = 4GB

# Keep checkpoint writes smooth.
checkpoint_completion_target = 0.9
wal_buffers = 16MB

# --- 4. Parallelism and workers ---
# Keep tune result: CPU(16) + Timescale background(16) + reserve ≈ 35.
max_worker_processes = 35

# Maximum processes for parallel queries, limited to CPU cores.
max_parallel_workers = 16

# Maximum parallelism for one query, limited to half of CPU cores.
max_parallel_workers_per_gather = 8

# Aggressive parallel-query cost model.
# Lower the parallel setup cost to strongly encourage parallel plans.
parallel_setup_cost = 10
# Lower tuple transfer cost to reduce the computed penalty of inter-process communication.
parallel_tuple_cost = 0.001
# Lower the table-size threshold for parallel scans so smaller tables/chunks can also be scanned in parallel.
min_parallel_table_scan_size = 1MB

# TimescaleDB background workers, used for compression, continuous aggregates, and similar jobs.
timescaledb.max_background_workers = 16

# --- 5. Storage and I/O optimization (SSD) ---
# Tell the optimizer that random reads are fast (SSD behavior).
random_page_cost = 1.1

# Allow highly concurrent I/O requests.
effective_io_concurrency = 256

# Long-text compression algorithm, if PostgreSQL version >= 14.
default_toast_compression = lz4

# --- 6. Auto-maintenance and locks ---
# Increase autovacuum worker count.
autovacuum_max_workers = 10
autovacuum_naptime = 10

# Important: adapt to TimescaleDB's lock requirements for many child tables.
max_locks_per_transaction = 512

# --- 7. Misc ---
# Disable JIT to improve response speed under mixed workloads.
jit = off
default_statistics_target = 100
max_connections = 100

# checkpoint_timeout controls how often PostgreSQL automatic WAL checkpoints happen.
# It defines the maximum time the system can wait before forcing a checkpoint.
checkpoint_timeout = 30min

# ----------------------------------------
# Background Writer optimization
# Goal: make the background writer flush dirty pages more actively and avoid blocking aggregate jobs.
# ----------------------------------------

# Scan frequency: shorten from the default 200ms to 10ms.
bgwriter_delay = 10ms

# Work per round: increase from the default 100 pages to 1000 pages, about 8MB.
bgwriter_lru_maxpages = 1000

# Prediction aggressiveness: increase from the default 2.0 to 4.0.
bgwriter_lru_multiplier = 4.0

# Enable this to address standby errors: "canceling statement due to conflict with recovery".
hot_standby_feedback = on

# Show I/O execution time in plans.
track_io_timing = on

# ==========================================
# --- 8. Query planning and cache ---
# ==========================================

# Force custom execution plans.
# Solves the issue where prepared statements prevent TimescaleDB chunk exclusion.
plan_cache_mode = 'force_custom_plan'
```

Check whether settings have taken effect:

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

# Optimization Configuration

The following configuration is mainly for production environments. Before tuning, first confirm the current write volume, query concurrency, disk type, whether read replicas exist, and whether Parser write latency is already limited by the database.

## Storage Separation

After installing TimescaleDB, try to place the following data on different disks or different high-performance storage volumes:

- Data directory: PostgreSQL/TimescaleDB main data directory, including tables, indexes, chunks, continuous aggregate materialized data, and similar files.
- WAL logs: PostgreSQL write-ahead logs, which receive continuous sequential writes during write peaks.
- WAL archive: archived WAL used for backup, recovery, or primary/standby replication.

Recommendations:

- Use larger SSD/NVMe storage for the data directory.
- Use low-latency, high-write-performance SSD/NVMe storage for WAL logs.
- Use a larger independent disk or object-storage-mounted directory for WAL archive, to avoid archive writes competing with the main data disk I/O.
- If using Docker or Kubernetes, configure independent volumes for the data directory and WAL directory.
- Do not place WAL archive inside the main data directory; otherwise a full disk can affect both database operation and recovery capability.

Common directory example:

```text
/pgdata/data        # PostgreSQL/TimescaleDB data directory
/pgdata/wal         # pg_wal independent disk or independent volume
/pgdata/wal_archive # WAL archive independent disk, independent volume, or archive mount directory
```

## Read-Replica Queries and `hot_standby_feedback`

If UI Server queries a PostgreSQL/TimescaleDB read replica, long-running queries or continuous aggregate queries may encounter this error:

```text
canceling statement due to conflict with recovery
```

This happens because the read replica must continuously replay WAL from the primary. When a query on the replica is still reading old row versions, but VACUUM on the primary has already cleaned those versions, the replica may cancel the running query so it can keep up with WAL replay.

You can enable this on the read replica:

```conf
hot_standby_feedback = on
```

After enabling it, the read replica sends transaction visibility information that is still in use back to the primary. The primary then tries to preserve old row versions still needed by replica queries, reducing the chance that replica queries are canceled.

Notes:

- `hot_standby_feedback` is mainly for read-replica query conflicts. It is not a parameter for improving Parser write throughput.
- After enabling it, the primary may keep more old row versions and experience table bloat, especially when the read replica has long transactions or long-running queries.
- Monitor primary autovacuum, table bloat, disk space, and replication lag at the same time.
- If the read replica often runs long queries, also optimize query time ranges, indexes, and continuous aggregate refresh policies instead of relying only on `hot_standby_feedback`.

Check the current setting:

```sql
SHOW hot_standby_feedback;
```
