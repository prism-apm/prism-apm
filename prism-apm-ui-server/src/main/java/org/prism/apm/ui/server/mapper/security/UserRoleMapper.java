package org.prism.apm.ui.server.mapper.security;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.prism.apm.ui.server.entity.response.admin.UserRoleRelationResponse;
import org.prism.apm.ui.server.entity.model.security.UserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
@DS("master")
public interface UserRoleMapper extends BaseMapper<UserRole> {
    
    @Select("SELECT role_id FROM role_user WHERE user_id = #{userId}")
    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);
    
    @Delete("DELETE FROM role_user WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
    
    @Delete("DELETE FROM role_user WHERE user_id = #{userId} AND role_id = #{roleId}")
    int deleteByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Select("select * FROM role_user WHERE user_id = #{userId} AND role_id = #{roleId}")
    UserRole selectByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Select("SELECT ru.id as relationId, ru.user_id as userId, u.username, ru.role_id as roleId, r.name as roleName, ru.created_at as createTime " +
            "FROM role_user ru " +
            "LEFT JOIN users u ON ru.user_id = u.id " +
            "LEFT JOIN roles r ON ru.role_id = r.id " +
            "ORDER BY ru.created_at DESC")
    Page<UserRoleRelationResponse> selectUserRoleRelations(Page<UserRoleRelationResponse> page);
}