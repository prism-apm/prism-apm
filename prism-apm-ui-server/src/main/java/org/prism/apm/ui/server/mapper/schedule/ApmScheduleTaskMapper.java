package org.prism.apm.ui.server.mapper.schedule;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
@DS("master")
public interface ApmScheduleTaskMapper {

    String executeFunction(@Param("functionName") String functionName);
}
