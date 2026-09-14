package org.prism.apm.ui.server.entity.model.project;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * User-project-group relationship table (many-to-many)
 */
@Data
@TableName("user_project_group")
public class UserProjectGroup {

    /**
     * Primary key ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * ID referencing the user table
     */
    @TableField("user_id")
    private Long userId;

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