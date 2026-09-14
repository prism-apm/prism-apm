package org.prism.apm.ui.server.entity.response.admin;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Project group-user relation DTO
 */
@Data
public class ProjectGroupUserRelationResponse {
    
    /**
     * Relation ID
     */
    private Long relationId;
    
    /**
     * Project group ID
     */
    private Long projectGroupId;
    
    /**
     * Project group name
     */
    private String groupName;
    
    /**
     * User ID
     */
    private Long userId;
    
    /**
     * Username
     */
    private String username;
    
    /**
     * Creation time
     */
    private OffsetDateTime createTime;
} 