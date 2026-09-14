package org.prism.apm.ui.server.entity.response.service;

import org.prism.apm.ui.server.entity.model.service.ServiceCallView;
import org.prism.apm.ui.server.entity.response.base.RedMetricResponse;
import org.prism.apm.ui.server.utils.DateUtils;
import org.prism.apm.ui.server.utils.UnitConvertUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ServiceDetailInfoResponse {
    private String time;
    private RedMetricResponse redMetric;

    public static ServiceDetailInfoResponse ConvertFrom(ServiceCallView serviceCallView) {
        ServiceDetailInfoResponse serviceDetailInfoResponse = new ServiceDetailInfoResponse();
        serviceDetailInfoResponse.setTime(DateUtils.formatDateTime(serviceCallView.getTimeb()));

        serviceDetailInfoResponse.setRedMetric(
                RedMetricResponse.builder()
                        .avgDuration(UnitConvertUtils.convertMicroSecondsToMilliseconds_BigDecimal(serviceCallView.getAvgDuration()))
                        .totalCount(serviceCallView.getTotalCount())
                        .totalError(serviceCallView.getTotalError())
                        .build()
        );
        return serviceDetailInfoResponse;
    }
}
