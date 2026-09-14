package org.prism.apm.ui.server.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.prism.apm.ui.server.entity.model.project.Project;
import org.prism.apm.ui.server.entity.model.project.ProjectGroup;
import org.prism.apm.ui.server.entity.param.common.PageParam;
import org.prism.apm.ui.server.entity.request.admin.DeleteRequest;
import org.prism.apm.ui.server.entity.response.ViewResponse;
import org.prism.apm.ui.server.service.admin.AdminProjectGroupService;
import org.prism.apm.ui.server.service.admin.AdminProjectService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminProjectController {

    private final AdminProjectGroupService adminProjectGroupService;

    private final AdminProjectService adminProjectService;

    public AdminProjectController(AdminProjectGroupService adminProjectGroupService, AdminProjectService adminProjectService) {
        this.adminProjectGroupService = adminProjectGroupService;
        this.adminProjectService = adminProjectService;
    }

    // ==================== Project group management ====================

    /**
     * Add project group
     */
    @PostMapping("/project-group/add")
    public ViewResponse.Result<?> addProjectGroup(@RequestBody ProjectGroup projectGroup) {
        boolean success = adminProjectGroupService.addProjectGroup(projectGroup);
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to add project group");
    }

    /**
     * Update project group
     */
    @PostMapping("/project-group/modify")
    public ViewResponse.Result<?> modifyProjectGroup(@RequestBody ProjectGroup projectGroup) {
        boolean success = adminProjectGroupService.modifyProjectGroup(projectGroup);
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to update project group");
    }

    /**
     * Delete project group
     */
    @PostMapping("/project-group/delete")
    public ViewResponse.Result<?> deleteProjectGroup(@RequestBody DeleteRequest request) {
        boolean success = adminProjectGroupService.deleteProjectGroup(request.getId());
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to delete project group");
    }

    /**
     * Project group list
     */
    @PostMapping("/project-group/list")
    public ViewResponse.Result<List<ProjectGroup>> listProjectGroups(@RequestBody PageParam pageParam) {
        Page<ProjectGroup> page = adminProjectGroupService.listProjectGroups(pageParam);
        return ViewResponse.Result.ok(page.getRecords());
    }

    // ==================== Project management ====================

    /**
     * Add project
     */
    @PostMapping("/project/add")
    public ViewResponse.Result<?> addProject(@RequestBody Project project) {
        boolean success = adminProjectService.addProject(project);
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to add project");
    }

    /**
     * Update project
     */
    @PostMapping("/project/modify")
    public ViewResponse.Result<?> modifyProject(@RequestBody Project project) {
        boolean success = adminProjectService.modifyProject(project);
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to update project");
    }

    /**
     * Delete project
     */
    @PostMapping("/project/delete")
    public ViewResponse.Result<?> deleteProject(@RequestBody DeleteRequest request) {
        boolean success = adminProjectService.deleteProject(request.getId());
        return success ? ViewResponse.Result.ok() :ViewResponse.Result.fail("Failed to delete project");
    }

    /**
     * Project list
     */
    @PostMapping("/project/list")
    public ViewResponse.Result<List<Project>> listProjects(@RequestBody PageParam pageParam) {
        Page<Project> page = adminProjectService.listProjects(pageParam);
        return ViewResponse.Result.ok(page.getRecords());
    }
}
