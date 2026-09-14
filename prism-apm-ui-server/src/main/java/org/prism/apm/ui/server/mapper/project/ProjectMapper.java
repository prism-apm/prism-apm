package org.prism.apm.ui.server.mapper.project;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.prism.apm.ui.server.entity.model.project.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
@DS("master")
public interface ProjectMapper extends BaseMapper<Project> {

    @Select("select * from projects where name = #{name}")
    Project selectByName(String name);
} 