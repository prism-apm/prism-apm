package org.prism.apm.ui.server.entity.request.admin;

import lombok.Data;

/**
 * User-role assignment request parameters
 */
@Data
public class AssignRoleUserRequest {

    /**
     * Role ID
     */
    private Long roleId;

    /**
     * User ID
     */
    private Long userId;
} 