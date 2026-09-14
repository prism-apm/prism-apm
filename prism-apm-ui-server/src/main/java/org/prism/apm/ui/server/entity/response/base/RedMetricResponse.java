package org.prism.apm.ui.server.entity.response.base;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RedMetricResponse {

    private BigDecimal avgDuration;
    private Long totalCount;
    private Long totalError;

}
