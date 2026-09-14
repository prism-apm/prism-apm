package org.prism.apm.ui.server.mapper.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.prism.apm.ui.server.entity.dto.route.RouteInfo;
import org.prism.apm.ui.server.entity.model.project.Project;
import org.prism.apm.ui.server.entity.param.common.DateParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface HostnameMapper extends BaseMapper<Project> {

    @Select("""
            select
              host_name
            from ${routeInfo.dynamicTableName}
            where
              service_name = #{serviceName}
              and span_kind = 2
              AND (time >= #{dateParam.startUTC}::TIMESTAMPTZ AND time < #{dateParam.endUTC}::TIMESTAMPTZ)
            group by host_name
            """)
    List<String> queryMachineByServiceName(@Param("dateParam") DateParam dateParam,
                                           @Param("serviceName") String serviceName,
                                           @Param("routeInfo") RouteInfo routeInfo);
} 