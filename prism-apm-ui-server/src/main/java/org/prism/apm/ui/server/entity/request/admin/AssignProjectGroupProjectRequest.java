package org.prism.apm.ui.server.entity.request.admin;

import lombok.Data;

/**
 * Project group-project assignment request parameters
 */
@Data
public class AssignProjectGroupProjectRequest {

    /**
     * Project group ID
     */
    private Long projectGroupId;

    /**
     * Project ID
     */
    private Long projectId;
} 