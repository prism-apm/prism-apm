package org.prism.apm.parser.service.save;

import org.prism.apm.parser.cache.CacheService;
import org.prism.apm.parser.model.entity.ResourceHashDictionaryEntity;
import org.prism.apm.parser.model.entity.ServiceCallEntity;
import org.prism.apm.parser.utils.HashUtils;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.prism.apm.parser.utils.CommonUtil.getCurrentTimeOffsetDateTime;

/**
 * Resolver from resource names to hash UUIDs.
 * Converts string fields in entities, such as targetName, to UUID hashes,
 * and uses cache to avoid repeated computation.
 *
 * @author john
 * @date 2026-01-27
 */
@Service
@Data
public class ResourceHashResolver {

    /**
     * Default UUID used when callerTargetName is empty
     */
    public static final UUID DEFAULT_UUID = new UUID(0L, 0L);

    private static final String CATEGORY_TARGET_NAME = "target_name";
    private static final String ALGORITHM_XXH3_128 = "XXH3_128";

    @Resource
    private CacheService cacheService;

    /**
     * Resolve resource-name fields in the entity list, convert strings to UUID hashes, and set the corresponding ID fields.
     * <p>
     * Processing logic:
     * 1. First look up existing hashes from cache
     * 2. When the cache misses, compute the hash and set it on the entity field
     * 3. Return ResourceHashDictionaryEntity entries that need to be persisted; the caller persists them and updates the cache
     *
     * @param entities List of ServiceCallEntity entries to process
     * @return ResourceHashDictionaryEntity entries that need to be persisted (not found in cache)
     */
    public List<ResourceHashDictionaryEntity> resolveHashes(List<ServiceCallEntity> entities) {
        // Used for deduplication: the same name in one batch returns only one entity
        Map<String, UUID> newHashesMap = new HashMap<>();

        for (ServiceCallEntity entity : entities) {
            // Handle targetName
            if (StringUtils.hasText(entity.getTargetName())) {
                String name = entity.getTargetName();
                UUID hash = resolveHash(name, newHashesMap);
                if (hash != null) {
                    entity.setTargetNameId(hash);
                }
            }

            // Handle callerTargetName
            if (StringUtils.hasText(entity.getCallerTargetName())) {
                String name = entity.getCallerTargetName();
                UUID hash = resolveHash(name, newHashesMap);
                if (hash != null) {
                    entity.setCallerTargetNameId(hash);
                }
            } else {
                // Set the default value when callerTargetName is empty
                // TargetName is not handled similarly because target is required; if it is empty, expose the problem when inserting into the database.
                entity.setCallerTargetNameId(DEFAULT_UUID);
            }
        }

        // Convert Map to List<ResourceHashDictionaryEntity>
        List<ResourceHashDictionaryEntity> result = new ArrayList<>();
        for (Map.Entry<String, UUID> entry : newHashesMap.entrySet()) {
            result.add(ResourceHashDictionaryEntity.builder()
                    .textValue(entry.getKey())
                    .hashKey(entry.getValue())
                    .category(CATEGORY_TARGET_NAME)
                    .algorithm(ALGORITHM_XXH3_128)
                    .createTime(getCurrentTimeOffsetDateTime())
                    .build());
        }

        return result;
    }

    /**
     * Resolve the hash for a single resource name.
     * Read from cache first; if the cache misses, compute it and record it in newHashes.
     *
     * @param name      Resource name
     * @param newHashes Used to collect newly computed hash mappings
     * @return hash UUID, or null if computation fails
     */
    private UUID resolveHash(String name, Map<String, UUID> newHashes) {
        // 1. Try to read from cache
        UUID cachedHash = cacheService.getResourceHash(name);
        if (cachedHash != null) {
            return cachedHash;
        }

        // 2. Check whether this batch has already computed it
        UUID existingHash = newHashes.get(name);
        if (existingHash != null) {
            return existingHash;
        }

        // 3. Compute a new hash
        UUID hashKey = HashUtils.hash128(name);
        if (hashKey != null) {
            newHashes.put(name, hashKey);
        }
        return hashKey;
    }
}
