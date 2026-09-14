package org.prism.apm.ui.server.entity.response.admin;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Project group-project relation DTO
 */
@Data
public class ProjectGroupProjectRelationResponse {
    
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
     * Project ID
     */
    private Long projectId;
    
    /**
     * Project name
     */
    private String projectName;
    
    /**
     * Project display name
     */
    private String projectDisplay;
    
    /**
     * Creation time
     */
    private OffsetDateTime createTime;
} 