package org.prism.apm.parser.cache;

import org.prism.apm.parser.config.EhcacheConfig;
import lombok.NoArgsConstructor;
import org.ehcache.Cache;
import org.ehcache.core.spi.service.StatisticsService;
import org.ehcache.core.statistics.CacheStatistics;
import org.ehcache.core.statistics.TierStatistics;
import org.ehcache.sizeof.SizeOf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import static org.prism.apm.parser.config.EhcacheConfig.CACHE_NAME;
import static org.prism.apm.parser.constants.Constants.CACHE_LOCAL;
import java.util.UUID;

/**
 * @author john
 * @date 2024-10-21 16:27
 */

@Service
@NoArgsConstructor
@ConditionalOnProperty(name = "span.cache.type", havingValue = CACHE_LOCAL, matchIfMissing = true)
public class EhcacheService implements CacheService {

    @Autowired
    private EhcacheConfig ehcacheConfig;

    private SizeOf sizeOf = SizeOf.newInstance();

    public EhcacheService(EhcacheConfig ehcacheConfig) {
        this.ehcacheConfig = ehcacheConfig;
    }

    @Override
    public void set(String key, String value) {
        Cache<String, String> cache = ehcacheConfig.getManager().getCache(
                CACHE_NAME, String.class, String.class);
        cache.put(key, value);
    }

    @Override
    public String get(String key) {
        if (key == null) {
            return null;
        }

        Cache<String, String> cache = ehcacheConfig.getManager().getCache(
                CACHE_NAME, String.class, String.class);
        return cache.get(key);
    }

    @Override
    public void clear() {
        ehcacheConfig.getManager().getCache(
                CACHE_NAME, String.class, String.class).clear();
    }

    // ========== Resource Hash Cache Operations ==========

    @Override
    public UUID getResourceHash(String resourceName) {
        if (resourceName == null) {
            return null;
        }
        Cache<String, UUID> cache = ehcacheConfig.getManager().getCache(
                EhcacheConfig.RESOURCE_HASH_CACHE_NAME, String.class, UUID.class);
        return cache.get(resourceName);
    }

    @Override
    public void setResourceHash(String resourceName, UUID hashUuid) {
        Cache<String, UUID> cache = ehcacheConfig.getManager().getCache(
                EhcacheConfig.RESOURCE_HASH_CACHE_NAME, String.class, UUID.class);
        cache.put(resourceName, hashUuid);
    }

    @Override
    public void clearResourceHashCache() {
        ehcacheConfig.getManager().getCache(
                EhcacheConfig.RESOURCE_HASH_CACHE_NAME, String.class, UUID.class).clear();
    }

    public String getStatistics() {

        StatisticsService statisticsService = ehcacheConfig.getStatisticsService();

        CacheStatistics cacheStatics = statisticsService.getCacheStatistics(CACHE_NAME);
        TierStatistics tierStatics = cacheStatics.getTierStatistics().get("OnHeap");
        Cache<String, String> cache = ehcacheConfig.getManager().getCache(
                CACHE_NAME, String.class, String.class);

        StringBuilder sb = new StringBuilder();
        sb.append("ehcache stats. ");
        sb.append("exists:" + tierStatics.getMappings() + ", ");
        sb.append("puts:" + cacheStatics.getCachePuts() + ", ");
        sb.append("gets:" + cacheStatics.getCacheGets() + ", ");
        sb.append("removals:" + cacheStatics.getCacheRemovals() + ", ");
        sb.append("hits:" + cacheStatics.getCacheHits() + ", ");
        sb.append("misses:" + cacheStatics.getCacheMisses() + ", ");
        sb.append("Evictions:" + cacheStatics.getCacheEvictions() + ", ");
        sb.append("Expirations:" + cacheStatics.getCacheExpirations() + ", ");
        // unit: byte
        sb.append("OccupiedByteSize:" + tierStatics.getOccupiedByteSize() + ", ");
        // unit: byte
        // very cost time, don't use it.
        // sb.append("SizeOf.newInstance().deepSizeOf:" + sizeOf.deepSizeOf(cache));

        return sb.toString();

    }
}
