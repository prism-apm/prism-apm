package org.prism.apm.parser.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author john
 * @date 2024-10-21 17:03
 */
@Configuration
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CacheConfig {

    /** Estimated byte size of a single spanCache entry (key + value). Used to derive max entry count from heap %. */
    @Value("${cache.entry_size_bytes:1000}")
    private int spanInfoCacheEntrySizeBytes;

    @Value("${cache.timeout_s:3600}")
    private int spanCacheTimeout;

    @Value("${cache.heap_percent:25}")
    private int cacheHeapPercent;  // percentage of JVM -Xmx to allocate for spanCache (1–100)

    @Value("${resource.hash.cache.size_entries:100000}")
    private int resourceHashCacheSizeEntries;

}
