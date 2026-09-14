package org.prism.apm.ui.server.mapper.security;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.prism.apm.ui.server.entity.dto.security.PermissionWithRoleDTO;
import org.prism.apm.ui.server.entity.model.security.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
@DS("master")
public interface PermissionMapper extends BaseMapper<Permission> {
    
    @Select("SELECT * FROM permissions WHERE name = #{name}")
    Permission findByName(@Param("name") String name);
    
    @Select("SELECT * FROM permissions WHERE url = #{url}")
    List<Permission> findByUrl(@Param("url") String url);

    /**
     * Query all permissions and their corresponding role information
     * This method maps to a custom SQL query in the XML file
     * @return DTO list containing URL and role name
     */
    List<PermissionWithRoleDTO> getPermissionWithRoles();
} 