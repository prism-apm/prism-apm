package org.prism.apm.ui.server.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.prism.apm.ui.server.entity.param.common.PageParam;
import org.prism.apm.ui.server.entity.request.admin.AssignProjectGroupProjectRequest;
import org.prism.apm.ui.server.entity.request.admin.AssignProjectGroupUserRequest;
import org.prism.apm.ui.server.entity.request.admin.AssignRolePermissionRequest;
import org.prism.apm.ui.server.entity.request.admin.AssignRoleUserRequest;
import org.prism.apm.ui.server.entity.request.admin.RemoveRelationRequest;
import org.prism.apm.ui.server.entity.response.ViewResponse;
import org.prism.apm.ui.server.entity.response.admin.ProjectGroupProjectRelationResponse;
import org.prism.apm.ui.server.entity.response.admin.ProjectGroupUserRelationResponse;
import org.prism.apm.ui.server.entity.response.admin.RolePermissionRelationResponse;
import org.prism.apm.ui.server.entity.response.admin.UserRoleRelationResponse;
import org.prism.apm.ui.server.service.admin.AdminAssignService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminAssignController {

    private final AdminAssignService adminAssignService;

    public AdminAssignController(AdminAssignService adminAssignService) {
        this.adminAssignService = adminAssignService;
    }

    // ==================== User-role assignment ====================

    /**
     * Assign role to user
     */
    @PostMapping("/role-user/assign")
    public ViewResponse.Result<?> assignRoleToUser(@RequestBody AssignRoleUserRequest request) {
        boolean success = adminAssignService.assignRoleToUser(request.getRoleId(), request.getUserId());
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to assign role to user");
    }

    /**
     * Remove role from user
     */
    @PostMapping("/role-user/remove")
    public ViewResponse.Result<?> removeUserRole(@RequestBody RemoveRelationRequest request) {
        boolean success = adminAssignService.removeUserRole(request.getRelationId());
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to remove user role");
    }

    /**
     * List user-role assignment relations
     */
    @PostMapping("/role-user/list")
    public ViewResponse.Result<List<UserRoleRelationResponse>> listUserRoleRelations(@RequestBody PageParam pageParam) {
        Page<UserRoleRelationResponse> page = adminAssignService.listUserRoleRelations(pageParam);
        return ViewResponse.Result.ok(page.getRecords());
    }

    // ==================== Role-permission assignment ====================

    /**
     * Assign endpoint resource to role
     */
    @PostMapping("/role-permission/assign")
    public ViewResponse.Result<?> assignPermissionToRole(@RequestBody AssignRolePermissionRequest request) {
        boolean success = adminAssignService.assignPermissionToRole(request.getRoleId(), request.getPermissionId());
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to assign endpoint resource to role");
    }

    /**
     * Remove endpoint resource from role
     */
    @PostMapping("/role-permission/remove")
    public ViewResponse.Result<?> removeRolePermission(@RequestBody RemoveRelationRequest request) {
        boolean success = adminAssignService.removeRolePermission(request.getRelationId());
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to remove role endpoint resource");
    }

    /**
     * List role-permission assignment relations
     */
    @PostMapping("/role-permission/list")
    public ViewResponse.Result<List<RolePermissionRelationResponse>> listRolePermissionRelations(@RequestBody PageParam pageParam) {
        Page<RolePermissionRelationResponse> page = adminAssignService.listRolePermissionRelations(pageParam);
        return ViewResponse.Result.ok(page.getRecords());
    }

    // ==================== Project group-project assignment ====================

    /**
     * Add project to project group
     */
    @PostMapping("/projectGroup-project/assign")
    public ViewResponse.Result<?> assignProjectToProjectGroup(@RequestBody AssignProjectGroupProjectRequest request) {
        boolean success = adminAssignService.assignProjectToProjectGroup(request.getProjectGroupId(), request.getProjectId());
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to add project to project group");
    }

    /**
     * Remove project from project group
     */
    @PostMapping("/projectGroup-project/remove")
    public ViewResponse.Result<?> removeProjectGroupProject(@RequestBody RemoveRelationRequest request) {
        boolean success = adminAssignService.removeProjectGroupProject(request.getRelationId());
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to remove project group project");
    }

    /**
     * List project group-project assignment relations
     */
    @PostMapping("/projectGroup-project/list")
    public ViewResponse.Result<List<ProjectGroupProjectRelationResponse>> listProjectGroupProjectRelations(@RequestBody PageParam pageParam) {
        Page<ProjectGroupProjectRelationResponse> page = adminAssignService.listProjectGroupProjectRelations(pageParam);
        return ViewResponse.Result.ok(page.getRecords());
    }

    // ==================== Project group-user assignment ====================

    /**
     * Add user to project group
     */
    @PostMapping("/projectGroup-user/assign")
    public ViewResponse.Result<?> assignUserToProjectGroup(@RequestBody AssignProjectGroupUserRequest request) {
        boolean success = adminAssignService.assignUserToProjectGroup(request.getProjectGroupId(), request.getUserId());
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to add user to project group");
    }

    /**
     * Remove user from project group
     */
    @PostMapping("/projectGroup-user/remove")
    public ViewResponse.Result<?> removeProjectGroupUser(@RequestBody RemoveRelationRequest request) {
        boolean success = adminAssignService.removeProjectGroupUser(request.getRelationId());
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to remove project group user");
    }

    /**
     * List project group-user assignment relations
     */
    @PostMapping("/projectGroup-user/list")
    public ViewResponse.Result<List<ProjectGroupUserRelationResponse>> listProjectGroupUserRelations(@RequestBody PageParam pageParam) {
        Page<ProjectGroupUserRelationResponse> page = adminAssignService.listProjectGroupUserRelations(pageParam);
        return ViewResponse.Result.ok(page.getRecords());
    }
}
