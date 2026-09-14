package org.prism.apm.ui.server.entity.response.service;

import org.prism.apm.ui.server.entity.model.service.ServiceCallView;
import org.prism.apm.ui.server.entity.response.base.RedMetricResponse;
import org.prism.apm.ui.server.utils.UnitConvertUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
public class IfaceGatherInfoResponse {
    private String ifaceName;
    private String systemType;
    private BigDecimal totalTime;
    private BigDecimal percent;

    private RedMetricResponse redMetric;

    public static IfaceGatherInfoResponse convertFrom(ServiceCallView serviceCallView){

        IfaceGatherInfoResponse ifaceGatherInfoResponse = new IfaceGatherInfoResponse();
        ifaceGatherInfoResponse.setSystemType(serviceCallView.getSystemType());
        // The database stores UUID in targetName; temporarily assign it to the display field
        ifaceGatherInfoResponse.setIfaceName(serviceCallView.getTargetName());
        //Calculate total duration
        BigDecimal total_time_micro = serviceCallView.getAvgDuration().multiply(BigDecimal.valueOf(serviceCallView.getTotalCount()));
        ifaceGatherInfoResponse.setTotalTime(UnitConvertUtils.convertMicroSecondsToSeconds_BigDecimal(total_time_micro));

        // 0.1111 => 11.11%

        ifaceGatherInfoResponse.setRedMetric(
                RedMetricResponse.builder()
                        .avgDuration(UnitConvertUtils.convertMicroSecondsToMilliseconds_BigDecimal(serviceCallView.getAvgDuration()))
                        .totalCount(serviceCallView.getTotalCount())
                        .totalError(serviceCallView.getTotalError())
                        .build());

        return ifaceGatherInfoResponse;
    }

}
