package org.prism.apm.ui.server.mapper.project;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.prism.apm.ui.server.entity.response.admin.ProjectGroupProjectRelationResponse;
import org.prism.apm.ui.server.entity.model.project.ProjectProjectGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
@DS("master")
public interface ProjectProjectGroupMapper extends BaseMapper<ProjectProjectGroup> {
    void deleteByProjectIdAndProjectGroupId(@Param("projectId") Long projectId, @Param("projectGroupId") Long projectGroupId);

    @Select("SELECT id FROM project_project_group WHERE project_id = #{projectId} AND project_group_id = #{projectGroupId}")
    ProjectProjectGroup selectByProjectIdAndProjectGroupId(@Param("projectId") Long projectId, @Param("projectGroupId") Long projectGroupId);

    List<ProjectProjectGroup> findByProjectId(Long projectId);
    List<ProjectProjectGroup> findByProjectGroupId(Integer projectGroupId);

    @Select("SELECT ppg.id as relationId, ppg.project_group_id as projectGroupId, pg.name as groupName, ppg.project_id as projectId, p.name as projectName, p.display as projectDisplay, ppg.created_at as createTime " +
            "FROM project_project_group ppg " +
            "LEFT JOIN project_groups pg ON ppg.project_group_id = pg.id " +
            "LEFT JOIN projects p ON ppg.project_id = p.id " +
            "ORDER BY ppg.created_at DESC")
    Page<ProjectGroupProjectRelationResponse> selectProjectGroupProjectRelations(Page<ProjectGroupProjectRelationResponse> page);
} 