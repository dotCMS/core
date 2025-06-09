# ClickHouse Optimization Plan for Higher Throughput

## Current Analysis

### ClickHouse Configuration Issues Found:
1. **Async Inserts Disabled**: `async_insert = 0` (Major bottleneck!)
2. **Synchronous Processing**: `wait_for_async_insert = 1` (Blocking inserts)
3. **Small Batch Sizes**: `max_insert_block_size = 1048449` rows (could be optimized)
4. **Large Table Schema**: 149+ columns (many unnecessary for analytics)

### Table Structure Issues:
- **ReplicatedMergeTree**: Good for reliability but adds replication overhead
- **Partition by customer_id**: Could cause small partitions
- **Many String columns**: No LowCardinality optimization
- **Wide schema**: 149+ columns including many filter variants

## Optimization Strategy

### Phase 1: Immediate ClickHouse Settings Changes

#### 1. Enable Async Inserts (Highest Impact)
```sql
-- Apply these settings globally
ALTER USER default SETTINGS 
    async_insert = 1,
    wait_for_async_insert = 0,
    async_insert_max_data_size = 10000000,      -- 10MB batches
    async_insert_busy_timeout_ms = 1000,        -- Flush after 1 second
    async_insert_stale_timeout_ms = 5000;       -- Force flush after 5 seconds
```

#### 2. Optimize Insert Block Sizes
```sql
ALTER USER default SETTINGS
    max_insert_block_size = 1000000,            -- 1M rows per block
    min_insert_block_size_bytes = 100000000,    -- 100MB min block size
    min_insert_block_size_rows = 500000;        -- 500K min rows
```

#### 3. Disable Insert Optimization Overhead
```sql
ALTER USER default SETTINGS
    insert_deduplicate = 0,                     -- Disable deduplication
    optimize_on_insert = 0;                     -- Disable optimization on insert
```

## Expected Results

### Before Optimization:
- **Throughput**: ~400 events/second
- **Processing Time**: 5+ seconds for 5K events
- **Bottleneck**: Synchronous ClickHouse inserts

### After Phase 1 (Async Inserts):
- **Expected Throughput**: 800-1200 events/second
- **Processing Time**: <1 second for 10K events
- **Improved**: Async processing, larger batches

The primary issue is that **async inserts are completely disabled**, forcing every insert to be synchronous and creating a massive bottleneck. Enabling async inserts alone should improve throughput by 3-5x immediately. 