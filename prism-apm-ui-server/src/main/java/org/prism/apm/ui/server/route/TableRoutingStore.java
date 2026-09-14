package org.prism.apm.ui.server.route;

import org.prism.apm.ui.server.mapper.route.TableRoutingRuleMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@Data
public class TableRoutingStore {

    private List<TableRoutingRules> rules;

    private final TableRoutingRuleMapper tableRoutingRuleMapper;

    public TableRoutingStore(TableRoutingRuleMapper tableRoutingRuleMapper) {
        this.tableRoutingRuleMapper = tableRoutingRuleMapper;
    }

    @PostConstruct
    public void initRules() {
        rules = tableRoutingRuleMapper.selectList(null);

        if (rules == null || rules.isEmpty()) {
            log.warn("No table routing rules found.");
            return;
        }

        // 1. Sort by minSeconds ascending and treat null as 0
        rules.sort(Comparator.comparingLong(r ->
                r.getMinSeconds() == null ? 0L : r.getMinSeconds()
        ));

        rules.stream().collect(Collectors.groupingBy(TableRoutingRules::getTag))
                .forEach((tag, rule) -> {
                    // 2. Check rule validity
                    checkRules(rule);
                });

        log.info("Table routing rules loaded and validated successfully. Total rules: {}", rules.size());
    }

    /**
     * Check whether rules contain overlaps or gaps
     * Also check whether an infinite rule appears before the end
     */
    private void checkRules(List<TableRoutingRules> rules) {
        // Record the previous rule end time, initialized to -1, meaning the first expected rule starts from 0
        // If the business allows rules not to start from 0, change this to Long.MIN_VALUE or adjust it as needed
        long prevMax = -1;

        for (int i = 0; i < rules.size(); i++) {
            TableRoutingRules rule = rules.get(i);

            // Handle null values: min null -> 0, max null -> infinity
            long curMin = rule.getMinSeconds() == null ? 0L : rule.getMinSeconds();
            long curMax = rule.getMaxSeconds() == null ? Long.MAX_VALUE : rule.getMaxSeconds();

            // 0. Self-validity check
            if (curMin > curMax) {
                throw new IllegalStateException(String.format(
                        "Routing rule configuration error: minimum time (%d) cannot be greater than maximum time (%d). TargetTable: %s",
                        curMin, curMax, rule.getTargetTable()));
            }

            // 1. Gap detection
            // Expected current rule start = previous rule end + 1
            // For example: if the previous rule is [0, 10], the current rule must be [11, ...]
            if (curMin > prevMax + 1) {
                throw new IllegalStateException(String.format(
                        "Routing rule gap: range [%d, %d] is not covered by any rule. Previous rule ended at %d, current rule starts at %d.",
                        prevMax + 1, curMin - 1, prevMax, curMin));
            }

            // 2. Overlap detection
            // Because rules are sorted by min, curMin <= prevMax means the current rule starts within the previous rule range
            // For example: previous rule [0, 10], current rule [10, 20]; 10 is covered twice
            if (curMin <= prevMax) {
                throw new IllegalStateException(String.format(
                        "Routing rule overlap: time point %d is covered by multiple rules. Previous rule ended at %d, current rule starts at %d. TargetTable: %s",
                        curMin, prevMax, curMin, rule.getTargetTable()));
            }

            // 3. Check the position of the infinite rule
            // If the current rule is infinite (max is null/MAX_VALUE), it must be the last rule
            if (curMax == Long.MAX_VALUE && i < rules.size() - 1) {
                throw new IllegalStateException(String.format(
                        "Routing rule configuration error: found an infinite (max=null) rule [%s], but it is not the last rule. Later rules can never be matched.",
                        rule.getTargetTable()));
            }

            // Update cursor
            prevMax = curMax;
        }
    }

}
