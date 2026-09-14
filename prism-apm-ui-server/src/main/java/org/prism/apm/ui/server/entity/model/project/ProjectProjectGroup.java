package org.prism.apm.ui.server.entity.model.project;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Project-project-group relationship table (many-to-many)
 */
@Data
@TableName("project_project_group")
public class ProjectProjectGroup {

    /**
     * Primary key ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * ID referencing the project table
     */
    @TableField("project_id")
    private Long projectId;

    /**
     * ID referencing the project group table
     */
    @TableField("project_group_id")
    private Long projectGroupId;

    /**
     * Record creation time
     */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    /**
     * Record last update time
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
} 