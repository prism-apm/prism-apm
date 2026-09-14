package org.prism.apm.ui.server.service.admin;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.prism.apm.ui.server.entity.model.project.ProjectGroup;
import org.prism.apm.ui.server.entity.model.project.ProjectProjectGroup;
import org.prism.apm.ui.server.entity.model.project.UserProjectGroup;
import org.prism.apm.ui.server.entity.param.common.PageParam;
import org.prism.apm.ui.server.mapper.project.ProjectGroupMapper;
import org.prism.apm.ui.server.mapper.project.ProjectProjectGroupMapper;
import org.prism.apm.ui.server.mapper.project.UserProjectGroupMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin project group management service
 */
@Service
@DS("master")
public class AdminProjectGroupService {

    private final ProjectGroupMapper projectGroupMapper;
    private final UserProjectGroupMapper userProjectGroupMapper;
    private final ProjectProjectGroupMapper projectProjectGroupMapper;

    public AdminProjectGroupService(ProjectGroupMapper projectGroupMapper, UserProjectGroupMapper userProjectGroupMapper, ProjectProjectGroupMapper projectProjectGroupMapper) {
        this.projectGroupMapper = projectGroupMapper;
        this.userProjectGroupMapper = userProjectGroupMapper;
        this.projectProjectGroupMapper = projectProjectGroupMapper;
    }

    /**
     * Add project group
     */
    @Transactional
    public boolean addProjectGroup(ProjectGroup projectGroup) {
        if (projectGroupMapper.selectByName(projectGroup.getName()) != null) {
            return false;
        }
        return projectGroupMapper.insert(projectGroup) > 0;
    }

    /**
     * Update project group
     */
    @Transactional
    public boolean modifyProjectGroup(ProjectGroup projectGroup) {
        if (projectGroupMapper.selectById(projectGroup.getId()) == null) {
            return false;
        }
        ProjectGroup byName = projectGroupMapper.selectByName(projectGroup.getName());
        if (byName != null && !byName.getId().equals(projectGroup.getId())) {
            return false;
        }
        return projectGroupMapper.updateById(projectGroup) > 0;
    }

    /**
     * Delete project group
     */
    @Transactional
    public boolean deleteProjectGroup(Long id) {
        ProjectGroup byId = projectGroupMapper.selectById(id);
        if (byId == null) {
            return false;
        }
        boolean relation_exist = userProjectGroupMapper.selectCount(
                Wrappers.lambdaQuery(UserProjectGroup.class)
                        .eq(UserProjectGroup::getProjectGroupId, id)) != 0;
        if (relation_exist) {
            return false;
        }
        relation_exist = projectProjectGroupMapper.selectCount(
                Wrappers.lambdaQuery(ProjectProjectGroup.class)
                        .eq(ProjectProjectGroup::getProjectGroupId, id)) != 0;
        if (relation_exist) {
            return false;
        }
        return projectGroupMapper.deleteById(id) > 0;
    }

    /**
     * Query the project group list with pagination
     */
    public Page<ProjectGroup> listProjectGroups(PageParam pageParam) {
        Page<ProjectGroup> page = new Page<>(pageParam.getPageNum(), pageParam.getPageSize());
        return projectGroupMapper.selectPage(page, null);
    }
} 