package org.prism.apm.ui.server.mapper.topology;

import org.prism.apm.ui.server.entity.dto.route.RouteInfo;
import org.prism.apm.ui.server.entity.model.topology.ServiceTopologyRelationView;
import org.prism.apm.ui.server.entity.param.common.DateParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TopologyDataQueryMapper {

    List<ServiceTopologyRelationView> selectRpc(@Param("dateParam") DateParam dateParam, @Param("serviceList") List<String> serviceList,
                                                @Param("routeInfo") RouteInfo routeInfo);

    List<ServiceTopologyRelationView> selectJdbc(@Param("dateParam") DateParam dateParam, @Param("serviceList") List<String> serviceList,
                                                 @Param("routeInfo")RouteInfo routeInfo);

    List<ServiceTopologyRelationView> selectRedis(@Param("dateParam") DateParam dateParam, @Param("serviceList") List<String> serviceList,
                                                  @Param("routeInfo")RouteInfo routeInfo);

    List<ServiceTopologyRelationView> selectMqProducer(@Param("dateParam") DateParam dateParam, @Param("serviceList") List<String> serviceList,
                                                       @Param("routeInfo")RouteInfo routeInfo);

    List<ServiceTopologyRelationView> selectMqConsumer(@Param("dateParam") DateParam dateParam, @Param("serviceList") List<String> serviceList,
                                                       @Param("routeInfo")RouteInfo routeInfo);

    /**
     * Query service names that exceed the alert rule threshold
     */
    List<String> selectServicesAboveThreshold(@Param("dateParam") DateParam dateParam, @Param("serviceName") String serviceName, @Param("threshold") String threshold);

    /**
     * Query downstream services that exceed the threshold
     */
    List<ServiceTopologyRelationView> selectOverThresholdDownstream(@Param("dateParam") DateParam dateParam, @Param("serviceName") String serviceName, @Param("threshold") String threshold);

    /**
     * Query upstream services that exceed the threshold
     */
    List<ServiceTopologyRelationView> selectOverThresholdUpstream(@Param("dateParam") DateParam dateParam, @Param("serviceName") String serviceName, @Param("threshold") String threshold);
}