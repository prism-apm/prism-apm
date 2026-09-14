package org.prism.apm.ui.server.mapper.service;

import org.prism.apm.ui.server.entity.db.query.ServiceDataParam;
import org.prism.apm.ui.server.entity.dto.service.InnerCallStatsDTO;
import org.prism.apm.ui.server.entity.dto.route.RouteInfo;
import org.prism.apm.ui.server.entity.model.service.ServiceCallView;
import org.prism.apm.ui.server.entity.param.common.DateParam;
import org.prism.apm.ui.server.entity.param.common.PageParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ServiceCallMinuteDOMapper{

    ServiceCallView queryServiceSummaryByServiceName(@Param("dateParam") DateParam dateParam,
                                                     @Param("routeInfo") RouteInfo routeInfo,
                                                     @Param("param") ServiceDataParam param
    );

    /**
     * Query details for a single service
     */
    List<ServiceCallView> queryServiceDetailByServiceName(@Param("dateParam") DateParam dateParam,
                                                          @Param("routeInfo") RouteInfo routeInfo,
                                                          @Param("param") ServiceDataParam param
                                                          );


    /**
     * Query statistics for all endpoints of a single service
     */
    List<ServiceCallView> queryTargetSummaryByServiceName(@Param("dateParam") DateParam dateParam,
                                                          @Param("routeInfo") RouteInfo routeInfo,
                                                          @Param("param") ServiceDataParam param
    );


    /**
     * Query details for a single endpoint
     */
    List<ServiceCallView> queryTargetDetailByTargetName(@Param("dateParam") DateParam dateParam,
                                                        @Param("routeInfo") RouteInfo routeInfo,
                                                        @Param("param") ServiceDataParam param
                                                        );

    /**
     * Query internal endpoint details for a single endpoint
     */
    List<ServiceCallView> queryInnerActionDetailByActionName(@Param("dateParam") DateParam dateParam,
                                                             @Param("routeInfo") RouteInfo routeInfo,
                                                             @Param("param") ServiceDataParam param);
    List<ServiceCallView> queryInnerTargetDetailByTargetName(@Param("dateParam") DateParam dateParam,
                                                             @Param("routeInfo") RouteInfo routeInfo,
                                                             @Param("param") ServiceDataParam param);

    /**
     * Query internal endpoint statistics for a single endpoint
     * These endpoints mainly query internal calls for a single endpoint
     */
    InnerCallStatsDTO countAndSumInnerDetailByActionName(@Param("dateParam") DateParam dateParam,
                                                          @Param("routeInfo") RouteInfo routeInfo,
                                                          @Param("param") ServiceDataParam param);
    InnerCallStatsDTO countAndSumInnerDetailByTargetName(@Param("dateParam") DateParam dateParam,
                                                          @Param("routeInfo") RouteInfo routeInfo,
                                                          @Param("param") ServiceDataParam param);
    List<ServiceCallView> queryInnerDetailByActionName(@Param("dateParam") DateParam dateParam,
                                                       @Param("routeInfo") RouteInfo routeInfo,
                                                       @Param("param") ServiceDataParam param,
                                                       @Param("pageParam") PageParam pageParam);
    List<ServiceCallView> queryInnerDetailByTargetName(@Param("dateParam") DateParam dateParam,
                                                       @Param("routeInfo") RouteInfo routeInfo,
                                                       @Param("param") ServiceDataParam param,
                                                       @Param("pageParam") PageParam pageParam);

    /**
     * Query statistics for who called the endpoint
     * These endpoints mainly query who called an endpoint in reverse
     */
    List<ServiceCallView> queryServiceIfaceWhoCallMe(@Param("dateParam") DateParam dateParam,
                                                     @Param("pageParam") PageParam pageParam,
                                                     @Param("routeInfo") RouteInfo routeInfo,
                                                     @Param("param") ServiceDataParam param);
    Long countQueryServiceIfaceWhoCallMe(@Param("dateParam") DateParam dateParam,
                                         @Param("pageParam") PageParam pageParam,
                                         @Param("routeInfo") RouteInfo routeInfo,
                                         @Param("param") ServiceDataParam param);
    List<ServiceCallView> queryWhoCallMeGroupByService(@Param("dateParam") DateParam dateParam,
                                                       @Param("routeInfo") RouteInfo routeInfo,
                                                       @Param("param") ServiceDataParam param);


    /**
     * Query database component statistics for a single service
     */
    List<ServiceCallView> queryDbSummaryByServiceName(@Param("dateParam") DateParam dateParam,
                                                      @Param("routeInfo") RouteInfo routeInfo,
                                                      @Param("param") ServiceDataParam param
                                                      );


    /**
     * Query database component details for a single service
     */
    List<ServiceCallView> queryDbDetailByServiceName(@Param("dateParam") DateParam dateParam,
                                                     @Param("routeInfo") RouteInfo routeInfo,
                                                     @Param("param") ServiceDataParam param
    );

}