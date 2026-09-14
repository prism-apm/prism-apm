package org.prism.apm.parser.model.entity;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity corresponding to 'resource_hash_dictionary' table.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResourceHashDictionaryEntity {

    /**
     * 128-bit Hash Key (UUID).
     */
    private UUID hashKey;

    /**
     * Category of the resource (e.g. 'target_name').
     */
    private String category;

    /**
     * Algorithm used (e.g. 'XXH3_128').
     */
    private String algorithm;

    /**
     * Original text value.
     */
    private String textValue;

    /**
     * Creation time.
     */
    private OffsetDateTime createTime;
}
