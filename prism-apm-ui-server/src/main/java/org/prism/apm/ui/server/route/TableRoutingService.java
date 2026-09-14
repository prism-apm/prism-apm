package org.prism.apm.ui.server.route;

import org.prism.apm.ui.server.entity.dto.route.RouteInfo;
import org.prism.apm.ui.server.entity.param.common.DateParam;
import org.prism.apm.ui.server.utils.DateUtils;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.function.Supplier;

@Service
public class TableRoutingService {

    // The default provider is LocalDateTime::now
    @Setter
    private Supplier<LocalDateTime> timeProvider = LocalDateTime::now;

    private final TableRoutingStore tableRoutingStore;

    public TableRoutingService(TableRoutingStore tableRoutingStore) {
        this.tableRoutingStore = tableRoutingStore;
    }

    public RouteInfo getTableName(String startDate, String endDate, String tag) {

        LocalDateTime queryStart = DateUtils.parseDateTimeString(startDate);
        LocalDateTime queryEnd = DateUtils.parseDateTimeString(endDate);

        long durationSeconds = Duration.between(queryStart, queryEnd).getSeconds();

        // 1. First filter rules whose time span matches
        TableRoutingRules matchedRule = tableRoutingStore.getRules().stream()
                .filter(rule -> tag.equals(rule.getTag()))
                // 1. First filter rules whose time span matches
                .filter(rule -> rule.matchesDuration(durationSeconds))
                .min(Comparator.comparing(TableRoutingRules::getMinSeconds))
                .orElse(null);

        if (matchedRule != null) {
            RouteInfo.RouteInfoBuilder routeInfoBuilder = RouteInfo.builder()
                    .dataStep(matchedRule.getDataStep());

            // Type 1: simple match
            if (TableRoutingRules.RuleType.SIMPLE.name().equals(matchedRule.getRuleType())) {
                routeInfoBuilder.dynamicTableName(matchedRule.getTargetTable());
            }

            // Type 2: check chunk progress
            else if (TableRoutingRules.RuleType.COMPLEX.name().equals(matchedRule.getRuleType())) {
                // Calculate the chunk safety cutoff time
                LocalDateTime safeChunkBoundary = timeProvider.get().toLocalDate()
                        .atStartOfDay()
                        .minusDays(matchedRule.getChunkLagDays());
                // If query end time <= safety boundary, the data is already in the optimized chunk
                if (queryEnd.isBefore(safeChunkBoundary) || queryEnd.isEqual(safeChunkBoundary)) {
                    routeInfoBuilder.dynamicTableName(matchedRule.getOptimizedTable());
                } else {
                    routeInfoBuilder.dynamicTableName(matchedRule.getTargetTable());
                }
            }

            return routeInfoBuilder.build();
        }

        throw new RuntimeException("No matching table routing rule found.");
    }

    public RouteInfo getTableName(DateParam dateParam, String tag) {
        return getTableName(dateParam.getStartDate(), dateParam.getEndDate(), tag);
    }

}