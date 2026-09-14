package org.prism.apm.ui.server.entity.model.security;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Data
@TableName("roles")
@EqualsAndHashCode(callSuper = false)
public class Role {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
} 