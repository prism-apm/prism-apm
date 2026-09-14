package org.prism.apm.parser.repository;

import org.prism.apm.parser.model.entity.ResourceHashDictionaryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Mapper
@Repository
public interface ResourceHashDictionaryMapper {

    /**
     * Batch upsert mappings.
     * On conflict (hash_key, category), do nothing (or update if needed, usually
     * ignore for dictionary).
     *
     * @param entities list of entities
     * @return rows affected
     */
    int batchUpsert(List<ResourceHashDictionaryEntity> entities);

    /**
     * Select by hash key and category.
     *
     * @param hashKey  the UUID hash key
     * @param category the category (e.g. 'target_name')
     * @return the entity if found, null otherwise
     */
    ResourceHashDictionaryEntity selectByHashKeyAndCategory(UUID hashKey, String category);
}
