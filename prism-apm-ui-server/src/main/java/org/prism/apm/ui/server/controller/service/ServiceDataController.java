package org.prism.apm.ui.server.controller.service;

import org.prism.apm.ui.server.entity.param.common.DateParam;
import org.prism.apm.ui.server.entity.param.common.PageParam;
import org.prism.apm.ui.server.entity.response.service.IfaceAnalyseMapResponse;
import org.prism.apm.ui.server.entity.response.service.IfaceDetailResponse;
import org.prism.apm.ui.server.entity.response.service.ServiceDetailInfoResponse;
import org.prism.apm.ui.server.entity.param.service.AnalyseMapQueryParam;
import org.prism.apm.ui.server.entity.param.service.GatherQueryParam;
import org.prism.apm.ui.server.entity.param.service.WhoCallMeQueryParam;
import org.prism.apm.ui.server.entity.response.ViewResponse;
import org.prism.apm.ui.server.entity.response.service.IfaceGatherInfoResponse;
import org.prism.apm.ui.server.entity.response.service.ServiceOverviewResponse;
import org.prism.apm.ui.server.entity.response.service.TransactionInfoResponse;
import org.prism.apm.ui.server.entity.response.service.WhoCallMeResponse;
import org.prism.apm.ui.server.service.service.OverViewService;
import org.prism.apm.ui.server.service.service.ServiceDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/apm/service")
public class ServiceDataController {

    private final ServiceDataService serviceDataService;
    private final OverViewService overViewService;

    public ServiceDataController(ServiceDataService serviceDataService, OverViewService overViewService) {
        this.serviceDataService = serviceDataService;
        this.overViewService = overViewService;
    }

    @GetMapping("/overview")
    public ViewResponse.Result<ServiceOverviewResponse> getServiceOverview(DateParam dateParam, String serviceName) {
        return ViewResponse.Result.ok(overViewService.getServiceOverview(serviceName, dateParam));
    }

    /**
     * APM => Service Overview
     */
    @GetMapping("/details")
    public ViewResponse.Result<List<ServiceDetailInfoResponse>> getServiceDetails(DateParam dateParam, String serviceName, String machineId) {
        return ViewResponse.Result.ok(serviceDataService.getServiceDetails(dateParam, serviceName, machineId));
    }

    /**
     * APM => Endpoint Details => Metrics
     */
    @GetMapping("/transaction/list")
    public ViewResponse.Result<List<TransactionInfoResponse>> getTransactionList(DateParam dateParam, String serviceName, String machineId) {
        return ViewResponse.Result.ok(serviceDataService.getTransactionList(dateParam, serviceName, machineId));
    }

    /**
     * APM => Endpoint Details => Internal Details/RED Comparison
     */
    @GetMapping("/iface/detail")
    public ViewResponse.Result<List<IfaceDetailResponse>> getServiceIfaceDetail(DateParam dateParam, String serviceName, String ifaceName, String ifaceNameHashKey, String machineId) {
        return ViewResponse.Result.ok(serviceDataService.getServiceIfaceDetail(dateParam, serviceName, ifaceName, ifaceNameHashKey, machineId));
    }

    /**
     * APM => Endpoint Details => Internal Details => Internal Endpoint Call Duration
     */
    @GetMapping("/iface/map")
    public ViewResponse.Result<List<IfaceAnalyseMapResponse>> getIfaceInnerMapData(DateParam dateParam, AnalyseMapQueryParam param) {
        return ViewResponse.Result.ok(serviceDataService.getIfaceMapData(dateParam, param));
    }

    /**
     * APM => Endpoint Details => Internal Endpoint Analysis
     */
    @GetMapping("/iface/gather")
    public ViewResponse.Page<IfaceGatherInfoResponse> getServiceIfaceAnalyseGather(DateParam dateParam, PageParam pageParam, GatherQueryParam param) {
        return serviceDataService.getServiceIfaceAnalyseGather(dateParam, pageParam, param);
    }

    /**
     * Query statistics for who called the endpoint
     */
    @GetMapping("/iface/from/iface")
    public ViewResponse.Page<WhoCallMeResponse> getServiceIfaceWhoCallMe(DateParam dateParam, PageParam pageParam, WhoCallMeQueryParam param) {
        return serviceDataService.getWhoCallMeGroupByIface(dateParam, pageParam, param);
    }
    /**
     * Query statistics for who called the endpoint
     */
    @GetMapping("/iface/from/service")
    public ViewResponse.Page<WhoCallMeResponse> whoCallMeGroupByService(DateParam dateParam, WhoCallMeQueryParam param) {
        return serviceDataService.getWhoCallMeGroupByService(dateParam, param);
    }
    /**
     * APM => Database (list)
     */
    @GetMapping("/db/list")
    public ViewResponse.Result<List<TransactionInfoResponse>> getDbList(DateParam dateParam, String serviceName, String systemType, Short spanKind, String machineId) {
        return ViewResponse.Result.ok(serviceDataService.getDbList(dateParam, serviceName, systemType, spanKind, machineId));
    }

    /**
     * APM => Database (detail)
     */
    @GetMapping("/db/detail")
    public ViewResponse.Result<List<ServiceDetailInfoResponse>> getDbDetail(DateParam dateParam, String serviceName, String machineId,
                                                                            String ifaceCaller, String ifaceCallerHashKey,
                                                                            String ifaceName, String ifaceNameHashKey,
                                                                            Short spanKind, String systemType) {
        return ViewResponse.Result.ok(serviceDataService.getDbDetail(dateParam, serviceName, machineId, ifaceCaller, ifaceCallerHashKey, ifaceName, ifaceNameHashKey, spanKind, systemType));
    }
}
