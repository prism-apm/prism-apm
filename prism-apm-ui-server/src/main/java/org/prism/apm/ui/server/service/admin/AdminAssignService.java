package org.prism.apm.ui.server.service.admin;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.prism.apm.ui.server.entity.model.project.ProjectProjectGroup;
import org.prism.apm.ui.server.entity.model.project.UserProjectGroup;
import org.prism.apm.ui.server.entity.model.security.RolePermission;
import org.prism.apm.ui.server.entity.model.security.UserRole;
import org.prism.apm.ui.server.entity.param.common.PageParam;
import org.prism.apm.ui.server.entity.response.admin.ProjectGroupProjectRelationResponse;
import org.prism.apm.ui.server.entity.response.admin.ProjectGroupUserRelationResponse;
import org.prism.apm.ui.server.entity.response.admin.RolePermissionRelationResponse;
import org.prism.apm.ui.server.entity.response.admin.UserRoleRelationResponse;
import org.prism.apm.ui.server.mapper.project.ProjectGroupMapper;
import org.prism.apm.ui.server.mapper.project.ProjectMapper;
import org.prism.apm.ui.server.mapper.project.ProjectProjectGroupMapper;
import org.prism.apm.ui.server.mapper.project.UserProjectGroupMapper;
import org.prism.apm.ui.server.mapper.security.PermissionMapper;
import org.prism.apm.ui.server.mapper.security.RoleMapper;
import org.prism.apm.ui.server.mapper.security.RolePermissionMapper;
import org.prism.apm.ui.server.mapper.security.UserMapper;
import org.prism.apm.ui.server.mapper.security.UserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin permission assignment service
 */
@Service
@DS("master")
public class AdminAssignService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final ProjectGroupMapper projectGroupMapper;
    private final ProjectMapper projectMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final ProjectProjectGroupMapper projectProjectGroupMapper;
    private final UserProjectGroupMapper userProjectGroupMapper;

    public AdminAssignService(UserMapper userMapper, RoleMapper roleMapper, PermissionMapper permissionMapper, ProjectGroupMapper projectGroupMapper, ProjectMapper projectMapper, UserRoleMapper userRoleMapper, RolePermissionMapper rolePermissionMapper, ProjectProjectGroupMapper projectProjectGroupMapper, UserProjectGroupMapper userProjectGroupMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.projectGroupMapper = projectGroupMapper;
        this.projectMapper = projectMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.projectProjectGroupMapper = projectProjectGroupMapper;
        this.userProjectGroupMapper = userProjectGroupMapper;
    }

    // ==================== User-role assignment ====================

    /**
     * Assign role to user
     */
    @Transactional
    public boolean assignRoleToUser(Long roleId, Long userId) {
        if (userMapper.selectById(userId) == null) {
            return false;
        }
        if (roleMapper.selectById(roleId) == null) {
            return false;
        }
        if (userRoleMapper.selectByUserIdAndRoleId(userId, roleId) != null) {
            return false;
        }
        UserRole userRole = new UserRole();
        userRole.setRoleId(roleId);
        userRole.setUserId(userId);
        return userRoleMapper.insert(userRole) > 0;
    }

    /**
     * Remove user-role relation
     */
    @Transactional
    public boolean removeUserRole(Long relationId) {
        if (userRoleMapper.selectById(relationId) == null) {
            return false;
        }
        return userRoleMapper.deleteById(relationId) > 0;
    }

    /**
     * List user-role assignment relations
     */
    public Page<UserRoleRelationResponse> listUserRoleRelations(PageParam pageParam) {
        Page<UserRoleRelationResponse> page = new Page<>(pageParam.getPageNum(), pageParam.getPageSize());
        return userRoleMapper.selectUserRoleRelations(page);
    }

    // ==================== Role-permission assignment ====================

    /**
     * Assign permission to role
     */
    @Transactional
    public boolean assignPermissionToRole(Long roleId, Long permissionId) {
        if (roleMapper.selectById(roleId) == null) {
            return false;
        }
        if (permissionMapper.selectById(permissionId) == null) {
            return false;
        }
        if (rolePermissionMapper.selectByRoleIdAndPermissionId(roleId, permissionId) != null) {
            return false;
        }
        RolePermission rolePermission = new RolePermission();
        rolePermission.setRoleId(roleId);
        rolePermission.setPermissionId(permissionId);
        return rolePermissionMapper.insert(rolePermission) > 0;
    }

    /**
     * Remove role-permission relation
     */
    @Transactional
    public boolean removeRolePermission(Long relationId) {
        if (rolePermissionMapper.selectById(relationId) == null) {
            return false;
        }
        return rolePermissionMapper.deleteById(relationId) > 0;
    }

    /**
     * List role-permission assignment relations
     */
    public Page<RolePermissionRelationResponse> listRolePermissionRelations(PageParam pageParam) {
        Page<RolePermissionRelationResponse> page = new Page<>(pageParam.getPageNum(), pageParam.getPageSize());
        return rolePermissionMapper.selectRolePermissionRelations(page);
    }

    // ==================== Project group-project assignment ====================

    /**
     * Assign project to project group
     */
    @Transactional
    public boolean assignProjectToProjectGroup(Long projectGroupId, Long projectId) {
        if (projectGroupMapper.selectById(projectGroupId) == null) {
            return false;
        }
        if (projectMapper.selectById(projectId) == null) {
            return false;
        }
        if (projectProjectGroupMapper.selectByProjectIdAndProjectGroupId(projectId, projectGroupId) != null) {
            return false;
        }
        ProjectProjectGroup projectProjectGroup = new ProjectProjectGroup();
        projectProjectGroup.setProjectGroupId(projectGroupId);
        projectProjectGroup.setProjectId(projectId);
        return projectProjectGroupMapper.insert(projectProjectGroup) > 0;
    }

    /**
     * Remove project group-project relation
     */
    @Transactional
    public boolean removeProjectGroupProject(Long relationId) {
        if (projectProjectGroupMapper.selectById(relationId) == null) {
            return false;
        }
        return projectProjectGroupMapper.deleteById(relationId) > 0;
    }

    /**
     * List project group-project assignment relations
     */
    public Page<ProjectGroupProjectRelationResponse> listProjectGroupProjectRelations(PageParam pageParam) {
        Page<ProjectGroupProjectRelationResponse> page = new Page<>(pageParam.getPageNum(), pageParam.getPageSize());
        return projectProjectGroupMapper.selectProjectGroupProjectRelations(page);
    }

    // ==================== Project group-user assignment ====================

    /**
     * Assign user to project group
     */
    @Transactional
    public boolean assignUserToProjectGroup(Long projectGroupId, Long userId) {
        if (projectGroupMapper.selectById(projectGroupId) == null) {
            return false;
        }
        if (userMapper.selectById(userId) == null) {
            return false;
        }
        if (userProjectGroupMapper.selectByUserIdAndProjectGroupId(userId, projectGroupId) != null) {
            return false;
        }
        UserProjectGroup userProjectGroup = new UserProjectGroup();
        userProjectGroup.setProjectGroupId(projectGroupId);
        userProjectGroup.setUserId(userId);
        return userProjectGroupMapper.insert(userProjectGroup) > 0;
    }

    /**
     * Remove project group-user relation
     */
    @Transactional
    public boolean removeProjectGroupUser(Long relationId) {
        if (userProjectGroupMapper.selectById(relationId) == null) {
            return false;
        }
        return userProjectGroupMapper.deleteById(relationId) > 0;
    }

    /**
     * List project group-user assignment relations
     */
    public Page<ProjectGroupUserRelationResponse> listProjectGroupUserRelations(PageParam pageParam) {
        Page<ProjectGroupUserRelationResponse> page = new Page<>(pageParam.getPageNum(), pageParam.getPageSize());
        return userProjectGroupMapper.selectProjectGroupUserRelations(page);
    }
}
