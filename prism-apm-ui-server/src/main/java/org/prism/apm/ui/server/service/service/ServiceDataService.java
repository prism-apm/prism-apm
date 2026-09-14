package org.prism.apm.ui.server.service.service;

import org.prism.apm.ui.server.constant.CommonConstants;
import org.prism.apm.ui.server.constant.GroupBy;
import org.prism.apm.ui.server.constant.OrderBy;
import org.prism.apm.ui.server.constant.SpanConstants;
import org.prism.apm.ui.server.constant.route.DynamicTablePlaceholder;
import org.prism.apm.ui.server.entity.db.query.ServiceDataParam;
import org.prism.apm.ui.server.entity.dto.service.InnerCallStatsDTO;
import org.prism.apm.ui.server.entity.dto.route.RouteInfo;
import org.prism.apm.ui.server.entity.model.service.ServiceCallView;
import org.prism.apm.ui.server.entity.param.common.DateParam;
import org.prism.apm.ui.server.entity.param.common.PageParam;
import org.prism.apm.ui.server.entity.param.service.AnalyseMapQueryParam;
import org.prism.apm.ui.server.entity.param.service.GatherQueryParam;
import org.prism.apm.ui.server.entity.param.service.WhoCallMeQueryParam;
import org.prism.apm.ui.server.entity.response.ViewResponse;
import org.prism.apm.ui.server.entity.response.base.RedMetricResponse;
import org.prism.apm.ui.server.entity.response.service.IfaceAnalyseMapResponse;
import org.prism.apm.ui.server.entity.response.service.IfaceDetailResponse;
import org.prism.apm.ui.server.entity.response.service.IfaceGatherInfoResponse;
import org.prism.apm.ui.server.entity.response.service.ServiceDetailInfoResponse;
import org.prism.apm.ui.server.entity.response.service.TransactionInfoResponse;
import org.prism.apm.ui.server.entity.response.service.WhoCallMeResponse;
import org.prism.apm.ui.server.enums.SpanKindEnum;
import org.prism.apm.ui.server.enums.SystemTypeEnum;
import org.prism.apm.ui.server.mapper.service.ServiceCallMinuteDOMapper;
import org.prism.apm.ui.server.route.TableRoutingService;
import org.prism.apm.ui.server.utils.DateUtils;
import org.prism.apm.ui.server.utils.FillDataHoleUtil;
import org.prism.apm.ui.server.utils.ParamCheckUtils;
import org.prism.apm.ui.server.utils.UnitConvertUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServiceDataService {

    private final ServiceCallMinuteDOMapper serviceCallMinuteDOMapper;
    private final CommonService commonService;
    private final TableRoutingService tableRoutingService;
    private final HashDictionaryService hashDictionaryService;

    public ServiceDataService(ServiceCallMinuteDOMapper serviceCallMinuteDOMapper,
                              CommonService commonService,
                              TableRoutingService tableRoutingService,
                              HashDictionaryService hashDictionaryService) {
        this.serviceCallMinuteDOMapper = serviceCallMinuteDOMapper;
        this.commonService = commonService;
        this.tableRoutingService = tableRoutingService;
        this.hashDictionaryService = hashDictionaryService;
    }



    /**
     * Query service RED metrics
     */
    public List<ServiceDetailInfoResponse> getServiceDetails(DateParam dateParam, String serviceName, String machineId) {
        ParamCheckUtils.validateDateAndServiceName(dateParam, serviceName);
        dateParam.convertToUTC();
        ParamCheckUtils.validateServiceNamePermission(commonService.getCurrentUserServiceNameList(), serviceName);
        RouteInfo routeInfo = tableRoutingService.getTableName(dateParam, DynamicTablePlaceholder.TAG_ENTRY);

        ServiceDataParam param = ServiceDataParam.builder()
                .strInterval(DateUtils.getDefaultTimeBucket())
                .serviceName(serviceName)
                .hostName(machineId)
                .build();

        List<ServiceCallView> data = serviceCallMinuteDOMapper
                .queryServiceDetailByServiceName(
                        dateParam,
                        routeInfo,
                        param
                );

        if (CollectionUtils.isEmpty(data)) {
            return Collections.emptyList();
        }

        data = FillDataHoleUtil.fillServiceMinuteCallDataHoleInDatetimeRange(dateParam, data, routeInfo);

        return data.stream()
                .map(ServiceDetailInfoResponse::ConvertFrom)
                .collect(Collectors.toList());
    }

    /**
     * Query endpoints for a single service
     */
    public List<TransactionInfoResponse> getTransactionList(DateParam dateParam, String serviceName, String machineId) {
        ParamCheckUtils.validateDateAndServiceName(dateParam, serviceName);
        dateParam.convertToUTC();
        ParamCheckUtils.validateServiceNamePermission(commonService.getCurrentUserServiceNameList(), serviceName);
        RouteInfo routeInfo = tableRoutingService.getTableName(dateParam, DynamicTablePlaceholder.TAG_ENTRY);
        ServiceDataParam param = ServiceDataParam.builder()
                .serviceName(serviceName)
                .hostName(machineId)
                .build();

        List<ServiceCallView> data = serviceCallMinuteDOMapper
                .queryTargetSummaryByServiceName(
                        dateParam,
                        routeInfo,
                        param
                        );

        if (CollectionUtils.isEmpty(data)) {
            return Collections.emptyList();
        }

        // Extract all HashKeys and batch query the dictionary
        Map<String, String> hashToTextMap = hashDictionaryService.buildHashToTextMapFromServiceCallView(data);

        // Convert and translate
        List<TransactionInfoResponse> result = new ArrayList<>();
        for (ServiceCallView d : data){
            TransactionInfoResponse r = TransactionInfoResponse.convertFrom(d, hashToTextMap);
            r.setMethod(r.getIfaceName());
            result.add(r);
        }
        return result;
    }

    /**
     * Query metrics for an endpoint with timestamps
     */
    public List<IfaceDetailResponse> getServiceIfaceDetail(DateParam dateParam, String serviceName, String ifaceName, String ifaceNameHashKey, String machineId) {
        ParamCheckUtils.validateDateAndServiceNameAndIfaceName(dateParam, serviceName, ifaceName);
        dateParam.convertToUTC();
        ParamCheckUtils.validateServiceNamePermission(commonService.getCurrentUserServiceNameList(), serviceName);
        RouteInfo routeInfo = tableRoutingService.getTableName(dateParam, DynamicTablePlaceholder.TAG_ENTRY);
        ServiceDataParam param = ServiceDataParam.builder()
                .serviceName(serviceName)
                .targetName(ifaceName)
                .targetNameHashKey(ifaceNameHashKey)
                .hostName(machineId)
                .strInterval(DateUtils.getDefaultTimeBucket())
                .build();

        List<ServiceCallView> data = serviceCallMinuteDOMapper
                .queryTargetDetailByTargetName(
                        dateParam,
                        routeInfo,
                        param
                );
        data = FillDataHoleUtil.fillServiceMinuteCallDataHoleInDatetimeRange(dateParam, data, routeInfo);

        return data.stream()
                .map(IfaceDetailResponse::convertFrom)
                .collect(Collectors.toList());
    }

    /**
     * Query the time axis for internal endpoint calls
     */
    public List<IfaceAnalyseMapResponse> getIfaceMapData(DateParam dateParam, AnalyseMapQueryParam param) {
        ParamCheckUtils.validateDateAndServiceNameAndIfaceName(dateParam, param.getServiceName(), param.getIfaceName());
        dateParam.convertToUTC();
        ParamCheckUtils.validateServiceNamePermission(commonService.getCurrentUserServiceNameList(), param.getServiceName());

        ServiceDataParam db_query_param = ServiceDataParam.builder()
                .strInterval(DateUtils.getDefaultTimeBucket())
                .build().convertFrom(param);

        RouteInfo routeInfo = tableRoutingService.getTableName(dateParam, DynamicTablePlaceholder.TAG_DEPENDENCY);

        // 2. Database query
        List<ServiceCallView> data;
        boolean isGroupByAction = param.getGroupByKey() == null || param.getGroupByKey().equals(GroupBy.GROUP_BY_KEY_ACTION);

        if (isGroupByAction) {
            data = serviceCallMinuteDOMapper.queryInnerActionDetailByActionName(dateParam, routeInfo, db_query_param);
        } else {
            data = serviceCallMinuteDOMapper.queryInnerTargetDetailByTargetName(dateParam, routeInfo, db_query_param);
        }

        if (CollectionUtils.isEmpty(data)) {
            return new ArrayList<>();
        }

        // Extract all HashKeys and batch query the dictionary (translate targetName)
        Map<String, String> hashToTextMap = hashDictionaryService.buildHashToTextMapFromTargetName(data);

        // 3. Define the group-key extraction strategy, replacing the original Wrapper name-setting logic
        // Use ActionName when grouping by Action; otherwise use TargetName
        Function<ServiceCallView, String> nameExtractor = isGroupByAction
                ? ServiceCallView::getActionName
                : ServiceCallView::getTargetName;

        return data.stream()
                .collect(Collectors.groupingBy(
                        nameExtractor,
                        Collectors.collectingAndThen(
                                Collectors.toList(), // Collect into List first
                                list -> FillDataHoleUtil.fillServiceMinuteCallDataHoleInDatetimeRange( // Then process the List immediately
                                        dateParam,
                                        list,
                                        routeInfo
                                )
                        )
                ))
                .entrySet().stream()
                .map(entry -> {

                    List<ServiceCallView> filled_list = entry.getValue();
                    String system_type = filled_list.stream()
                            .map(ServiceCallView::getSystemType)
                            .filter(StringUtils::hasText)
                            .findFirst()
                            .orElse(SpanConstants.DEFAULT_STRING_VALUE_WHEN_NO_DATA);
                    String iface_name_hash = entry.getKey();
                    // Translate: restore HashKey to original text
                    String iface_name = hashToTextMap.getOrDefault(iface_name_hash, iface_name_hash);
                    String resultKey = String.format("%s: %s", system_type, iface_name);

                    List<IfaceDetailResponse> responseList = filled_list.stream()
                            .map(IfaceDetailResponse::convertFrom)
                            .collect(Collectors.toList());

                    return new IfaceAnalyseMapResponse(resultKey, responseList);
                })
                .collect(Collectors.toList());
    }

    /**
     * Query statistics for internal endpoint calls
     */
    public ViewResponse.Page<IfaceGatherInfoResponse> getServiceIfaceAnalyseGather(DateParam dateParam, PageParam pageParam, GatherQueryParam param) {
        // 1. Parameter validation (unchanged)
        ParamCheckUtils.validateDateAndServiceNameAndIfaceName(dateParam,
                param.getServiceName(),
                param.getIfaceName());
        dateParam.convertToUTC();
        ParamCheckUtils.validateServiceNamePermission(commonService.getCurrentUserServiceNameList(), param.getServiceName());
        RouteInfo routeInfo = tableRoutingService.getTableName(dateParam, DynamicTablePlaceholder.TAG_DEPENDENCY);

        // 2. Prepare query parameters
        ServiceDataParam db_query_param = ServiceDataParam.builder()
                .strInterval(DateUtils.getDefaultTimeBucket())
                .build()
                .convertFrom(param);
        param.setIfaceTotalTime(
                UnitConvertUtils.convertMillisecondsToMicroseconds_BigDecimal(
                        param.getIfaceAvgDuration()).multiply(BigDecimal.valueOf(param.getIfaceTotalCount())
                )
        );

        // 3. [Added]Convert pagination parameters
        pageParam.convertPageNumToOffset();

        // 4. [Key change]Query statistics first to get total rows and total duration
        boolean isGroupByAction = param.getGroupByKey() == null || param.getGroupByKey().equals(GroupBy.GROUP_BY_KEY_ACTION);
        InnerCallStatsDTO stats;
        if (isGroupByAction) {
            stats = serviceCallMinuteDOMapper.countAndSumInnerDetailByActionName(dateParam, routeInfo, db_query_param);
        } else {
            stats = serviceCallMinuteDOMapper.countAndSumInnerDetailByTargetName(dateParam, routeInfo, db_query_param);
        }

        long total = stats.getTotalRows();
        BigDecimal totalInnerCallsTime = stats.getTotalDurationSum();

        // 5. If there is no data, return empty directly
        if (total == 0) {
            return ViewResponse.Page.okEmpty();
        }

        // 6. [Key change]Query only current-page data; sorting and pagination are done in the database layer
        List<ServiceCallView> pagedInnerCalls;
        if (isGroupByAction) {
            pagedInnerCalls = serviceCallMinuteDOMapper.queryInnerDetailByActionName(dateParam, routeInfo, db_query_param, pageParam);
        } else {
            pagedInnerCalls = serviceCallMinuteDOMapper.queryInnerDetailByTargetName(dateParam, routeInfo, db_query_param, pageParam);
        }

        // 7. Translate HashKey
        Map<String, String> hashToTextMap = hashDictionaryService.buildHashToTextMapFromTargetName(pagedInnerCalls);

        // 8. Convert current-page data directly; no extra sorting/pagination is needed
        List<IfaceGatherInfoResponse> convertedData = convertToGatherInfoList(pagedInnerCalls, param, hashToTextMap);

        // 9. Add table header and footer, using totalInnerCallsTime returned by the database
        List<IfaceGatherInfoResponse> finalList = this.addHeaderAndFooter(convertedData, totalInnerCallsTime, param);

        // 10. Return paginated response
        return ViewResponse.Page.ok(total, finalList);
    }

    /**
     * Query statistics for who called the endpoint
     */
    public ViewResponse.Page<WhoCallMeResponse> getWhoCallMeGroupByIface(DateParam dateParam, PageParam pageParam, WhoCallMeQueryParam param) {
        ParamCheckUtils.validateDateAndServiceNameAndIfaceName(dateParam,
                param.getServiceName(),
                param.getIfaceName());
        dateParam.convertToUTC();
        ParamCheckUtils.validateServiceNamePermission(commonService.getCurrentUserServiceNameList(), param.getServiceName());
        RouteInfo routeInfo = tableRoutingService.getTableName(dateParam, DynamicTablePlaceholder.TAG_ENTRY);
        pageParam.convertPageNumToOffset();
        pageParam.convertToDatabaseSort(pageParam.getSortBy(), OrderBy.Fields.AVG_DURATION, pageParam.getSortOrder());

        ServiceDataParam db_query_param = ServiceDataParam.builder().build()
                .convertFrom(param);

        List<ServiceCallView> data = serviceCallMinuteDOMapper.queryServiceIfaceWhoCallMe(dateParam, pageParam, routeInfo, db_query_param);

        if (CollectionUtils.isEmpty(data)) {
            return ViewResponse.Page.okEmpty();
        }

        long total = serviceCallMinuteDOMapper.countQueryServiceIfaceWhoCallMe(dateParam, pageParam, routeInfo, db_query_param);

        // Extract all callerTargetName HashKeys and batch query the dictionary
        Map<String, String> hashToTextMap = hashDictionaryService.buildHashToTextMapFromCallerTargetName(data);

        List<WhoCallMeResponse> converted_data = data.stream()
                .map(d -> WhoCallMeResponse.convertFrom(d, hashToTextMap))
                .collect(Collectors.toList());
        return ViewResponse.Page.ok(total, converted_data);
    }
    /**
     * Query statistics for who called the endpoint
     */
    public ViewResponse.Page<WhoCallMeResponse> getWhoCallMeGroupByService(DateParam dateParam, WhoCallMeQueryParam param) {
        ParamCheckUtils.validateDateAndServiceNameAndIfaceName(dateParam,
                param.getServiceName(),
                param.getIfaceName());
        dateParam.convertToUTC();
        ParamCheckUtils.validateServiceNamePermission(commonService.getCurrentUserServiceNameList(), param.getServiceName());
        RouteInfo routeInfo = tableRoutingService.getTableName(dateParam, DynamicTablePlaceholder.TAG_ENTRY);

        ServiceDataParam db_query_param = ServiceDataParam.builder().build()
                .convertFrom(param);
        List<ServiceCallView> data = serviceCallMinuteDOMapper.queryWhoCallMeGroupByService(dateParam, routeInfo, db_query_param);

        if (CollectionUtils.isEmpty(data)) {
            return ViewResponse.Page.okEmpty();
        }

        // Extract all callerTargetName HashKeys and batch query the dictionary
        Map<String, String> hashToTextMap = hashDictionaryService.buildHashToTextMapFromCallerTargetName(data);

        List<WhoCallMeResponse> converted_data = data.stream()
                .map(d -> WhoCallMeResponse.convertFrom(d, hashToTextMap))
                .sorted(Comparator.comparing((WhoCallMeResponse o) -> o.getRedMetric().getAvgDuration()).reversed())
                .collect(Collectors.toList());
        return ViewResponse.Page.ok(converted_data.size(), converted_data);
    }

    public List<TransactionInfoResponse> getDbList(DateParam dateParam, String serviceName, String systemType, Short spanKind, String hostName) {
        ParamCheckUtils.validateDateAndServiceName(dateParam, serviceName);
        dateParam.convertToUTC();
        ParamCheckUtils.validateSystemType(systemType);
        spanKind = hackSpanKind(systemType, spanKind).getValue();
        ParamCheckUtils.validateSpinKind(spanKind);
        ParamCheckUtils.validateServiceNamePermission(commonService.getCurrentUserServiceNameList(), serviceName);
        RouteInfo routeInfo;
        if (SpanKindEnum.CONSUMER.getValue() == spanKind) {
            routeInfo = tableRoutingService.getTableName(dateParam, DynamicTablePlaceholder.TAG_ENTRY);
        } else {
            routeInfo = tableRoutingService.getTableName(dateParam, DynamicTablePlaceholder.TAG_DEPENDENCY);
        }

        ServiceDataParam param = ServiceDataParam.builder()
                .serviceName(serviceName)
                .systemType(systemType)
                .hostName(hostName)
                .spanKind(spanKind)
                .build();

        List<ServiceCallView> data = serviceCallMinuteDOMapper
                .queryDbSummaryByServiceName(dateParam,
                        routeInfo,
                        param
                );

        if (CollectionUtils.isEmpty(data)) {
            return Collections.emptyList();
        }

        // Extract all HashKeys and batch query the dictionary
        Map<String, String> hashToTextMap = hashDictionaryService.buildHashToTextMapFromServiceCallView(data);

        List<TransactionInfoResponse> result = new ArrayList<>();
        for (ServiceCallView d : data){
            TransactionInfoResponse r = TransactionInfoResponse.convertFrom(d, hashToTextMap);

            // Get method from the translated ifaceName
            String ifaceName = r.getIfaceName();
            if (SystemTypeEnum.MYSQL.getName().equals(systemType) || SystemTypeEnum.POSTGRESQL.getName().equals(systemType) || SystemTypeEnum.REDIS.getName().equals(systemType)) {
                String sql = Optional.ofNullable(ifaceName).orElse(CommonConstants.EMPTY_STRING);
                String caller = StringUtils.hasText(r.getIfaceCaller()) ? r.getIfaceCaller() : SpanConstants.DEFAULT_DISPLAY_VALUE_WHEN_NO_DATA;
                String format = String.format("%s | %s", caller, sql.split(CommonConstants.SPACE)[0].toUpperCase());
                r.setMethod(format);
            } else {
                r.setMethod(ifaceName);
            }

            result.add(r);
        }

        return result;

    }

    /**
     * Query DB details and time axis
     */
    public List<ServiceDetailInfoResponse> getDbDetail(DateParam dateParam, String serviceName, String machineId, String iface_caller, String ifaceCallerHashKey, String iface_name, String ifaceNameHashKey, Short spanKind, String systemType) {
        ParamCheckUtils.validateDateAndServiceName(dateParam, serviceName);
        dateParam.convertToUTC();
        spanKind = hackSpanKind(systemType, spanKind).getValue();
        ParamCheckUtils.validateSpinKind(spanKind);
        ParamCheckUtils.validateServiceNamePermission(commonService.getCurrentUserServiceNameList(), serviceName);
        RouteInfo routeInfo;
        if (SpanKindEnum.CONSUMER.getValue() == spanKind) {
            routeInfo = tableRoutingService.getTableName(dateParam, DynamicTablePlaceholder.TAG_ENTRY);
        } else {
            routeInfo = tableRoutingService.getTableName(dateParam, DynamicTablePlaceholder.TAG_DEPENDENCY);
        }

        ServiceDataParam param = ServiceDataParam.builder()
                .strInterval(DateUtils.getDefaultTimeBucket())
                .serviceName(serviceName)
                .callerTargetName(iface_caller)
                .callerTargetNameHashKey(ifaceCallerHashKey)
                .targetName(iface_name)
                .targetNameHashKey(ifaceNameHashKey)
                .systemType(systemType)
                .spanKind(spanKind)
                .hostName(machineId)
                .build();

        //Query
        List<ServiceCallView> data = serviceCallMinuteDOMapper
                .queryDbDetailByServiceName(
                        dateParam,
                        routeInfo,
                        param
                );

        if (CollectionUtils.isEmpty(data)) {
            return Collections.emptyList();
        }

        data = FillDataHoleUtil.fillServiceMinuteCallDataHoleInDatetimeRange(dateParam, data, routeInfo);

        //Format
        return data.stream()
                .map(ServiceDetailInfoResponse::ConvertFrom)
                .collect(Collectors.toList());
    }

    private List<IfaceGatherInfoResponse> addHeaderAndFooter(List<IfaceGatherInfoResponse> list, BigDecimal totalInnerCallsTime, GatherQueryParam gatherQueryParam) {
        IfaceGatherInfoResponse footer = new IfaceGatherInfoResponse();
        footer.setSystemType("-");
        footer.setIfaceName("OTHER");
        footer.setRedMetric(RedMetricResponse.builder().totalCount(-1L).build());

        // If ifaceTotalTime is null or 0, avoid division by zero
        if (gatherQueryParam.getIfaceTotalTime() == null || gatherQueryParam.getIfaceTotalTime().compareTo(BigDecimal.ZERO) == 0) {
            footer.setPercent(BigDecimal.ZERO);
            footer.setTotalTime(BigDecimal.ZERO);
        } else {
            // Core calculation logic: use the passed totalInnerCallsTime
            BigDecimal other_inner_totalTime = gatherQueryParam.getIfaceTotalTime().subtract(totalInnerCallsTime);
            footer.setPercent(other_inner_totalTime.divide(gatherQueryParam.getIfaceTotalTime(), 4, RoundingMode.FLOOR));
            footer.setTotalTime(UnitConvertUtils.convertMicroSecondsToSeconds_BigDecimal(other_inner_totalTime));
        }

        list.add(footer);
        return list;
    }

    private List<IfaceGatherInfoResponse> convertToGatherInfoList(List<ServiceCallView> data, GatherQueryParam gatherQueryParam, Map<String, String> hashToTextMap) {
        List<IfaceGatherInfoResponse> list = new ArrayList<>();
        for (ServiceCallView one_data : data) {
            IfaceGatherInfoResponse ifaceGatherInfoResponse = IfaceGatherInfoResponse.convertFrom(one_data);
            String iface_name_hash = GroupBy.GROUP_BY_KEY_ACTION.equals(gatherQueryParam.getGroupByKey()) ? one_data.getActionName() : one_data.getTargetName();
            // Translate: restore HashKey to original text
            String iface_name = hashToTextMap.getOrDefault(iface_name_hash, iface_name_hash);
            ifaceGatherInfoResponse.setIfaceName(iface_name);
            //Calculate total duration
            BigDecimal total_time_micro = one_data.getAvgDuration().multiply(BigDecimal.valueOf(one_data.getTotalCount()));
            ifaceGatherInfoResponse.setTotalTime(UnitConvertUtils.convertMicroSecondsToSeconds_BigDecimal(total_time_micro));

            // 0.1111 => 11.11%
            ifaceGatherInfoResponse.setPercent(total_time_micro.divide(gatherQueryParam.getIfaceTotalTime(), 4, RoundingMode.FLOOR));
            list.add(ifaceGatherInfoResponse);
        }
        return list;
    }

    private SpanKindEnum hackSpanKind(String systemType, Short spanKind){
        if (!SystemTypeEnum.KAFKA.getName().equals(systemType) && !SystemTypeEnum.ROCKETMQ.getName().equals(systemType)) {
            return SpanKindEnum.CLIENT;
        }
        return SpanKindEnum.getEnumByValue(spanKind);
    }

}
