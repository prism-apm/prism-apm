package org.prism.apm.ui.server.entity.request.admin;

import lombok.Data;

/**
 * Remove relation request parameters
 */
@Data
public class RemoveRelationRequest {

    /**
     * Relation record ID
     */
    private Long relationId;
} 