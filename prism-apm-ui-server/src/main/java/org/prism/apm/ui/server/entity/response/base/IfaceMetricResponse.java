package org.prism.apm.ui.server.entity.response.base;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class IfaceMetricResponse {

    private BigDecimal maxDuration;
    private BigDecimal minDuration;
    private BigDecimal p90;
    private BigDecimal p95;
    private BigDecimal p99;

}
