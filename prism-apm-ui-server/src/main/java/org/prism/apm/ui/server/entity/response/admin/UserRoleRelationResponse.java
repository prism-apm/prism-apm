package org.prism.apm.ui.server.entity.response.admin;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * User-role relation DTO
 */
@Data
public class UserRoleRelationResponse {
    
    /**
     * Relation ID
     */
    private Long relationId;
    
    /**
     * User ID
     */
    private Long userId;
    
    /**
     * Username
     */
    private String username;
    
    /**
     * Role ID
     */
    private Long roleId;
    
    /**
     * Role name
     */
    private String roleName;
    
    /**
     * Creation time
     */
    private OffsetDateTime createTime;
} 