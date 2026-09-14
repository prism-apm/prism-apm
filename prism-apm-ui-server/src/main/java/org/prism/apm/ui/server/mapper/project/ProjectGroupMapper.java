package org.prism.apm.ui.server.mapper.project;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.prism.apm.ui.server.entity.model.project.ProjectGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
@DS("master")
public interface ProjectGroupMapper extends BaseMapper<ProjectGroup> {

    @Select("select * from project_groups where name = #{name}")
    ProjectGroup selectByName(String name);
} 