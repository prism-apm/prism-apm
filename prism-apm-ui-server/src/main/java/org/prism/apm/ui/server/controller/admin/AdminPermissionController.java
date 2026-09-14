package org.prism.apm.ui.server.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.prism.apm.ui.server.entity.model.security.Permission;
import org.prism.apm.ui.server.entity.model.security.Role;
import org.prism.apm.ui.server.entity.model.security.User;
import org.prism.apm.ui.server.entity.param.common.PageParam;
import org.prism.apm.ui.server.entity.request.admin.DeleteRequest;
import org.prism.apm.ui.server.entity.response.ViewResponse;
import org.prism.apm.ui.server.service.admin.AdminPermissionService;
import org.prism.apm.ui.server.service.admin.AdminRoleService;
import org.prism.apm.ui.server.service.admin.AdminUserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminPermissionController {

    private final AdminPermissionService adminPermissionService;

    private final AdminRoleService adminRoleService;

    private final AdminUserService adminUserService;

    public AdminPermissionController(AdminPermissionService adminPermissionService, AdminRoleService adminRoleService, AdminUserService adminUserService) {
        this.adminPermissionService = adminPermissionService;
        this.adminRoleService = adminRoleService;
        this.adminUserService = adminUserService;
    }

    // ==================== Role management ====================

    /**
     * Add role
     */
    @PostMapping("/role/add")
    public ViewResponse.Result<?> addRole(@RequestBody Role role) {
        boolean success = adminRoleService.addRole(role);
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to add role");
    }

    /**
     * Update role
     */
    @PostMapping("/role/modify")
    public ViewResponse.Result<?> modifyRole(@RequestBody Role role) {
        boolean success = adminRoleService.modifyRole(role);
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to update role");
    }

    /**
     * Delete role
     */
    @PostMapping("/role/delete")
    public ViewResponse.Result<?> deleteRole(@RequestBody DeleteRequest request) {
        boolean success = adminRoleService.deleteRole(request.getId());
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to delete role");
    }

    /**
     * Role list
     */
    @PostMapping("/role/list")
    public ViewResponse.Result<List<Role>> listRoles(@RequestBody PageParam pageRequest) {
        Page<Role> page = adminRoleService.listRoles(pageRequest);
        return ViewResponse.Result.ok(page.getRecords());
    }

    // ==================== Permission management ====================

    /**
     * Add permission
     */
    @PostMapping("/permission/add")
    public ViewResponse.Result<?> addPermission(@RequestBody Permission permission) {
        boolean success = adminPermissionService.addPermission(permission);
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to add permission");
    }

    /**
     * Update permission
     */
    @PostMapping("/permission/modify")
    public ViewResponse.Result<?> modifyPermission(@RequestBody Permission permission) {
        boolean success = adminPermissionService.modifyPermission(permission);
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to update permission");
    }

    /**
     * Delete permission
     */
    @PostMapping("/permission/delete")
    public ViewResponse.Result<?> deletePermission(@RequestBody DeleteRequest request) {
        boolean success = adminPermissionService.deletePermission(request.getId());
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to delete permission");
    }

    /**
     * Permission list
     */
    @PostMapping("/permission/list")
    public ViewResponse.Result<List<Permission>> listPermissions(@RequestBody PageParam pageRequest) {
        Page<Permission> page = adminPermissionService.listPermissions(pageRequest);
        return ViewResponse.Result.ok(page.getRecords());
    }

    // ==================== User management ====================

    /**
     * Add user
     */
    @PostMapping("/user/add")
    public ViewResponse.Result<?> addUser(@RequestBody User user) {
        boolean success = adminUserService.addUser(user);
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to add user");
    }

    /**
     * Update user
     */
    @PostMapping("/user/modify")
    public ViewResponse.Result<?> modifyUser(@RequestBody User user) {
        boolean success = adminUserService.modifyUser(user);
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to update user");
    }

    /**
     * Delete user
     */
    @PostMapping("/user/delete")
    public ViewResponse.Result<?> deleteUser(@RequestBody DeleteRequest request) {
        boolean success = adminUserService.deleteUser(request.getId());
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to delete user");
    }

    /**
     * User list
     */
    @PostMapping("/user/list")
    public ViewResponse.Result<List<User>> listUsers(@RequestBody PageParam pageRequest) {
        Page<User> page = adminUserService.listUsers(pageRequest);
        return ViewResponse.Result.ok(page.getRecords());
    }
}
