package org.prism.apm.ui.server.service.service;

import org.prism.apm.ui.server.constant.route.DynamicTablePlaceholder;
import org.prism.apm.ui.server.entity.db.query.ServiceDataParam;
import org.prism.apm.ui.server.entity.dto.route.RouteInfo;
import org.prism.apm.ui.server.entity.model.service.ServiceCallView;
import org.prism.apm.ui.server.entity.param.common.DateParam;
import org.prism.apm.ui.server.entity.response.base.RedMetricResponse;
import org.prism.apm.ui.server.entity.response.service.ServiceOverviewResponse;
import org.prism.apm.ui.server.mapper.service.ServiceCallMinuteDOMapper;
import org.prism.apm.ui.server.route.TableRoutingService;
import org.prism.apm.ui.server.utils.DateUtils;
import org.prism.apm.ui.server.utils.ParamCheckUtils;
import org.prism.apm.ui.server.utils.UnitConvertUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class OverViewService {

    private final CommonService commonService;
    private final ServiceCallMinuteDOMapper serviceCallMinuteDOMapper;
    private final TableRoutingService tableRoutingService;

    public OverViewService(CommonService commonService, ServiceCallMinuteDOMapper serviceCallMinuteDOMapper, TableRoutingService tableRoutingService) {
        this.commonService = commonService;
        this.serviceCallMinuteDOMapper = serviceCallMinuteDOMapper;
        this.tableRoutingService = tableRoutingService;
    }

    public ServiceOverviewResponse getServiceOverview(String serviceName, DateParam dateParam) {
        ParamCheckUtils.validateDateAndServiceName(dateParam, serviceName);
        dateParam.convertToUTC();
        ParamCheckUtils.validateServiceNamePermission(commonService.getCurrentUserServiceNameList(), serviceName);
        RouteInfo routeInfo = tableRoutingService.getTableName(dateParam, DynamicTablePlaceholder.TAG_ENTRY);

        // --- Dynamically calculate the time span in minutes, matching minuteDuration in JS ---
        double minuteDuration = DateUtils.getTimeBetweenInMinutes(dateParam.getStartDate(), dateParam.getEndDate());

        // If the time range is invalid, such as equal start and end times or start after end, return an empty list directly
        if (minuteDuration <= 0) {
            return new ServiceOverviewResponse();
        }

        ServiceDataParam param = ServiceDataParam.builder()
                .serviceName(serviceName)
                .build();

        // Query service statistics
        ServiceCallView data = serviceCallMinuteDOMapper.queryServiceSummaryByServiceName(dateParam, routeInfo, param);

        // --- Generate response data and calculate metrics during the process ---
        return this.generateOverviewResponse(data, minuteDuration);
    }

    /**
     * Convert the DTO list to an ApplicationInfo list and calculate derived metrics
     *
     * @param data           Raw statistics queried from the database
     * @param minuteDuration Calculated time span in minutes
     * @return Fully calculated ApplicationInfo list
     */
    private ServiceOverviewResponse generateOverviewResponse(ServiceCallView data, double minuteDuration) {
        // Data format conversion
        ServiceOverviewResponse applicationInfo = new ServiceOverviewResponse();

        if (data == null){
            applicationInfo.setRedMetric(RedMetricResponse.builder()
                    .avgDuration(BigDecimal.ZERO)
                    .build());
            applicationInfo.setRpm(BigDecimal.ZERO);
            applicationInfo.setErrRate(BigDecimal.ZERO);
            return applicationInfo;
        }

        // Step A: map base data from DTO
        applicationInfo.setServiceName(data.getServiceName());
        applicationInfo.setRedMetric(
                RedMetricResponse.builder()
                        .avgDuration(UnitConvertUtils.convertMicroSecondsToMilliseconds_BigDecimal(data.getAvgDuration()))
                        .totalCount(data.getTotalCount())
                        .totalError(data.getTotalError())
                        .build()
        );

        // Step B: call utility methods to calculate RPM and errRate and convert units
        calculateMetrics(applicationInfo, minuteDuration);

        return applicationInfo;
    }

    /**
     * Calculate and fill derived metrics on the ApplicationInfo object, such as RPM, avgDuration, and errRate.
     * This method mutates the passed applicationInfo object directly, matching the original JavaScript behavior.
     *
     * @param minuteDuration Statistics time span, in minutes.
     */
    private static void calculateMetrics(ServiceOverviewResponse response, double minuteDuration) {
        if (response == null) {
            return; // If the passed object is null, do nothing
        }

        response.setRpm(BigDecimal.ZERO);
        response.setErrRate(BigDecimal.ZERO);

        RedMetricResponse info = response.getRedMetric();

        // --- 1. Calculate RPM (Requests Per Minute) ---
        // Calculate when the total is not null and the minute count is greater than 0
        if (minuteDuration > 0) {
            BigDecimal totalCountBd = BigDecimal.valueOf(info.getTotalCount());
            BigDecimal minuteDurationBd = BigDecimal.valueOf(minuteDuration);
            // Use BigDecimal for exact division, keep 2 decimal places, and round half up
            BigDecimal rpm = totalCountBd.divide(minuteDurationBd, 2, RoundingMode.HALF_UP);
            response.setRpm(rpm);
        }

        // --- 2. Calculate error rate (%) ---
        if (info.getTotalCount() > 0) {
            BigDecimal errorCountBd = BigDecimal.valueOf(info.getTotalError());
            BigDecimal totalCountBd = BigDecimal.valueOf(info.getTotalCount());

            // Calculate (error count / total count) * 100 and keep 2 decimal places
            // Use higher precision during division, such as 4 digits, to make the multiplied result more accurate
            BigDecimal errRate = errorCountBd.divide(totalCountBd, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
            response.setErrRate(errRate);
        }
    }
}
