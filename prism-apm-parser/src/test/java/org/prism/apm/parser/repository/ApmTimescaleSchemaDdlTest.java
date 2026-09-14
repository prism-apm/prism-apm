package org.prism.apm.parser.repository;

import org.prism.apm.parser.MybatisTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DDL-only TimescaleDB metadata checks for the service_call schema family.
 *
 * These tests validate schema objects created by the active test DDL. They intentionally avoid inserting rows,
 * refreshing continuous aggregates, checking aggregate values, or duplicating mapper CRUD tests.
 *
 * Task map:
 * 1. Smoke-check service_call as a hypertable and the two base minute CAGGs.
 * 2. Verify required TimescaleDB extensions.
 * 3. Verify service_call table columns, data types, and primary key.
 * 4. Verify service_call hypertable time dimension settings.
 * 5. Verify base CAGGs and their source hypertable.
 * 6. Verify rollup CAGG inventory.
 * 7. Verify CAGG dependency shape from view definitions.
 * 8. Verify refresh, retention, and columnstore policy jobs.
 * 9. Verify important query indexes.
 * 10. Verify public unified query views.
 * 11. Keep shared DDL metadata assertions in private helpers below the test cases.
 *
 *
 * todo:
     - task 8 lacks the test cases for specific CAGG, retention and compression settings.
     - task 9 only check whether the indexes exist, and don't check the index columns are as expected
 */
class ApmTimescaleSchemaDdlTest extends MybatisTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /*
     * Task 2: Verify required extensions.
     *
     * The service_call DDL depends on TimescaleDB metadata objects and toolkit aggregate functions.
     * This test checks pg_extension directly so a missing CREATE EXTENSION fails before lower-level
     * table, hypertable, or CAGG assertions become noisy.
     */
    @Test
    void ddlInstallsRequiredTimescaleExtensions() {
        assertExtensionsInstalled(Set.of("timescaledb", "timescaledb_toolkit"));
    }

    /*
     * Task 3: Verify service_call table DDL - table existence and all column types.
     *
     * This is a pure DDL contract check for the base table consumed by ServiceCallMapper. It verifies
     * every column currently declared by the schema, including caller-side fields and metric counters,
     * without testing insert/upsert behavior that belongs in mapper tests.
     */
    @Test
    void ddlCreatesServiceCallTableWithExpectedColumnTypes() {
        Map<String, String> expectedColumnTypes = Map.ofEntries(
                Map.entry("time", "timestamp with time zone"),
                Map.entry("trace_id", "text"),
                Map.entry("span_id", "text"),
                Map.entry("service_name", "text"),
                Map.entry("host_name", "text"),
                Map.entry("action_name", "text"),
                Map.entry("target_name", "uuid"),
                Map.entry("remote_addr", "text"),
                Map.entry("system_type", "text"),
                Map.entry("component", "text"),
                Map.entry("span_kind", "smallint"),
                Map.entry("caller_time", "timestamp with time zone"),
                Map.entry("caller_span_id", "text"),
                Map.entry("caller_service_name", "text"),
                Map.entry("caller_host_name", "text"),
                Map.entry("caller_action_name", "text"),
                Map.entry("caller_target_name", "uuid"),
                Map.entry("caller_remote_addr", "text"),
                Map.entry("caller_system_type", "text"),
                Map.entry("caller_component", "text"),
                Map.entry("caller_span_kind", "smallint"),
                Map.entry("cnt", "bigint"),
                Map.entry("duration", "bigint"),
                Map.entry("error", "bigint"),
                Map.entry("create_time", "timestamp with time zone"),
                Map.entry("update_time", "timestamp with time zone")
        );

        assertPublicTableExists("service_call");
        assertTableColumnTypes("service_call", expectedColumnTypes);
    }

    /*
     * Task 3: Verify service_call table DDL - primary key definition.
     *
     * TimescaleDB requires the partitioning time column to participate in unique indexes on hypertables.
     * This test checks the declared primary-key column order through pg_constraint, rather than relying
     * on mapper writes to reveal a broken key later.
     */
    @Test
    void ddlCreatesServiceCallPrimaryKeyOnTimeAndSpanId() {
        assertPrimaryKeyColumns("service_call", List.of("time", "span_id"));
    }

    /*
     * Task 4: Verify service_call hypertable settings.
     *
     * The base table must be converted into a TimescaleDB hypertable on the time column. The two-hour
     * chunk interval is part of the DDL contract because it affects ingest layout, refresh cost, and
     * retention/compression behavior.
     */
    @Test
    void ddlConfiguresServiceCallHypertableTimeDimension() {
        assertHypertableTimeDimension("service_call", "time", "02:00:00");
    }

    /*
     * Task 1: Baseline DDL smoke test for service_call and the first CAGG layer.
     *
     * This intentionally checks only the smallest useful schema surface: the service_call hypertable
     * plus the entry/dependency minute CAGGs. If this fails, later rollup, policy, and view tests are
     * expected to fail too, so this acts as the quick "did the core DDL initialize?" signal.
     */
    @Test
    void ddlCreatesServiceCallHypertableAndBaseContinuousAggregates() {
        assertHypertableExists("service_call");
        assertContinuousAggregatesExist(Set.of(
                "service_call_minute_entry",
                "service_call_minute_dependency"
        ));
    }

    /*
     * Task 5: Verify base CAGG inventory and source hypertable mapping.
     *
     * The minute entry, minute dependency, and minute host-list CAGGs should all be built directly
     * from service_call. The test uses timescaledb_information.continuous_aggregates so it validates
     * Timescale's registered CAGG metadata, not just that similarly named views exist.
     */
    @Test
    void ddlCreatesBaseContinuousAggregatesFromServiceCall() {
        Map<String, String> caggSources = jdbcTemplate.queryForList("""
                        SELECT view_name, hypertable_name
                        FROM timescaledb_information.continuous_aggregates
                        WHERE view_name IN (
                            'service_call_minute_entry',
                            'service_call_minute_dependency',
                            'service_host_list_minute_entry'
                        )
                        """)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row.get("view_name"),
                        row -> (String) row.get("hypertable_name")));

        assertEquals("service_call", caggSources.get("service_call_minute_entry"));
        assertEquals("service_call", caggSources.get("service_call_minute_dependency"));
        assertEquals("service_call", caggSources.get("service_host_list_minute_entry"));
        assertEquals(3, caggSources.size());
    }

    /*
     * Task 6: Verify rollup CAGG inventory.
     *
     * This test protects the expected public CAGG family for service-call entry/dependency rollups,
     * service host lists, and topology relations. It checks object registration only; dependency
     * direction and query definitions are covered separately in Task 7.
     */
    @Test
    void ddlCreatesRollupContinuousAggregates() {
        Set<String> expectedRollupCaggs = Set.of(
                "service_call_5min_entry",
                "service_call_hour_entry",
                "service_call_day_entry",
                "service_call_5min_dependency",
                "service_call_hour_dependency",
                "service_call_day_dependency",
                "service_host_list_5min_entry",
                "service_host_list_hour_entry",
                "service_host_list_day_entry",
                "service_topology_relation_minute_entry",
                "service_topology_relation_minute_dependency",
                "service_topology_relation_5min_entry",
                "service_topology_relation_5min_dependency",
                "service_topology_relation_hour_entry",
                "service_topology_relation_hour_dependency",
                "service_topology_relation_day_entry",
                "service_topology_relation_day_dependency"
        );

        assertContinuousAggregatesExist(expectedRollupCaggs);
    }

    /*
     * Task 7: Verify CAGG dependency shape from each continuous aggregate definition.
     *
     * Existence alone cannot catch a rollup wired to the wrong source. This test inspects each
     * Timescale view_definition and asserts the coarse source chain, for example service_call to
     * minute, minute to 5-minute, 5-minute to hour, and hour to day. The regex is deliberately broad so formatting
     * changes in generated view SQL do not make the test brittle.
     */
    @Test
    void ddlCreatesExpectedContinuousAggregateDependencyShape() {
        Map<String, String> expectedSources = Map.ofEntries(
                Map.entry("service_call_minute_entry", "service_call"),
                Map.entry("service_call_minute_dependency", "service_call"),
                Map.entry("service_host_list_minute_entry", "service_call"),
                Map.entry("service_call_5min_entry", "service_call_minute_entry"),
                Map.entry("service_call_hour_entry", "service_call_5min_entry"),
                Map.entry("service_call_day_entry", "service_call_hour_entry"),
                Map.entry("service_call_5min_dependency", "service_call_minute_dependency"),
                Map.entry("service_call_hour_dependency", "service_call_5min_dependency"),
                Map.entry("service_call_day_dependency", "service_call_hour_dependency"),
                Map.entry("service_host_list_5min_entry", "service_host_list_minute_entry"),
                Map.entry("service_host_list_hour_entry", "service_host_list_5min_entry"),
                Map.entry("service_host_list_day_entry", "service_host_list_hour_entry"),
                Map.entry("service_topology_relation_minute_entry", "service_call_minute_entry"),
                Map.entry("service_topology_relation_minute_dependency", "service_call_minute_dependency"),
                Map.entry("service_topology_relation_5min_entry", "service_topology_relation_minute_entry"),
                Map.entry("service_topology_relation_5min_dependency", "service_topology_relation_minute_dependency"),
                Map.entry("service_topology_relation_hour_entry", "service_topology_relation_5min_entry"),
                Map.entry("service_topology_relation_hour_dependency", "service_topology_relation_5min_dependency"),
                Map.entry("service_topology_relation_day_entry", "service_topology_relation_hour_entry"),
                Map.entry("service_topology_relation_day_dependency", "service_topology_relation_hour_dependency")
        );

        Map<String, String> viewDefinitions = jdbcTemplate.queryForList("""
                        SELECT view_name, lower(view_definition) AS view_definition
                        FROM timescaledb_information.continuous_aggregates
                        """)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row.get("view_name"),
                        row -> (String) row.get("view_definition")));

        expectedSources.forEach((viewName, sourceName) ->
                assertContinuousAggregateSource(viewDefinitions, viewName, sourceName));
    }

    /*
     * Task 8: Verify refresh, retention, and columnstore policy jobs.
     *
     * The DDL does more than create tables and CAGGs: it also schedules Timescale background jobs.
     * This test checks the expected job targets in timescaledb_information.jobs for CAGG refresh,
     * retention, and columnstore/compression policies. It does not execute the jobs.
     */
    @Test
    void ddlCreatesExpectedPolicyJobs() {
        Set<String> expectedContinuousAggregatePolicyTargets = Set.of(
                "service_call_minute_entry",
                "service_call_minute_entry_merged",
                "service_call_minute_dependency",
                "service_call_minute_dependency_merged",
                "service_call_5min_entry",
                "service_call_5min_entry_merged",
                "service_call_5min_dependency",
                "service_call_5min_dependency_merged",
                "service_call_hour_entry",
                "service_call_hour_entry_merged",
                "service_call_hour_dependency",
                "service_call_hour_dependency_merged",
                "service_call_day_entry",
                "service_call_day_dependency",
                "service_host_list_minute_entry",
                "service_host_list_5min_entry",
                "service_host_list_hour_entry",
                "service_host_list_day_entry",
                "service_topology_relation_minute_entry",
                "service_topology_relation_minute_dependency",
                "service_topology_relation_5min_entry",
                "service_topology_relation_5min_dependency",
                "service_topology_relation_hour_entry",
                "service_topology_relation_hour_dependency",
                "service_topology_relation_day_entry",
                "service_topology_relation_day_dependency"
        );
        Set<String> expectedRetentionAndColumnstoreTargets = Set.of(
                "service_call",
                "service_call_minute_entry",
                "service_call_minute_entry_merged",
                "service_call_minute_dependency",
                "service_call_minute_dependency_merged",
                "service_call_5min_entry",
                "service_call_5min_entry_merged",
                "service_call_5min_dependency",
                "service_call_5min_dependency_merged",
                "service_call_hour_entry",
                "service_call_hour_entry_merged",
                "service_call_hour_dependency",
                "service_call_hour_dependency_merged",
                "service_call_day_entry",
                "service_call_day_dependency",
                "service_host_list_minute_entry",
                "service_host_list_5min_entry",
                "service_host_list_hour_entry",
                "service_host_list_day_entry",
                "service_topology_relation_minute_entry",
                "service_topology_relation_minute_dependency",
                "service_topology_relation_5min_entry",
                "service_topology_relation_5min_dependency",
                "service_topology_relation_hour_entry",
                "service_topology_relation_hour_dependency",
                "service_topology_relation_day_entry",
                "service_topology_relation_day_dependency"
        );

        Map<String, Set<String>> policyTargetsByProcName = jdbcTemplate.queryForList("""
                        SELECT proc_name, hypertable_name
                        FROM timescaledb_information.jobs
                        WHERE proc_name IN (
                            'policy_refresh_continuous_aggregate',
                            'policy_retention',
                            'policy_compression'
                        )
                        """)
                .stream()
                .collect(Collectors.groupingBy(
                        row -> (String) row.get("proc_name"),
                        Collectors.mapping(row -> (String) row.get("hypertable_name"), Collectors.toSet())));

        assertPolicyTargets(
                policyTargetsByProcName,
                "policy_refresh_continuous_aggregate",
                expectedContinuousAggregatePolicyTargets);
        assertPolicyTargets(
                policyTargetsByProcName,
                "policy_retention",
                expectedRetentionAndColumnstoreTargets);
        assertPolicyTargets(
                policyTargetsByProcName,
                "policy_compression",
                expectedRetentionAndColumnstoreTargets);
    }

    /*
     * Task 9: Verify important query indexes exist.
     *
     * These indexes support the expected query paths over service-call entry/dependency CAGGs,
     * service-host-list CAGGs, and topology relation CAGGs. The assertion reads pg_class instead of
     * pg_indexes because indexes created for continuous aggregates may live on Timescale internal
     * materialization hypertables rather than appearing as ordinary public indexes.
     */
    @Test
    void ddlCreatesImportantIndexes() {
        Set<String> expectedIndexNames = Set.of(
                "idx_sc_kind_time",
                "idx_sc_min_entry_svc_kind_time",
                "idx_sc_min_entry_svc_kind_tgt_time",
                "idx_sc_min_dep_caller",
                "idx_sc_min_dep_callee",
                "idx_shl_min_entry_svc_kind_time_inc_host",
                "idx_shl_5min_entry_svc_kind_time_inc_host",
                "idx_shl_hour_entry_svc_kind_time_inc_host",
                "idx_shl_day_entry_svc_kind_time_inc_host",
                "idx_str_min_entry_kind_caller_svc_type_time",
                "idx_str_min_dep_type_kind_caller_svc_time",
                "idx_str_5min_entry_kind_caller_svc_type_time",
                "idx_str_5min_dep_type_kind_caller_svc_time",
                "idx_str_hour_entry_kind_caller_svc_type_time",
                "idx_str_hour_dep_type_kind_caller_svc_time",
                "idx_str_day_entry_kind_caller_svc_type_time",
                "idx_str_day_dep_type_kind_caller_svc_time"
        );

        assertIndexesExist(expectedIndexNames);
    }

    /*
     * Task 10: Verify unified query views exist as normal public views.
     *
     * The unified service-call views expose hot CAGG data together with merged/archive CAGG data.
     * The topology views expose entry and dependency branches through stable public names. This test
     * verifies those public query views exist as relkind='v'; it does not validate their UNION ALL
     * predicates or returned rows.
     */
    @Test
    void ddlCreatesUnifiedViews() {
        Set<String> expectedViewNames = Set.of(
                "service_call_minute_entry_unified",
                "service_call_minute_dependency_unified",
                "service_call_5min_entry_unified",
                "service_call_5min_dependency_unified",
                "service_call_hour_entry_unified",
                "service_call_hour_dependency_unified",
                "service_topology_relation_minute",
                "service_topology_relation_5min",
                "service_topology_relation_hour",
                "service_topology_relation_day"
        );

        assertPublicViewsExist(expectedViewNames);
    }

    private void assertExtensionsInstalled(Set<String> expectedExtensionNames) {
        List<String> actualExtensionNames = jdbcTemplate.queryForList("""
                SELECT extname
                FROM pg_extension
                """, String.class);

        assertNamesPresent(expectedExtensionNames, actualExtensionNames, "extensions");
    }

    private void assertPublicTableExists(String tableName) {
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM pg_catalog.pg_class cls
                JOIN pg_catalog.pg_namespace ns ON ns.oid = cls.relnamespace
                WHERE ns.nspname = 'public'
                  AND cls.relname = ?
                  AND cls.relkind = 'r'
                """, Integer.class, tableName);

        assertEquals(1, tableCount, "Missing public table: " + tableName);
    }

    private void assertTableColumnTypes(String tableName, Map<String, String> expectedColumnTypes) {
        Map<String, String> actualColumnTypes = jdbcTemplate.queryForList("""
                        SELECT column_name, data_type
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = ?
                        """, tableName)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row.get("column_name"),
                        row -> (String) row.get("data_type")));

        assertEquals(expectedColumnTypes, actualColumnTypes);
    }

    private void assertPrimaryKeyColumns(String tableName, List<String> expectedPrimaryKeyColumns) {
        List<String> actualPrimaryKeyColumns = jdbcTemplate.queryForList("""
                SELECT attr.attname
                FROM pg_catalog.pg_constraint con
                JOIN pg_catalog.pg_class cls ON cls.oid = con.conrelid
                JOIN pg_catalog.pg_namespace ns ON ns.oid = cls.relnamespace
                JOIN unnest(con.conkey) WITH ORDINALITY AS key_cols(attnum, ord) ON true
                JOIN pg_catalog.pg_attribute attr
                  ON attr.attrelid = cls.oid
                 AND attr.attnum = key_cols.attnum
                WHERE ns.nspname = 'public'
                  AND cls.relname = ?
                  AND con.contype = 'p'
                ORDER BY key_cols.ord
                """, String.class, tableName);

        assertIterableEquals(expectedPrimaryKeyColumns, actualPrimaryKeyColumns);
    }

    private void assertHypertableTimeDimension(String hypertableName,
                                               String expectedColumnName,
                                               String expectedTimeInterval) {
        Map<String, Object> dimension = jdbcTemplate.queryForMap("""
                SELECT column_name, time_interval::text AS time_interval
                FROM timescaledb_information.dimensions
                WHERE hypertable_name = ?
                  AND dimension_type = 'Time'
                """, hypertableName);

        assertEquals(expectedColumnName, dimension.get("column_name"));
        assertEquals(expectedTimeInterval, dimension.get("time_interval"));
    }

    private void assertHypertableExists(String hypertableName) {
        Integer hypertableCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM timescaledb_information.hypertables
                WHERE hypertable_name = ?
                """, Integer.class, hypertableName);

        assertEquals(1, hypertableCount, "Missing hypertable: " + hypertableName);
    }

    private void assertContinuousAggregatesExist(Set<String> expectedViewNames) {
        List<String> actualViewNames = jdbcTemplate.queryForList("""
                SELECT view_name
                FROM timescaledb_information.continuous_aggregates
                """, String.class);

        assertNamesPresent(expectedViewNames, actualViewNames, "continuous aggregates");
    }

    private void assertIndexesExist(Set<String> expectedIndexNames) {
        List<String> actualIndexNames = jdbcTemplate.queryForList("""
                SELECT cls.relname
                FROM pg_catalog.pg_class cls
                WHERE cls.relkind = 'i'
                """, String.class);

        assertNamesPresent(expectedIndexNames, actualIndexNames, "indexes");
    }

    private void assertPublicViewsExist(Set<String> expectedViewNames) {
        List<String> actualViewNames = jdbcTemplate.queryForList("""
                SELECT cls.relname
                FROM pg_catalog.pg_class cls
                JOIN pg_catalog.pg_namespace ns ON ns.oid = cls.relnamespace
                WHERE ns.nspname = 'public'
                  AND cls.relkind = 'v'
                """, String.class);

        assertNamesPresent(expectedViewNames, actualViewNames, "public views");
    }

    private void assertPolicyTargets(Map<String, Set<String>> policyTargetsByProcName,
                                     String procName,
                                     Set<String> expectedTargets) {
        assertEquals(expectedTargets, policyTargetsByProcName.get(procName), "Unexpected policy targets for " + procName);
    }

    private void assertNamesPresent(Set<String> expectedNames, List<String> actualNames, String objectType) {
        Set<String> actualNameSet = Set.copyOf(actualNames);
        Set<String> missingNames = expectedNames.stream()
                .filter(name -> !actualNameSet.contains(name))
                .collect(Collectors.toSet());

        assertTrue(missingNames.isEmpty(), "Missing " + objectType + ": " + missingNames);
    }

    private void assertContinuousAggregateSource(Map<String, String> viewDefinitions,
                                                 String viewName,
                                                 String sourceName) {
        String viewDefinition = viewDefinitions.get(viewName);

        assertTrue(viewDefinition != null, "Missing continuous aggregate definition for " + viewName);
        assertTrue(
                viewDefinition.matches("(?s).*\\bfrom\\s+(public\\.)?" + sourceName + "\\b.*"),
                viewName + " should be sourced from " + sourceName);
    }
}
