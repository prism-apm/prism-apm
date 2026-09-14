package org.prism.apm.ui.server.mapper.security;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.prism.apm.ui.server.entity.response.admin.RolePermissionRelationResponse;
import org.prism.apm.ui.server.entity.model.security.RolePermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
@DS("master")
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
    
    @Select("SELECT permission_id FROM role_permission WHERE role_id = #{roleId}")
    List<Long> findPermissionIdsByRoleId(@Param("roleId") Long roleId);
    
    @Delete("DELETE FROM role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);
    
    @Delete("DELETE FROM role_permission WHERE role_id = #{roleId} AND permission_id = #{permissionId}")
    int deleteByRoleIdAndPermissionId(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    @Select("select * FROM role_permission WHERE role_id = #{roleId} AND permission_id = #{permissionId}")
    RolePermission selectByRoleIdAndPermissionId(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    @Select("SELECT rp.id as relationId, rp.role_id as roleId, r.name as roleName, rp.permission_id as permissionId, p.name as permissionName, p.url, rp.created_at as createTime " +
            "FROM role_permission rp " +
            "LEFT JOIN roles r ON rp.role_id = r.id " +
            "LEFT JOIN permissions p ON rp.permission_id = p.id " +
            "ORDER BY rp.created_at DESC")
    Page<RolePermissionRelationResponse> selectRolePermissionRelations(Page<RolePermissionRelationResponse> page);
}