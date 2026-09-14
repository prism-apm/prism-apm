package org.prism.apm.ui.server.entity.request.admin;

import lombok.Data;

/**
 * Project group-user assignment request parameters
 */
@Data
public class AssignProjectGroupUserRequest {

    /**
     * Project group ID
     */
    private Long projectGroupId;

    /**
     * User ID
     */
    private Long userId;
} 