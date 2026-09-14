package org.prism.apm.ui.server.service.admin;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.prism.apm.ui.server.entity.model.security.Permission;
import org.prism.apm.ui.server.entity.model.security.RolePermission;
import org.prism.apm.ui.server.entity.param.common.PageParam;
import org.prism.apm.ui.server.mapper.security.PermissionMapper;
import org.prism.apm.ui.server.mapper.security.RolePermissionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin permission management service
 */
@Service
@DS("master")
public class AdminPermissionService {

    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    public AdminPermissionService(PermissionMapper permissionMapper, RolePermissionMapper rolePermissionMapper) {
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    /**
     * Add permission
     */
    @Transactional
    public boolean addPermission(Permission permission) {
        return permissionMapper.insert(permission) > 0;
    }

    /**
     * Update permission
     */
    @Transactional
    public boolean modifyPermission(Permission permission) {
        return permissionMapper.updateById(permission) > 0;
    }

    /**
     * Delete permission
     */
    @Transactional
    public boolean deletePermission(Long id) {
        Permission byId = permissionMapper.selectById(id);
        if (byId == null) {
            return false;
        }
        boolean relation_exist = rolePermissionMapper.selectCount(
                Wrappers.lambdaQuery(RolePermission.class)
                        .eq(RolePermission::getPermissionId, id)) != 0;
        if (relation_exist) {
            return false;
        }
        return permissionMapper.deleteById(id) > 0;
    }

    /**
     * Query the permission list with pagination
     */
    public Page<Permission> listPermissions(PageParam pageRequest) {
        Page<Permission> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        return permissionMapper.selectPage(page, null);
    }
}
