package org.prism.apm.parser.cache;

import java.util.UUID;

/**
 * Cache service interface for all caching operations.
 * 
 * @author john
 * @date 2024-05-09 18:24
 */
public interface CacheService {



    // ========== Span Cache Operations ==========

    void set(String key, String value);

    String get(String key);

    void clear();

    // ========== Resource Hash Cache Operations ==========

    /**
     * Get the cached hash UUID for a resource name.
     *
     * @param resourceName the resource name (e.g., target name string)
     * @return the cached hash UUID, or null if not cached
     */
    UUID getResourceHash(String resourceName);

    /**
     * Cache a resource name to hash UUID mapping.
     *
     * @param resourceName the resource name (e.g., target name string)
     * @param hashUuid     the hash UUID
     */
    void setResourceHash(String resourceName, UUID hashUuid);

    /**
     * Clear all entries from the resource hash cache.
     */
    void clearResourceHashCache();
}
