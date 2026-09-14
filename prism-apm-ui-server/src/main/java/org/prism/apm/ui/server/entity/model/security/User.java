package org.prism.apm.ui.server.entity.model.security;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
@TableName("users")
@EqualsAndHashCode(callSuper = false)
public class User {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;

    // 允许请求写入、禁止响应输出（哈希不出现在接口返回中）
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private Boolean enabled;
    
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
} 