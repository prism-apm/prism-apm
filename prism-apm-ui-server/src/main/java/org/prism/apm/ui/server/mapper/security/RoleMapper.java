package org.prism.apm.ui.server.mapper.security;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.prism.apm.ui.server.entity.model.security.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
@DS("master")
public interface RoleMapper extends BaseMapper<Role> {
    
    @Select("SELECT * FROM roles WHERE name = #{name}")
    Role findByName(@Param("name") String name);
} 