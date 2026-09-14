package org.prism.apm.ui.server.entity.dto.security;

import lombok.Data;

@Data
public class PermissionWithRoleDTO {

    /**
     * URL pattern corresponding to the permission
     */
    private String url;

    /**
     * Role names that have this permission
     */
    private String roleName;

}
