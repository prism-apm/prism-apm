package org.prism.apm.ui.server.mapper.service;

import org.prism.apm.ui.server.entity.db.ResourceHashDictionary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Resource hash dictionary Mapper
 * Used to query Hash -> original text mappings
 */
@Mapper
public interface ResourceHashDictionaryMapper {

    /**
     * Batch query resource dictionary
     *
     * @param hashKeys HashKey list (UUID-format string)
     * @param category Business category (e.g. 'target_name')
     * @return Dictionary list
     */
    List<ResourceHashDictionary> batchQuery(@Param("hashKeys") List<String> hashKeys,
                                             @Param("category") String category);
}
