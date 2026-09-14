package org.prism.apm.ui.server.entity.response.admin;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Role-permission relation DTO
 */
@Data
public class RolePermissionRelationResponse {
    
    /**
     * Relation ID
     */
    private Long relationId;
    
    /**
     * Role ID
     */
    private Long roleId;
    
    /**
     * Role name
     */
    private String roleName;
    
    /**
     * Permission ID
     */
    private Long permissionId;
    
    /**
     * Permission name
     */
    private String permissionName;
    
    /**
     * Permission URL
     */
    private String url;
    
    /**
     * Creation time
     */
    private OffsetDateTime createTime;
} 