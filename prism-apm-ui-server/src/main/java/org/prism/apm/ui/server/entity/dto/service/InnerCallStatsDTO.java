package org.prism.apm.ui.server.entity.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Internal call statistics DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InnerCallStatsDTO {
    
    /**
     * Total row count after grouping (used for pagination)
     */
    private Long totalRows;
    
    /**
     * Total duration of all internal calls, SUM(avg_duration * cnt)
     */
    private BigDecimal totalDurationSum;
}
