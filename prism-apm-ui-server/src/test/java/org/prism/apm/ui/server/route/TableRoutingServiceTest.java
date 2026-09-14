package org.prism.apm.ui.server.route;

import org.prism.apm.ui.server.entity.dto.route.RouteInfo;
import org.prism.apm.ui.server.utils.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableRoutingServiceTest {

    private TableRoutingService routerService;
    private final List<TableRoutingRules> standardRules = new ArrayList<>();

    @BeforeEach
    void setUp() {

        long oneDay = 86400L;

        // --- Build the standard test rule set ---

        // Rule 1: 0 to 1 day -> use Chunk check (1min vs 1min_1day)
        // Assume Chunk lags by 2 days
        int LAG_DAYS = 2;
        standardRules.add(new TableRoutingRules(0L, oneDay,
                TableRoutingRules.RuleType.COMPLEX.name(), "service_call_1min", "service_call_1min_1day", LAG_DAYS, "all", 1));

        // Rule 2: 1 to 3 days -> 5min table
        standardRules.add(new TableRoutingRules(oneDay, oneDay * 3,
                TableRoutingRules.RuleType.SIMPLE.name(), "service_call_5min", null, LAG_DAYS, "all", 5));

        // Rule 3: > 60 days -> 1day table
        standardRules.add(new TableRoutingRules(oneDay * 60, null,
                TableRoutingRules.RuleType.SIMPLE.name(), "service_call_1day", null, LAG_DAYS, "all", 1440));

        TableRoutingStore store = new TableRoutingStore(null);
        store.setRules(standardRules);
        this.routerService = new TableRoutingService(store);
    }

    @Test
    @DisplayName("Test: Simple rule - medium range hits the 5min table")
    void testSimpleRuleMatch() {
        // Range is 2 days (172800 seconds), falling in the [1 day, 3 days) interval
        LocalDateTime start = LocalDateTime.of(2025, 5, 1, 0, 0, 0);
        LocalDateTime end = start.plusDays(2);

        RouteInfo result = routerService.getTableName(DateUtils.formatDateTime(start), DateUtils.formatDateTime(end), "all");

        assertEquals("service_call_5min", result.getDynamicTableName());
    }

    @Test
    @DisplayName("Test: Simple rule - very large range hits the 1day table (infinity test)")
    void testInfiniteRuleMatch() {
        // Range is 100 days, falling in the [60 days, null) interval
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        LocalDateTime end = start.plusDays(100);

        RouteInfo result = routerService.getTableName(DateUtils.formatDateTime(start), DateUtils.formatDateTime(end), "all");

        assertEquals("service_call_1day", result.getDynamicTableName());
    }

    @Test
    @DisplayName("Test: CheckChunk - historical data should hit the optimized table")
    void testCheckChunkOptimized() {
        // Current time is 2026-02-10; Lag=2 -> before 2026-02-08 00:00 is safe
        // Query: Feb 6 00:00 to Feb 7 00:00 (range 1 day < 1-day rule interval)
        // End time (Feb 7) is earlier than the safe boundary (Feb 8)
        LocalDateTime now = LocalDateTime.of(2026, 2, 10, 18, 0, 0);
        routerService.setTimeProvider(()-> now);

        LocalDateTime start = LocalDateTime.of(2026, 2, 6, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 2, 7, 0, 0, 0);

        RouteInfo result = routerService.getTableName(DateUtils.formatDateTime(start), DateUtils.formatDateTime(end), "all");

        assertEquals("service_call_1min_1day", result.getDynamicTableName());
    }

    @Test
    @DisplayName("Test: CheckChunk - latest data should fall back to the raw table")
    void testCheckChunkFallback() {
        // Query: May 8 10:00 to May 8 12:00 (range 2 hours < 1 day)
        // End time (May 8 12:00) is later than the safe boundary (May 8 00:00)
        LocalDateTime start = LocalDateTime.of(2025, 5, 8, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 5, 8, 12, 0, 0);

        LocalDateTime now = LocalDateTime.of(2025, 5, 10, 3, 0, 0);
        routerService.setTimeProvider(()-> now);
        RouteInfo result = routerService.getTableName(DateUtils.formatDateTime(start), DateUtils.formatDateTime(end), "all");

        assertEquals("service_call_1min", result.getDynamicTableName(), "Because the query includes unsafe data, it should use the realtime table");
    }

    @Test
    @DisplayName("Test: Boundary value - query end time is exactly 00:00 on the 8th and should hit the optimized table")
    void testCriticalBoundaryFallback() {
        // Scenario:
        // Now = 10th 03:00
        // Lag = 2 -> boundary = 8th 00:00
        // Current state: the 7th chunk is ready (Safe), while the 8th chunk is not ready (Unsafe)
        // Because the query uses .<. for the end time, the boundary point is not included

        LocalDateTime now = LocalDateTime.of(2025, 5, 10, 3, 0, 0);
        routerService.setTimeProvider(()-> now);

        // Query: 7th 23:00 to 8th 00:00 (exactly reaches the boundary)
        LocalDateTime start = LocalDateTime.of(2025, 5, 7, 23, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 5, 8, 0, 0, 0);

        // Execute routing
        RouteInfo result = routerService.getTableName(DateUtils.formatDateTime(start), DateUtils.formatDateTime(end), "all");

        // --- Core assertion ---
        assertEquals("service_call_1min_1day", result.getDynamicTableName(),
                "When the end time equals the start time of an ungenerated Chunk, it should hit the optimized table");
    }

    @Test
    @DisplayName("Test: default fallback when no rule matches")
    void testFallback() {
        // Build an empty rule list
        List<TableRoutingRules> emptyRules = new ArrayList<>();

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(1);

        RouteInfo result = routerService.getTableName(DateUtils.formatDateTime(start), DateUtils.formatDateTime(end), "all");

        assertEquals("service_call_1min", result.getDynamicTableName());
    }
}
