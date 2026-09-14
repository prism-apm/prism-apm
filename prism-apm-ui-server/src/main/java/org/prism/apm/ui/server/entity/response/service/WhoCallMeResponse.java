package org.prism.apm.ui.server.entity.response.service;

import org.prism.apm.ui.server.entity.model.service.ServiceCallView;
import org.prism.apm.ui.server.entity.response.base.RedMetricResponse;
import org.prism.apm.ui.server.utils.UnitConvertUtils;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class WhoCallMeResponse {

    /**
     * Caller name
     */
    private String callerServiceName;
    /**
     * Caller endpoint
     */
    private String ifaceCaller;

    /**
     * Total endpoint duration
     */
    private BigDecimal totalTime;

    private RedMetricResponse redMetric;

    /**
     * Convert from ServiceCallView and translate names using the dictionary mapping
     */
    public static WhoCallMeResponse convertFrom(ServiceCallView serviceCallView, Map<String, String> hashToTextMap){
        WhoCallMeResponse response = convertFrom(serviceCallView);
        if (hashToTextMap != null) {
            String callerText = hashToTextMap.get(serviceCallView.getCallerTargetName());
            if (callerText != null) {
                response.setIfaceCaller(callerText);
            }
        }
        return response;
    }

    private static WhoCallMeResponse convertFrom(ServiceCallView serviceCallView){
        WhoCallMeResponse response = new WhoCallMeResponse();
        response.setCallerServiceName(serviceCallView.getCallerServiceName());
        response.setIfaceCaller(serviceCallView.getCallerTargetName());
        response.setTotalTime(UnitConvertUtils.convertMicroSecondsToSeconds_BigDecimal(serviceCallView.getTotalTime()));

        response.setRedMetric(
                RedMetricResponse.builder()
                        .avgDuration(UnitConvertUtils.convertMicroSecondsToMilliseconds_BigDecimal(serviceCallView.getAvgDuration()))
                        .totalCount(serviceCallView.getTotalCount())
                        .totalError(serviceCallView.getTotalError())
                        .build());
        return response;
    }

}
