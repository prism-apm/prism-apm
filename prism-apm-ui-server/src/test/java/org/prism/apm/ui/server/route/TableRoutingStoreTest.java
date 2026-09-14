package org.prism.apm.ui.server.route;

import org.prism.apm.ui.server.mapper.route.TableRoutingRuleMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TableRoutingStoreTest {

    @Mock
    private TableRoutingRuleMapper tableRoutingRuleMapper;

    @InjectMocks
    private TableRoutingStore tableRoutingStore;

    /**
     * Test scenario: normal loading
     * 1. Rules should be input out of order to test whether sorting works correctly.
     * 2. Includes min=null (0) and max=null (MAX_VALUE).
     * 3. Expected: no exception, and the rules list is sorted chronologically.
     */
    @Test
    @DisplayName("Normal loading: should sort correctly and verify continuous ranges")
    void testInitRules_Success_Sorted() {
        // Build out-of-order data
        // Rule 3: [21, infinity]
        TableRoutingRules r3 = createRule(21L, null, "table_C");
        // Rule 1: [0, 10] (min null -> 0)
        TableRoutingRules r1 = createRule(null, 10L, "table_A");
        // Rule 2: [11, 20]
        TableRoutingRules r2 = createRule(11L, 20L, "table_B");

        List<TableRoutingRules> mockList = new ArrayList<>(Arrays.asList(r3, r1, r2));

        when(tableRoutingRuleMapper.selectList(null)).thenReturn(mockList);

        // Execute
        tableRoutingStore.initRules();

        // Assert
        List<TableRoutingRules> sortedRules = tableRoutingStore.getRules();
        assertNotNull(sortedRules);
        assertEquals(3, sortedRules.size());

        // Verify order
        assertEquals("table_A", sortedRules.get(0).getTargetTable()); // 0 - 10
        assertEquals("table_B", sortedRules.get(1).getTargetTable()); // 11 - 20
        assertEquals("table_C", sortedRules.get(2).getTargetTable()); // 21 - MAX
    }

    /**
     * Test scenario: database returns empty or null
     * Expected: only logs are printed, no exception is thrown, and rules are empty or null.
     */
    @Test
    @DisplayName("Boundary case: should handle missing rules gracefully")
    void testInitRules_Empty() {
        when(tableRoutingRuleMapper.selectList(null)).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> tableRoutingStore.initRules());
    }

    /**
     * Test scenario: gap detection - middle gap
     * [0, 10] followed by [12, 20]; 11 is missing.
     */
    @Test
    @DisplayName("Exception check: middle gap should throw an exception")
    void testCheckRules_Gap_In_Middle() {
        List<TableRoutingRules> list = Arrays.asList(
                createRule(0L, 10L, "t1"),
                createRule(12L, 20L, "t2") // Should be 11; 12 creates a gap
        );
        when(tableRoutingRuleMapper.selectList(null)).thenReturn(list);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> tableRoutingStore.initRules());
        assertTrue(ex.getMessage().contains("gap"), "Exception message should contain gap");
        assertTrue(ex.getMessage().contains("11"), "Exception message should indicate the missing time point");
    }

    /**
     * Test scenario: gap detection - start is not 0
     * The first rule starts at 5.
     */
    @Test
    @DisplayName("Exception check: non-zero start time should throw an exception")
    void testCheckRules_Gap_At_Start() {
        List<TableRoutingRules> list = Arrays.asList(
                createRule(5L, 10L, "t1") // Previous end defaults to -1; here min=5 and 5 > -1 + 1, so a gap exists
        );
        when(tableRoutingRuleMapper.selectList(null)).thenReturn(list);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> tableRoutingStore.initRules());
        assertTrue(ex.getMessage().contains("gap"));
    }

    /**
     * Test scenario: overlap detection
     * [0, 10] followed by [10, 20]; 10 is covered twice.
     */
    @Test
    @DisplayName("Exception check: overlapping ranges should throw an exception")
    void testCheckRules_Overlap() {
        List<TableRoutingRules> list = Arrays.asList(
                createRule(0L, 10L, "t1"),
                createRule(10L, 20L, "t2") // Previous rule ends at 10 and this rule starts at 10. 10 <= 10, so they overlap.
        );
        when(tableRoutingRuleMapper.selectList(null)).thenReturn(list);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> tableRoutingStore.initRules());
        assertTrue(ex.getMessage().contains("overlap"), "Exception message should contain overlap");
        assertTrue(ex.getMessage().contains("TargetTable: t2"));
    }

    /**
     * Test scenario: Min > Max self-validation error
     */
    @Test
    @DisplayName("Exception check: Min > Max should throw an exception")
    void testCheckRules_Self_Invalid() {
        List<TableRoutingRules> list = Arrays.asList(
                createRule(0L, 10L, "t1"),
                createRule(20L, 15L, "t2") // Invalid configuration
        );
        when(tableRoutingRuleMapper.selectList(null)).thenReturn(list);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> tableRoutingStore.initRules());
        assertTrue(ex.getMessage().contains("cannot be greater than"));
    }

    /**
     * Test scenario: infinite rule is not the last rule
     * [0, MAX] appears before [MAX+?, ...] (although unreachable logically, config validation should block it).
     * Or [0, MAX], [100, 200] (after sorting, [0, MAX] is always first, but max=MAX prevents reaching the next rule).
     */
    @Test
    @DisplayName("Exception check: infinite rule that is not last should throw an exception")
    void testCheckRules_Infinite_Not_Last() {
        // In practice, because sorting is by min, [0, null] is always before [100, 200]
        // The first rule then has max=Long.MAX_VALUE and i < size-1
        List<TableRoutingRules> list = Arrays.asList(
                createRule(null, null, "t_infinite"),
                createRule(100L, 200L, "t_later")
        );
        when(tableRoutingRuleMapper.selectList(null)).thenReturn(list);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> tableRoutingStore.initRules());
        assertTrue(ex.getMessage().contains("infinite (max=null) rule"));
        assertTrue(ex.getMessage().contains("not the last rule"));
    }

    // --- Helper Method ---

    private TableRoutingRules createRule(Long min, Long max, String table) {
        TableRoutingRules rule = new TableRoutingRules();
        rule.setMinSeconds(min);
        rule.setMaxSeconds(max);
        rule.setTargetTable(table);
        rule.setTag("tag");
        return rule;
    }
}
