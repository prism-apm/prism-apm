package org.prism.apm.ui.server.service.admin;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.prism.apm.ui.server.entity.model.project.Project;
import org.prism.apm.ui.server.entity.model.project.ProjectProjectGroup;
import org.prism.apm.ui.server.entity.param.common.PageParam;
import org.prism.apm.ui.server.mapper.project.ProjectMapper;
import org.prism.apm.ui.server.mapper.project.ProjectProjectGroupMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin project management service
 */
@Service
@DS("master")
public class AdminProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectProjectGroupMapper projectProjectGroupMapper;

    public AdminProjectService(ProjectMapper projectMapper, ProjectProjectGroupMapper projectProjectGroupMapper) {
        this.projectMapper = projectMapper;
        this.projectProjectGroupMapper = projectProjectGroupMapper;
    }


    /**
     * Add project
     */
    @Transactional
    public boolean addProject(Project project) {
        Project byName = projectMapper.selectByName(project.getName());
        if (byName != null) {
            return false;
        }
        return projectMapper.insert(project) > 0;
    }

    /**
     * Update project
     */
    @Transactional
    public boolean modifyProject(Project project) {
        Project byId = projectMapper.selectById(project.getId());
        if (byId == null) {
            return false;
        }
        Project byName = projectMapper.selectByName(project.getName());
        if (byName != null && !byName.getId().equals(project.getId())) {
            return false;
        }
        return projectMapper.updateById(project) > 0;
    }

    /**
     * Delete project
     */
    @Transactional
    public boolean deleteProject(Long id) {
        Project byId = projectMapper.selectById(id);
        if (byId == null) {
            return false;
        }
        boolean relation_exist = projectProjectGroupMapper.selectCount(
                Wrappers.lambdaQuery(ProjectProjectGroup.class)
                        .eq(ProjectProjectGroup::getProjectId, id)) != 0;
        if (relation_exist) {
            return false;
        }
        return projectMapper.deleteById(id) > 0;
    }

    /**
     * Query the project list with pagination
     */
    public Page<Project> listProjects(PageParam pageParam) {
        Page<Project> page = new Page<>(pageParam.getPageNum(), pageParam.getPageSize());
        return projectMapper.selectPage(page, null);
    }
} 