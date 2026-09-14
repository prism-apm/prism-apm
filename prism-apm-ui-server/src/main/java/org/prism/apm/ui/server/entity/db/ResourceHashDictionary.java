package org.prism.apm.ui.server.entity.db;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Resource hash dictionary entity
 * Corresponds to database table: resource_hash_dictionary
 * Stores mappings between HashValue and original text
 */
@Data
public class ResourceHashDictionary {

    /**
     * 128-bit Hash (XXH3), stored as a UUID-format string
     */
    private String hashKey;

    /**
     * Business category (e.g. 'target_name')
     */
    private String category;

    /**
     * Algorithm identifier (e.g. 'XXH3_128')
     */
    private String algorithm;

    /**
     * Original text
     */
    private String textValue;

    /**
     * Creation time (TIMESTAMPTZ)
     */
    private OffsetDateTime createTime;
}
