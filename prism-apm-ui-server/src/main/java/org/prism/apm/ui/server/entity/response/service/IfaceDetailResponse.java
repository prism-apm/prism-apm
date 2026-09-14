package org.prism.apm.ui.server.entity.response.service;

import org.prism.apm.ui.server.entity.model.service.ServiceCallView;
import org.prism.apm.ui.server.entity.response.base.RedMetricResponse;
import org.prism.apm.ui.server.utils.DateUtils;
import org.prism.apm.ui.server.utils.UnitConvertUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class IfaceDetailResponse {
    private String time;
    private RedMetricResponse redMetric;

    public static IfaceDetailResponse convertFrom(ServiceCallView serviceCallView) {
        IfaceDetailResponse ifaceDetailResponse = new IfaceDetailResponse();
        ifaceDetailResponse.setTime(DateUtils.formatDateTime(serviceCallView.getTimeb()));

        ifaceDetailResponse.setRedMetric(
                RedMetricResponse.builder()
                        .avgDuration(UnitConvertUtils.convertMicroSecondsToMilliseconds_BigDecimal(serviceCallView.getAvgDuration()))
                        .totalCount(serviceCallView.getTotalCount())
                        .totalError(serviceCallView.getTotalError())
                        .build());
        return ifaceDetailResponse;
    }
}
