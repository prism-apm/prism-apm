package org.prism.apm.parser.config;

import org.prism.apm.parser.cache.listener.CacheEventLogger;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheEventListenerConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.core.internal.statistics.DefaultStatisticsService;
import org.ehcache.core.spi.service.StatisticsService;
import org.ehcache.event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.UUID;

import static org.prism.apm.parser.constants.Constants.CACHE_LOCAL;

/**
 * @author john
 * @date 2024-10-21 16:28
 */
@Data
@NoArgsConstructor
@Configuration
@ConditionalOnProperty(name = "span.cache.type", havingValue = CACHE_LOCAL, matchIfMissing = true)
public class EhcacheConfig {

    private static final Logger log = LoggerFactory.getLogger(EhcacheConfig.class);

    public final static String CACHE_NAME = "spanCache";
    public final static String RESOURCE_HASH_CACHE_NAME = "resource_hash_cache";

    @Autowired
    private CacheConfig cacheConfig;

    private CacheManager manager;

    private StatisticsService statisticsService;

    public EhcacheConfig(CacheConfig cacheConfig) {
        this.cacheConfig = cacheConfig;
    }

    @PostConstruct
    public void init() {
        CacheEventListenerConfigurationBuilder listenerConfig = CacheEventListenerConfigurationBuilder
                .newEventListenerConfiguration(new CacheEventLogger(), EventType.EXPIRED,
                        EventType.EVICTED)
                .ordered()
                .asynchronous();

        long maxEntries = computeMaxEntries(
                cacheConfig.getCacheHeapPercent(),
                cacheConfig.getSpanInfoCacheEntrySizeBytes());

        CacheConfiguration<String, String> config = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                        String.class, String.class,
                        ResourcePoolsBuilder.newResourcePoolsBuilder().heap(maxEntries, EntryUnit.ENTRIES))
                .withExpiry(
                        ExpiryPolicyBuilder.timeToLiveExpiration(
                                Duration.ofSeconds(cacheConfig.getSpanCacheTimeout())))
                .withService(listenerConfig)
                .build();

        // Define resource_hash_cache configuration using heap entries instead of MB, with UUID as the value type
        CacheConfiguration<String, UUID> resourceHashConfig = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(
                        String.class, UUID.class,
                        ResourcePoolsBuilder.newResourcePoolsBuilder().heap(
                                cacheConfig.getResourceHashCacheSizeEntries(),
                                EntryUnit.ENTRIES))
                .build();

        StatisticsService statisticsService = new DefaultStatisticsService();

        CacheManager manager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache(
                        CACHE_NAME,
                        config)
                .withCache(
                        RESOURCE_HASH_CACHE_NAME,
                        resourceHashConfig)
                .using(statisticsService)
                .build();
        manager.init();

        this.manager = manager;
        this.statisticsService = statisticsService;

        logCacheAllocation(maxEntries,
                cacheConfig.getSpanInfoCacheEntrySizeBytes(),
                cacheConfig.getResourceHashCacheSizeEntries());
    }

    /**
     * Logs a human-readable summary of each cache's allocation immediately after
     * the {@link CacheManager} is initialized.
     *
     * <p>Because both caches use {@link EntryUnit#ENTRIES}, Ehcache does not
     * enforce a hard byte limit — the "estimated heap" figures are approximate:
     * {@code maxEntries × entrySizeBytes}.</p>
     */
    private void logCacheAllocation(long spanCacheMaxEntries,
                                    int spanCacheEntrySizeBytes,
                                    long resourceHashCacheMaxEntries) {
        long jvmMaxMb    = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        long spanEstMb   = spanCacheMaxEntries * spanCacheEntrySizeBytes / (1024 * 1024);
        // UUID value = 16 B; key length varies — log as a lower-bound estimate
        int  uuidBytes   = 16;
        long hashEstMb   = resourceHashCacheMaxEntries * uuidBytes / (1024 * 1024);

        log.info("Ehcache initialized (JVM -Xmx: {} MB)", jvmMaxMb);
        log.info(String.format("  [%-22s]  max entries: %,12d  |  entry size: %6d B  |  estimated heap: ~%d MB  (heap_percent: %d%%)",
                CACHE_NAME, spanCacheMaxEntries, spanCacheEntrySizeBytes, spanEstMb, cacheConfig.getCacheHeapPercent()));
        log.info(String.format("  [%-22s]  max entries: %,12d  |  entry size: %6d B  |  estimated heap: ~%d MB  (fixed entry count)",
                RESOURCE_HASH_CACHE_NAME, resourceHashCacheMaxEntries, uuidBytes, hashEstMb));
    }

    /**
     * Computes the maximum number of cache entries for the spanCache based on a
     * percentage of the JVM's maximum heap ({@code -Xmx}) and a fixed per-entry byte size.
     *
     *
     * @param heapPercent    percentage of {@link Runtime#maxMemory()} to use (1–100)
     * @param entrySizeBytes estimated byte size of a single cache entry
     * @return the clamped maximum entry count
     */
    private long computeMaxEntries(int heapPercent, int entrySizeBytes) {
        long maxMemoryBytes = Runtime.getRuntime().maxMemory();
        long usableBytes = (long) (maxMemoryBytes * (heapPercent / 100.0));
        long entries = usableBytes / entrySizeBytes;
        return entries;
    }

    // @Bean
    // public CacheManager cacheManager() {
    //
    // CacheConfiguration<String, String> config =
    // CacheConfigurationBuilder.newCacheConfigurationBuilder(
    // String.class, String.class,
    // ResourcePoolsBuilder.newResourcePoolsBuilder().
    // heap(1000, MemoryUnit.MB)
    // )
    // .withExpiry(
    // ExpiryPolicyBuilder.timeToLiveExpiration(
    // Duration.ofSeconds(cacheConfig.getSpanCacheTimeout())))
    // .build();
    //
    //
    // StatisticsService statisticsService = new DefaultStatisticsService();
    //
    //
    //
    // CacheManager manager = CacheManagerBuilder.newCacheManagerBuilder()
    // .withCache(
    // CACHE_NAME,
    // config
    // )
    // .using(statisticsService)
    // .build();
    // manager.init();
    //
    //
    // this.statisticsService = statisticsService;
    //
    //
    // return manager;
    // }
}
