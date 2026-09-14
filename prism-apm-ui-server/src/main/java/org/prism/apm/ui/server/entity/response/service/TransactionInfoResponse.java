package org.prism.apm.ui.server.entity.response.service;

import org.prism.apm.ui.server.entity.model.service.ServiceCallView;
import org.prism.apm.ui.server.entity.response.base.IfaceMetricResponse;
import org.prism.apm.ui.server.entity.response.base.RedMetricResponse;
import org.prism.apm.ui.server.utils.UnitConvertUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class TransactionInfoResponse {
    private String ifaceName;
    private String ifaceCaller;
    private String ifaceNameHashKey;
    private String ifaceCallerHashKey;

    private RedMetricResponse redMetric;
    private IfaceMetricResponse ifaceMetric;
    //================================
    private String uniqueId;
    private String method;

    /**
     * Convert from ServiceCallView and translate names using the dictionary mapping
     *
     * @param serviceCallView Database query result
     * @param hashToTextMap   HashKey -> original text mapping
     */
    public static TransactionInfoResponse convertFrom(ServiceCallView serviceCallView, Map<String, String> hashToTextMap){
        TransactionInfoResponse response = convertFrom(serviceCallView);
        // Translate names
        if (hashToTextMap != null) {
            String targetText = hashToTextMap.get(serviceCallView.getTargetName());
            if (targetText != null) {
                response.setIfaceName(targetText);
            }
            String callerText = hashToTextMap.get(serviceCallView.getCallerTargetName());
            if (callerText != null) {
                response.setIfaceCaller(callerText);
            }
        }
        return response;
    }

    /**
     * Convert from ServiceCallView without translating names
     * The targetName and callerTargetName fields store UUID HashKeys
     */
    private static TransactionInfoResponse convertFrom(ServiceCallView serviceCallView){
        TransactionInfoResponse transactionInfoResponse = new TransactionInfoResponse();
        // The database stores UUID here; temporarily assign it to the display field and translate it later in the Service layer
        transactionInfoResponse.setIfaceName(serviceCallView.getTargetName());
        transactionInfoResponse.setIfaceCaller(serviceCallView.getCallerTargetName());
        // Also keep the HashKey
        transactionInfoResponse.setIfaceNameHashKey(serviceCallView.getTargetName());
        transactionInfoResponse.setIfaceCallerHashKey(serviceCallView.getCallerTargetName());
        transactionInfoResponse.setUniqueId(transactionInfoResponse.getIfaceCallerHashKey() + transactionInfoResponse.getIfaceNameHashKey());

        transactionInfoResponse.setRedMetric(
                RedMetricResponse.builder()
                        .avgDuration(UnitConvertUtils.convertMicroSecondsToMilliseconds_BigDecimal(serviceCallView.getAvgDuration()))
                        .totalCount(serviceCallView.getTotalCount())
                        .totalError(serviceCallView.getTotalError())
                        .build()
        );

        if (serviceCallView.getMax() != null) {
            transactionInfoResponse.setIfaceMetric(
                    IfaceMetricResponse.builder()
                            .maxDuration(UnitConvertUtils.convertMicroSecondsToMilliseconds_BigDecimal(serviceCallView.getMax()))
                            .minDuration(UnitConvertUtils.convertMicroSecondsToMilliseconds_BigDecimal(serviceCallView.getMin()))
                            .p99(UnitConvertUtils.convertMicroSecondsToMilliseconds_BigDecimal(serviceCallView.getP99()))
                            .p95(UnitConvertUtils.convertMicroSecondsToMilliseconds_BigDecimal(serviceCallView.getP95()))
                            .p90(UnitConvertUtils.convertMicroSecondsToMilliseconds_BigDecimal(serviceCallView.getP90()))
                            .build()
            );
        }

        return transactionInfoResponse;
    }
}
