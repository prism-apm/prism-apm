package org.prism.apm.ui.server.entity.request.admin;

import lombok.Data;

/**
 * Role-permission assignment request parameters
 */
@Data
public class AssignRolePermissionRequest {

    /**
     * Role ID
     */
    private Long roleId;

    /**
     * Permission ID
     */
    private Long permissionId;
} 