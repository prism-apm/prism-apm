package org.prism.apm.ui.server.entity.model.security;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
@TableName("permissions")
@EqualsAndHashCode(callSuper = false)
public class Permission {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private String url;
    
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
} 