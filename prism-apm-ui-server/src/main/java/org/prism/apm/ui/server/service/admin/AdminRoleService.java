package org.prism.apm.ui.server.service.admin;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.prism.apm.ui.server.entity.model.security.Role;
import org.prism.apm.ui.server.entity.model.security.RolePermission;
import org.prism.apm.ui.server.entity.model.security.UserRole;
import org.prism.apm.ui.server.entity.param.common.PageParam;
import org.prism.apm.ui.server.mapper.security.RoleMapper;
import org.prism.apm.ui.server.mapper.security.RolePermissionMapper;
import org.prism.apm.ui.server.mapper.security.UserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin role management service
 */
@Service
@DS("master")
public class AdminRoleService {

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    public AdminRoleService(RoleMapper roleMapper, UserRoleMapper userRoleMapper, RolePermissionMapper rolePermissionMapper) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    /**
     * Add role
     */
    @Transactional
    public boolean addRole(Role role) {
        Role byName = roleMapper.findByName(role.getName());
        if (byName != null) {
            return false;
        }
        return roleMapper.insert(role) > 0;
    }

    /**
     * Update role
     */
    @Transactional
    public boolean modifyRole(Role role) {
        Role byId = roleMapper.selectById(role.getId());
        if (byId == null) {
            return false;
        }
        Role byName = roleMapper.findByName(role.getName());
        if (byName != null && !byName.getId().equals(role.getId())) {
            return false;
        }
        return roleMapper.updateById(role) > 0;
    }

    /**
     * Delete role
     */
    @Transactional
    public boolean deleteRole(Long id) {
        Role byId = roleMapper.selectById(id);
        if (byId == null) {
            return false;
        }
        boolean relation_exist = userRoleMapper.selectCount(
                Wrappers.lambdaQuery(UserRole.class)
                        .eq(UserRole::getRoleId, id)) != 0;
        if (relation_exist) {
            return false;
        }
        relation_exist = rolePermissionMapper.selectCount(
                Wrappers.lambdaQuery(RolePermission.class)
                        .eq(RolePermission::getRoleId, id)) != 0;
        if (relation_exist) {
            return false;
        }

        return roleMapper.deleteById(id) > 0;
    }

    /**
     * Query the role list with pagination
     */
    public Page<Role> listRoles(PageParam pageRequest) {
        Page<Role> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        return roleMapper.selectPage(page, null);
    }
} 