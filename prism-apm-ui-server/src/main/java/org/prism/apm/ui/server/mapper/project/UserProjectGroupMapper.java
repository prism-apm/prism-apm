package org.prism.apm.ui.server.mapper.project;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.prism.apm.ui.server.entity.model.project.Project;
import org.prism.apm.ui.server.entity.response.admin.ProjectGroupUserRelationResponse;
import org.prism.apm.ui.server.entity.model.project.UserProjectGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
@DS("master")
public interface UserProjectGroupMapper extends BaseMapper<UserProjectGroup> {

    @Select("SELECT id FROM user_project_group WHERE user_id = #{userId} AND project_group_id = #{projectGroupId}")
    UserProjectGroup selectByUserIdAndProjectGroupId(@Param("userId") Long userId, @Param("projectGroupId") Long projectGroupId);

    @Select("SELECT upg.id as relationId, upg.project_group_id as projectGroupId, pg.name as groupName, upg.user_id as userId, u.username, upg.created_at as createTime " +
            "FROM user_project_group upg " +
            "LEFT JOIN project_groups pg ON upg.project_group_id = pg.id " +
            "LEFT JOIN users u ON upg.user_id = u.id " +
            "ORDER BY upg.created_at DESC")
    Page<ProjectGroupUserRelationResponse> selectProjectGroupUserRelations(Page<ProjectGroupUserRelationResponse> page);

    @Select("select p.* from user_project_group upg join project_project_group ppg on upg.project_group_id = ppg.project_group_id " +
            "join projects p on ppg.project_id = p.id where upg.user_id = #{userId}")
    List<Project> selectProjectsByUserId(@Param("userId") Long userId);
}