package org.prism.apm.ui.server.entity.response.service;

import org.prism.apm.ui.server.entity.response.base.RedMetricResponse;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceOverviewResponse {

    private String serviceName;
    private RedMetricResponse redMetric;

    //calculated fields
    private BigDecimal rpm;
    private BigDecimal errRate;

}
