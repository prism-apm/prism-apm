package org.prism.apm.parser.repository;

import org.prism.apm.parser.MybatisTestBase;
import org.prism.apm.parser.enums.SpanKindEnum;
import org.prism.apm.parser.model.entity.ServiceCallEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data-backed CAGG tests that complement the DDL-only metadata checks.
 *
 * These tests insert a small, deterministic service_call fixture, refresh the affected continuous
 * aggregates manually, and verify the public rollup/view metrics. Each test uses a unique service
 * name so it remains stable even though the TimescaleDB container is shared across the suite.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ApmTimescaleCaggCalculationTest extends MybatisTestBase {

    @Autowired
    private ServiceCallMapper serviceCallMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private record EntryFixtureDimensions(
            String serviceName,
            String hostName,
            String actionName,
            UUID targetNameId,
            String remoteAddr,
            String systemType,
            String component,
            short spanKind,
            String callerServiceName,
            String callerHostName,
            String callerActionName,
            UUID callerTargetNameId,
            String callerRemoteAddr,
            String callerSystemType,
            String callerComponent,
            short callerSpanKind) {
    }

    /**
     * Verifies entry-side calculation and aggregation for the core scalar metrics.
     *
     * Starting from raw `service_call` rows, this test checks that the following relations are
     * calculated correctly on the entry CAGG chain:
     * - `service_call_minute_entry`
     * - `service_call_5min_entry`
     * - `service_call_hour_entry`
     * - `service_call_day_entry`
     *
     * Fields checked on those entry CAGGs:
     * - `cnt`
     * - `sum_duration`
     * - `avg_duration`
     * - `max_duration`
     * - `min_duration`
     * - `error_num`
     *
     * This test also verifies that the minute-level downstream consumer objects remain aligned
     * with the same source data:
     * - `service_host_list_minute_entry.cnt`
     * - `service_topology_relation_minute.cnt`
     * - `service_topology_relation_minute.avg_duration`
     * - `service_topology_relation_minute.error_num`
     *
     * Insert rows at:
     * - `10:01:05` with `duration = 100` and `error = 0`
     * - `10:01:20` with `duration = 200` and `error = 1`
     * - `10:04:10` with `duration = 300` and `error = 0`
     * - `10:06:40` with `duration = 500` and `error = 1`
     *
     * Keep the service and caller dimensions identical so only the bucket timestamp changes.
     *
     * Assert minute entry buckets:
     * - `10:01` has `cnt = 2`, `sum_duration = 300`, `avg_duration = 150`,
     *   `max_duration = 200`, `min_duration = 100`, and `error_num = 1`
     * - `10:04` has `cnt = 1`, `sum_duration = 300`, `avg_duration = 300`,
     *   `max_duration = 300`, `min_duration = 300`, and `error_num = 0`
     * - `10:06` has `cnt = 1`, `sum_duration = 500`, `avg_duration = 500`,
     *   `max_duration = 500`, `min_duration = 500`, and `error_num = 1`
     *
     * Assert wider entry rollup buckets:
     * - 5-minute bucket `10:00` has `cnt = 3`, `sum_duration = 600`,
     *   `avg_duration = 200`, `max_duration = 300`, `min_duration = 100`, and `error_num = 1`
     * - 5-minute bucket `10:05` has `cnt = 1`, `sum_duration = 500`,
     *   `avg_duration = 500`, `max_duration = 500`, `min_duration = 500`, and `error_num = 1`
     * - hour bucket `10:00` has `cnt = 4`, `sum_duration = 1100`,
     *   `avg_duration = 275`, `max_duration = 500`, `min_duration = 100`, and `error_num = 2`
     * - day bucket has `cnt = 4`, `sum_duration = 1100`, `avg_duration = 275`,
     *   `max_duration = 500`, `min_duration = 100`, and `error_num = 2`
     *
     * Assert downstream minute objects:
     * - `service_host_list_minute_entry` has bucket counts `2`, `1`, and `1`
     * - `service_topology_relation_minute` has the same bucket counts plus
     *   `avg_duration = 150/300/500` and `error_num = 1/0/1`
     */
    @Test
    void entryRollupsCalculateExpectedNumericMetrics() {
        String testId = "entry_calc_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime minute101 = dayStart.plusHours(10).plusMinutes(1);
        OffsetDateTime minute104 = dayStart.plusHours(10).plusMinutes(4);
        OffsetDateTime minute106 = dayStart.plusHours(10).plusMinutes(6);

        // Arrange: build three minute buckets with durations and errors that make cnt, sum, avg,
        // min, max, and error_num change in a visible way as the data rolls up.
        List<ServiceCallEntity> rows = List.of(
                entryRow(testId, serviceName, callerServiceName, minute101.plusSeconds(5), 100, 0),
                entryRow(testId, serviceName, callerServiceName, minute101.plusSeconds(20), 200, 1),
                entryRow(testId, serviceName, callerServiceName, minute104.plusSeconds(10), 300, 0),
                entryRow(testId, serviceName, callerServiceName, minute106.plusSeconds(40), 500, 1)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        OffsetDateTime refreshStart = dayStart;
        OffsetDateTime refreshEnd = dayStart.plusDays(1);

        // Act: refresh only the entry-side rollups and the minute-level consumer aggregates that
        // should reflect this fixture window.
        refreshContinuousAggregate("service_call_minute_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_5min_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_hour_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_day_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_host_list_minute_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_minute_entry", refreshStart, refreshEnd);

        // Assert: minute buckets preserve the raw source math before any higher-level rollup.
        assertServiceCallAggregateRow(
                "service_call_minute_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                minute101,
                2,
                300,
                150,
                200,
                100,
                1);
        assertServiceCallAggregateRow(
                "service_call_minute_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                minute104,
                1,
                300,
                300,
                300,
                300,
                0);
        assertServiceCallAggregateRow(
                "service_call_minute_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                minute106,
                1,
                500,
                500,
                500,
                500,
                1);

        // Assert: 5-minute, hour, and day entry CAGGs recompute the same scalar metrics correctly
        // after minute rows are rolled into wider buckets.
        assertServiceCallAggregateRow(
                "service_call_5min_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                dayStart.plusHours(10),
                3,
                600,
                200,
                300,
                100,
                1);
        assertServiceCallAggregateRow(
                "service_call_5min_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                dayStart.plusHours(10).plusMinutes(5),
                1,
                500,
                500,
                500,
                500,
                1);

        assertServiceCallAggregateRow(
                "service_call_hour_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                dayStart.plusHours(10),
                4,
                1100,
                275,
                500,
                100,
                2);
        assertServiceCallAggregateRow(
                "service_call_day_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                dayStart,
                4,
                1100,
                275,
                500,
                100,
                2);

        // Assert: dependent minute-level consumer objects expose counts and durations that stay
        // consistent with the entry CAGG for the same service and caller dimensions.
        assertHostListAggregateRow(serviceName, SpanKindEnum.SERVER.getValue(), minute101, 2);
        assertHostListAggregateRow(serviceName, SpanKindEnum.SERVER.getValue(), minute104, 1);
        assertHostListAggregateRow(serviceName, SpanKindEnum.SERVER.getValue(), minute106, 1);

        assertTopologyAggregateRow(
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                minute101,
                2,
                150,
                1);
        assertTopologyAggregateRow(
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                minute104,
                1,
                300,
                0);
        assertTopologyAggregateRow(
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                minute106,
                1,
                500,
                1);
    }

    /**
     * Verifies `percent_duration` rollup behavior across the entry aggregation chain.
     *
     * Starting from raw `service_call` rows, this test checks the percentile sketch that is built
     * on:
     * - `service_call_minute_entry`
     * - `service_call_5min_entry`
     * - `service_call_hour_entry`
     * - `service_call_day_entry`
     *
     * Fields checked on those entry CAGGs:
     * - `approx_percentile(0.90, percent_duration)`
     * - `approx_percentile(0.95, percent_duration)`
     * - `approx_percentile(0.99, percent_duration)`
     * - percentile ordering checks (`p90 < p95 < p99`)
     * - percentile separation checks (`p95 - p90` and `p99 - p95`)
     *
     * Insert 100 rows in each minute bucket:
     * - `10:01`
     * - `10:02`
     * - `10:04`
     *
     * Use the same monotonic duration distribution in every minute bucket:
     * - `100, 200, ..., 10000`
     *
     * Keep all grouping dimensions identical so the three minute buckets roll into one 5-minute,
     * one hour, and one day bucket.
     *
     * Assert percentile values on minute, 5-minute, hour, and day entry CAGGs:
     * - `approx_percentile(0.90, percent_duration)` is in the expected high-percentile range
     * - `approx_percentile(0.95, percent_duration)` is above p90
     * - `approx_percentile(0.99, percent_duration)` is near the top of the distribution
     * - `p90 < p95 < p99`
     * - `p95 - p90` and `p99 - p95` remain meaningfully separated
     */
    @Test
    void entryRollupsExposePercentDuration() {
        String testId = "entry_percent_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime minute101 = dayStart.plusHours(10).plusMinutes(1);
        OffsetDateTime minute102 = dayStart.plusHours(10).plusMinutes(2);
        OffsetDateTime minute104 = dayStart.plusHours(10).plusMinutes(4);

        // Arrange: each minute bucket gets the same steadily increasing duration distribution.
        // That makes p90, p95, and p99 meaningfully different in the real Toolkit output and
        // catches bugs where percentile rollups flatten to one result after rollup(percent_duration).
        List<ServiceCallEntity> rows = new ArrayList<>();
        addPercentileFixtureRows(rows, testId + "_101", serviceName, callerServiceName, minute101);
        addPercentileFixtureRows(rows, testId + "_102", serviceName, callerServiceName, minute102);
        addPercentileFixtureRows(rows, testId + "_104", serviceName, callerServiceName, minute104);

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        // Act: refresh the entry-side rollup chain from minute to day for the fixture window.
        OffsetDateTime refreshStart = dayStart;
        OffsetDateTime refreshEnd = dayStart.plusDays(1);

        refreshContinuousAggregate("service_call_minute_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_5min_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_hour_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_day_entry", refreshStart, refreshEnd);

        // Assert: every grain should preserve the same percentile shape rather than flattening the
        // distribution. We check both approximate cluster values and strict p90 < p95 < p99 order.
        assertPercentDurationRow("service_call_minute_entry", serviceName, SpanKindEnum.SERVER.getValue(), minute101);
        assertPercentDurationRow("service_call_minute_entry", serviceName, SpanKindEnum.SERVER.getValue(), minute102);
        assertPercentDurationRow("service_call_minute_entry", serviceName, SpanKindEnum.SERVER.getValue(), minute104);
        assertPercentDurationRow(
                "service_call_5min_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                dayStart.plusHours(10));
        assertPercentDurationRow(
                "service_call_hour_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                dayStart.plusHours(10));
        assertPercentDurationRow(
                "service_call_day_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                dayStart);
    }

    /**
     * Verifies that the unified entry views correctly expose both recent hot data and older
     * archived data without losing or duplicating buckets.
     *
     * Starting from raw `service_call` rows, this test checks:
     * - `service_call_minute_entry_unified`
     * - `service_call_5min_entry_unified`
     * - `service_call_hour_entry_unified`
     *
     * Fields checked on those unified views:
     * - row count per unified view
     * - `cnt`
     * - `sum_duration`
     * - `avg_duration`
     * - `max_duration`
     * - `min_duration`
     * - `error_num`
     * - `approx_percentile(0.90, percent_duration)`
     * - `approx_percentile(0.95, percent_duration)`
     * - `approx_percentile(0.99, percent_duration)`
     *
     * Fields checked on the minute-layer hot/archive objects:
     * - `service_call_minute_entry.last_insert_time`
     * - `service_call_minute_entry_merged.last_insert_time`
     * - `service_call_minute_entry_unified.last_insert_time`
     *
     * The fixture uses one recent bucket and one archived bucket on purpose:
     * - the recent bucket should be served by the hot CAGG branch
     * - the archived bucket should be served by the merged branch
     * - each bucket has two rows with increasing `update_time`, so `MAX(update_time)` is exact
     *
     * Insert recent hot-bucket rows at:
     * - `recentTime`
     * - `recentTime + 10 seconds`
     * Use `duration = 210`, `error = 0`, and expect `last_insert_time = recentTime + 22 seconds`.
     *
     * Insert archived bucket rows at:
     * - `archivedTime`
     * - `archivedTime + 10 seconds`
     * Use `duration = 420`, `error = 1`, and expect `last_insert_time = archivedTime + 22 seconds`.
     *
     * Keep all grouping dimensions identical across the four rows so only the bucket time and
     * hot/archive branch determine where the data is read from.
     *
     * Assert each unified view returns exactly 2 rows for the test service:
     * - one recent hot bucket
     * - one archived merged bucket
     *
     * Assert the expected recent-bucket values on minute, 5-minute, and hour unified views:
     * - `cnt = 2`
     * - `sum_duration = 420`
     * - `avg_duration = 210`
     * - `max_duration = 210`
     * - `min_duration = 210`
     * - `error_num = 0`
     * - `approx_percentile(0.90/0.95/0.99, percent_duration) = 210`
     *
     * Assert the expected archived-bucket values on minute, 5-minute, and hour unified views:
     * - `cnt = 2`
     * - `sum_duration = 840`
     * - `avg_duration = 420`
     * - `max_duration = 420`
     * - `min_duration = 420`
     * - `error_num = 2`
     * - `approx_percentile(0.90/0.95/0.99, percent_duration) = 420`
     *
     * Assert exact minute-layer `last_insert_time` values:
     * - recent bucket uses `recentTime + 22 seconds`
     * - archived bucket uses `archivedTime + 22 seconds`
     *
     * That layout proves the unified views cross the hot/archive boundary correctly while keeping
     * the exposed bucket metrics stable.
     */
    @Test
    void unifiedEntryViewsExposeRecentAndArchivedBuckets() {
        String testId = "entry_unified_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;

        OffsetDateTime nowUtc = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime recentTime = nowUtc.minusHours(3).withSecond(15).withNano(0);
        OffsetDateTime archivedTime = nowUtc.minusDays(20).withHour(4).withMinute(12).withSecond(20).withNano(0);
        OffsetDateTime recentLastInsertTime = recentTime.plusSeconds(22);
        OffsetDateTime archivedLastInsertTime = archivedTime.plusSeconds(22);

        // Arrange: insert one bucket that stays in the hot retention window and one bucket that is
        // old enough to be served from the merged/archive branch. Each bucket has two rows so the
        // minute-level last_insert_time check proves MAX(update_time), not just pass-through.
        List<ServiceCallEntity> rows = List.of(
                entryRow(
                        testId + "_recent_1",
                        serviceName,
                        callerServiceName,
                        recentTime,
                        210,
                        0,
                        recentTime.plusSeconds(11)),
                entryRow(
                        testId + "_recent_2",
                        serviceName,
                        callerServiceName,
                        recentTime.plusSeconds(10),
                        210,
                        0,
                        recentLastInsertTime),
                entryRow(
                        testId + "_archive_1",
                        serviceName,
                        callerServiceName,
                        archivedTime,
                        420,
                        1,
                        archivedTime.plusSeconds(11)),
                entryRow(
                        testId + "_archive_2",
                        serviceName,
                        callerServiceName,
                        archivedTime.plusSeconds(10),
                        420,
                        1,
                        archivedLastInsertTime)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        OffsetDateTime refreshStart = startOfUtcDay(archivedTime);
        OffsetDateTime refreshEnd = startOfUtcDay(recentTime).plusDays(1);

        // Act: refresh both the hot and merged CAGGs so the unified views can read from each side
        // of the time boundary in the same test.
        refreshContinuousAggregate("service_call_minute_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_minute_entry_merged", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_5min_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_5min_entry_merged", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_hour_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_hour_entry_merged", refreshStart, refreshEnd);

        OffsetDateTime recentMinuteBucket = recentTime.withSecond(0).withNano(0);
        OffsetDateTime archivedMinuteBucket = archivedTime.withSecond(0).withNano(0);
        OffsetDateTime recent5MinBucket = floorToFiveMinutes(recentTime);
        OffsetDateTime archived5MinBucket = floorToFiveMinutes(archivedTime);
        OffsetDateTime recentHourBucket = recentTime.withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime archivedHourBucket = archivedTime.withMinute(0).withSecond(0).withNano(0);

        // Assert: each unified view returns exactly one hot bucket and one archived bucket for the
        // test service, which guards against dropped rows and double counting.
        assertViewRowCount("service_call_minute_entry_unified", serviceName, 2);
        assertViewRowCount("service_call_5min_entry_unified", serviceName, 2);
        assertViewRowCount("service_call_hour_entry_unified", serviceName, 2);

        // Assert: last_insert_time is only defined on minute-layer entry objects, where it should
        // equal the maximum update_time in each source bucket.
        assertLastInsertTime(
                "service_call_minute_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                recentMinuteBucket,
                recentLastInsertTime);
        assertLastInsertTime(
                "service_call_minute_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                archivedMinuteBucket,
                archivedLastInsertTime);
        assertLastInsertTime(
                "service_call_minute_entry_merged",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                recentMinuteBucket,
                recentLastInsertTime);
        assertLastInsertTime(
                "service_call_minute_entry_merged",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                archivedMinuteBucket,
                archivedLastInsertTime);
        assertLastInsertTime(
                "service_call_minute_entry_unified",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                recentMinuteBucket,
                recentLastInsertTime);
        assertLastInsertTime(
                "service_call_minute_entry_unified",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                archivedMinuteBucket,
                archivedLastInsertTime);

        // Assert: the unified minute, 5-minute, and hour views expose the same scalar and
        // percentile metrics regardless of whether data comes from the hot or merged branch.
        assertServiceCallAggregateRow(
                "service_call_minute_entry_unified",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                recentMinuteBucket,
                2,
                420,
                210,
                210,
                210,
                0);
        assertServiceCallAggregateRow(
                "service_call_minute_entry_unified",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                archivedMinuteBucket,
                2,
                840,
                420,
                420,
                420,
                2);
        assertUniformPercentDurationRow(
                "service_call_minute_entry_unified",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                recentMinuteBucket,
                210);
        assertUniformPercentDurationRow(
                "service_call_minute_entry_unified",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                archivedMinuteBucket,
                420);

        assertServiceCallAggregateRow(
                "service_call_5min_entry_unified",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                recent5MinBucket,
                2,
                420,
                210,
                210,
                210,
                0);
        assertServiceCallAggregateRow(
                "service_call_5min_entry_unified",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                archived5MinBucket,
                2,
                840,
                420,
                420,
                420,
                2);
        assertUniformPercentDurationRow(
                "service_call_5min_entry_unified",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                recent5MinBucket,
                210);
        assertUniformPercentDurationRow(
                "service_call_5min_entry_unified",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                archived5MinBucket,
                420);

        assertServiceCallAggregateRow(
                "service_call_hour_entry_unified",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                recentHourBucket,
                2,
                420,
                210,
                210,
                210,
                0);
        assertServiceCallAggregateRow(
                "service_call_hour_entry_unified",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                archivedHourBucket,
                2,
                840,
                420,
                420,
                420,
                2);
        assertUniformPercentDurationRow(
                "service_call_hour_entry_unified",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                recentHourBucket,
                210);
        assertUniformPercentDurationRow(
                "service_call_hour_entry_unified",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                archivedHourBucket,
                420);
    }

    /**
     * Verifies the exact 1-minute `time_bucket` boundary behavior for entry CAGGs.
     *
     * Starting from raw `service_call` rows, this test checks:
     * - `service_call_minute_entry`
     *
     * Fields checked on the minute entry CAGG:
     * - bucket `time`
     * - `cnt`
     * - `sum_duration`
     * - `avg_duration`
     * - `max_duration`
     * - `min_duration`
     * - `error_num`
     * - `approx_percentile(0.90, percent_duration)`
     * - `approx_percentile(0.95, percent_duration)`
     * - `approx_percentile(0.99, percent_duration)`
     * - `last_insert_time`
     *
     * Insert rows at:
     * - `10:01:00.000000` with `duration = 180` and `error = 0`
     * - `10:01:59.999999` with `duration = 180` and `error = 1`
     * - `10:02:00.000000` with `duration = 360` and `error = 0`
     *
     * Keep all non-time grouping columns identical.
     *
     * Assert exactly 2 minute buckets:
     * - `10:01:00` has `cnt = 2`, `sum_duration = 360`, `avg_duration = 180`,
     *   `max_duration = 180`, `min_duration = 180`, `error_num = 1`,
     *   all checked percentiles equal `180`, and `last_insert_time = 10:02:00`
     * - `10:02:00` has `cnt = 1`, `sum_duration = 360`, `avg_duration = 360`,
     *   `max_duration = 360`, `min_duration = 360`, `error_num = 0`,
     *   all checked percentiles equal `360`, and `last_insert_time = 10:02:11`
     *
     * This proves the bucket interval is closed on the start and open on the end:
     * `[10:01:00, 10:02:00)`.
     */
    @Test
    void minuteEntryBucketBoundaryIsCorrect() {
        String testId = "entry_minute_boundary_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime minute101 = dayStart.plusHours(10).plusMinutes(1);
        OffsetDateTime minute102 = minute101.plusMinutes(1);
        OffsetDateTime lastMicrosecondBefore102 = minute102.minusNanos(1_000);
        OffsetDateTime minute101LastInsertTime = minute102;
        OffsetDateTime minute102LastInsertTime = minute102.plusSeconds(11);

        // Arrange: keep all grouping dimensions identical and vary only event time. Two rows should
        // remain in the 10:01 bucket, while the exact 10:02 row should start the next bucket.
        List<ServiceCallEntity> rows = List.of(
                entryRow(
                        testId + "_start",
                        serviceName,
                        callerServiceName,
                        minute101,
                        180,
                        0,
                        minute101.plusSeconds(10)),
                entryRow(
                        testId + "_last_microsecond",
                        serviceName,
                        callerServiceName,
                        lastMicrosecondBefore102,
                        180,
                        1,
                        minute101LastInsertTime),
                entryRow(
                        testId + "_next_bucket",
                        serviceName,
                        callerServiceName,
                        minute102,
                        360,
                        0,
                        minute102LastInsertTime)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        // Act: refresh only the minute entry CAGG because this test isolates the minute boundary.
        refreshContinuousAggregate("service_call_minute_entry", minute101.minusMinutes(1), minute102.plusMinutes(1));

        // Assert: the boundary split itself is correct before checking derived metrics.
        assertBucketSplit(
                "service_call_minute_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                minute101,
                2,
                minute102,
                1);

        // Assert: the two rows before 10:02 roll into the 10:01 bucket, including error count,
        // uniform percentile values, and MAX(update_time) as last_insert_time.
        assertServiceCallAggregateRow(
                "service_call_minute_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                minute101,
                2,
                360,
                180,
                180,
                180,
                1);
        assertUniformPercentDurationRow(
                "service_call_minute_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                minute101,
                180);
        assertLastInsertTime(
                "service_call_minute_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                minute101,
                minute101LastInsertTime);

        // Assert: the exact 10:02 row starts a new bucket instead of joining the previous minute.
        assertServiceCallAggregateRow(
                "service_call_minute_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                minute102,
                1,
                360,
                360,
                360,
                360,
                0);
        assertUniformPercentDurationRow(
                "service_call_minute_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                minute102,
                360);
        assertLastInsertTime(
                "service_call_minute_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                minute102,
                minute102LastInsertTime);
    }

    /**
     * Verifies the exact 5-minute `time_bucket` boundary behavior for entry CAGGs.
     *
     * Starting from raw `service_call` rows, this test checks:
     * - `service_call_5min_entry`
     *
     * Fields checked on the 5-minute entry CAGG:
     * - bucket `time`
     * - `cnt`
     * - `sum_duration`
     * - `avg_duration`
     * - `max_duration`
     * - `min_duration`
     * - `error_num`
     * - `approx_percentile(0.90, percent_duration)`
     * - `approx_percentile(0.95, percent_duration)`
     * - `approx_percentile(0.99, percent_duration)`
     *
     * Insert rows at:
     * - `10:00:00.000000` with `duration = 180` and `error = 0`
     * - `10:04:59.999999` with `duration = 180` and `error = 1`
     * - `10:05:00.000000` with `duration = 360` and `error = 0`
     *
     * Keep all non-time grouping columns identical.
     *
     * Assert exactly 2 five-minute buckets:
     * - `10:00:00` has `cnt = 2`, `sum_duration = 360`, `avg_duration = 180`,
     *   `max_duration = 180`, `min_duration = 180`, `error_num = 1`,
     *   and all checked percentiles equal `180`
     * - `10:05:00` has `cnt = 1`, `sum_duration = 360`, `avg_duration = 360`,
     *   `max_duration = 360`, `min_duration = 360`, `error_num = 0`,
     *   and all checked percentiles equal `360`
     *
     * This proves the 5-minute bucket interval is closed on the start and open on the end:
     * `[10:00:00, 10:05:00)`.
     */
    @Test
    void fiveMinuteEntryBucketBoundaryIsCorrect() {
        String testId = "entry_5min_boundary_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime fiveMin1000 = dayStart.plusHours(10);
        OffsetDateTime fiveMin1005 = fiveMin1000.plusMinutes(5);
        OffsetDateTime lastMicrosecondBefore1005 = fiveMin1005.minusNanos(1_000);

        // Arrange: keep all grouping dimensions identical and vary only event time. Two rows should
        // remain in the 10:00 five-minute bucket, while the exact 10:05 row should start the next bucket.
        List<ServiceCallEntity> rows = List.of(
                entryRow(
                        testId + "_start",
                        serviceName,
                        callerServiceName,
                        fiveMin1000,
                        180,
                        0),
                entryRow(
                        testId + "_last_microsecond",
                        serviceName,
                        callerServiceName,
                        lastMicrosecondBefore1005,
                        180,
                        1),
                entryRow(
                        testId + "_next_bucket",
                        serviceName,
                        callerServiceName,
                        fiveMin1005,
                        360,
                        0)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        // Act: refresh minute first because the 5-minute entry CAGG rolls up from minute data.
        OffsetDateTime refreshStart = fiveMin1000.minusMinutes(1);
        OffsetDateTime refreshEnd = fiveMin1005.plusMinutes(5);

        refreshContinuousAggregate("service_call_minute_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_5min_entry", refreshStart, refreshEnd);

        // Assert: the boundary split itself is correct before checking derived metrics.
        assertBucketSplit(
                "service_call_5min_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                fiveMin1000,
                2,
                fiveMin1005,
                1);

        // Assert: the two rows before 10:05 roll into the 10:00 five-minute bucket.
        assertServiceCallAggregateRow(
                "service_call_5min_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                fiveMin1000,
                2,
                360,
                180,
                180,
                180,
                1);
        assertUniformPercentDurationRow(
                "service_call_5min_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                fiveMin1000,
                180);

        // Assert: the exact 10:05 row starts a new five-minute bucket.
        assertServiceCallAggregateRow(
                "service_call_5min_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                fiveMin1005,
                1,
                360,
                360,
                360,
                360,
                0);
        assertUniformPercentDurationRow(
                "service_call_5min_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                fiveMin1005,
                360);
    }

    /**
     * Verifies the exact 1-hour `time_bucket` boundary behavior for entry CAGGs.
     *
     * Starting from raw `service_call` rows, this test checks:
     * - `service_call_hour_entry`
     *
     * Fields checked on the hour entry CAGG:
     * - bucket `time`
     * - `cnt`
     * - `sum_duration`
     * - `avg_duration`
     * - `max_duration`
     * - `min_duration`
     * - `error_num`
     * - `approx_percentile(0.90, percent_duration)`
     * - `approx_percentile(0.95, percent_duration)`
     * - `approx_percentile(0.99, percent_duration)`
     *
     * Insert rows at:
     * - `10:00:00.000000` with `duration = 180` and `error = 0`
     * - `10:59:59.999999` with `duration = 180` and `error = 1`
     * - `11:00:00.000000` with `duration = 360` and `error = 0`
     *
     * Keep all non-time grouping columns identical.
     *
     * Assert exactly 2 hour buckets:
     * - `10:00:00` has `cnt = 2`, `sum_duration = 360`, `avg_duration = 180`,
     *   `max_duration = 180`, `min_duration = 180`, `error_num = 1`,
     *   and all checked percentiles equal `180`
     * - `11:00:00` has `cnt = 1`, `sum_duration = 360`, `avg_duration = 360`,
     *   `max_duration = 360`, `min_duration = 360`, `error_num = 0`,
     *   and all checked percentiles equal `360`
     *
     * This proves the hour bucket interval is closed on the start and open on the end:
     * `[10:00:00, 11:00:00)`.
     */
    @Test
    void hourEntryBucketBoundaryIsCorrect() {
        String testId = "entry_hour_boundary_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime hour10 = dayStart.plusHours(10);
        OffsetDateTime hour11 = hour10.plusHours(1);
        OffsetDateTime lastMicrosecondBefore11 = hour11.minusNanos(1_000);

        // Arrange: keep all grouping dimensions identical and vary only event time. Two rows should
        // remain in the 10:00 hour bucket, while the exact 11:00 row should start the next bucket.
        List<ServiceCallEntity> rows = List.of(
                entryRow(
                        testId + "_start",
                        serviceName,
                        callerServiceName,
                        hour10,
                        180,
                        0),
                entryRow(
                        testId + "_last_microsecond",
                        serviceName,
                        callerServiceName,
                        lastMicrosecondBefore11,
                        180,
                        1),
                entryRow(
                        testId + "_next_bucket",
                        serviceName,
                        callerServiceName,
                        hour11,
                        360,
                        0)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        // Act: refresh minute first because the hour entry CAGG rolls up from minute data.
        OffsetDateTime refreshStart = hour10.minusHours(1);
        OffsetDateTime refreshEnd = hour11.plusHours(1);

        refreshContinuousAggregate("service_call_minute_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_5min_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_hour_entry", refreshStart, refreshEnd);

        // Assert: the boundary split itself is correct before checking derived metrics.
        assertBucketSplit(
                "service_call_hour_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                hour10,
                2,
                hour11,
                1);

        // Assert: the two rows before 11:00 roll into the 10:00 hour bucket.
        assertServiceCallAggregateRow(
                "service_call_hour_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                hour10,
                2,
                360,
                180,
                180,
                180,
                1);
        assertUniformPercentDurationRow(
                "service_call_hour_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                hour10,
                180);

        // Assert: the exact 11:00 row starts a new hour bucket.
        assertServiceCallAggregateRow(
                "service_call_hour_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                hour11,
                1,
                360,
                360,
                360,
                360,
                0);
        assertUniformPercentDurationRow(
                "service_call_hour_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                hour11,
                360);
    }

    /**
     * Verifies the exact 1-day `time_bucket` boundary behavior for entry CAGGs.
     *
     * Starting from raw `service_call` rows, this test checks:
     * - `service_call_day_entry`
     *
     * Fields checked on the day entry CAGG:
     * - bucket `time`
     * - `cnt`
     * - `sum_duration`
     * - `avg_duration`
     * - `max_duration`
     * - `min_duration`
     * - `error_num`
     * - `approx_percentile(0.90, percent_duration)`
     * - `approx_percentile(0.95, percent_duration)`
     * - `approx_percentile(0.99, percent_duration)`
     *
     * Insert rows at:
     * - `firstDay 00:00:00.000000` with `duration = 180` and `error = 0`
     * - `firstDay 23:59:59.999999` with `duration = 180` and `error = 1`
     * - `secondDay 00:00:00.000000` with `duration = 360` and `error = 0`
     *
     * Keep all non-time grouping columns identical.
     *
     * Assert exactly 2 day buckets:
     * - `firstDay 00:00:00` has `cnt = 2`, `sum_duration = 360`, `avg_duration = 180`,
     *   `max_duration = 180`, `min_duration = 180`, `error_num = 1`,
     *   and all checked percentiles equal `180`
     * - `secondDay 00:00:00` has `cnt = 1`, `sum_duration = 360`, `avg_duration = 360`,
     *   `max_duration = 360`, `min_duration = 360`, `error_num = 0`,
     *   and all checked percentiles equal `360`
     *
     * This proves the day bucket interval is closed on the start and open on the end:
     * `[firstDay 00:00:00, secondDay 00:00:00)`.
     */
    @Test
    void dayEntryBucketBoundaryIsCorrect() {
        String testId = "entry_day_boundary_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime firstDay = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(2));

        OffsetDateTime secondDay = firstDay.plusDays(1);
        OffsetDateTime lastMicrosecondBeforeSecondDay = secondDay.minusNanos(1_000);

        // Arrange: keep all grouping dimensions identical and vary only event time. Two rows should
        // remain in the first day bucket, while the exact next-day row should start the next bucket.
        List<ServiceCallEntity> rows = List.of(
                entryRow(
                        testId + "_start",
                        serviceName,
                        callerServiceName,
                        firstDay,
                        180,
                        0),
                entryRow(
                        testId + "_last_microsecond",
                        serviceName,
                        callerServiceName,
                        lastMicrosecondBeforeSecondDay,
                        180,
                        1),
                entryRow(
                        testId + "_next_bucket",
                        serviceName,
                        callerServiceName,
                        secondDay,
                        360,
                        0)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        // Act: refresh minute and hour first because the day entry CAGG rolls up from hour data.
        OffsetDateTime refreshStart = firstDay.minusHours(1);
        OffsetDateTime refreshEnd = secondDay.plusDays(1);

        refreshContinuousAggregate("service_call_minute_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_5min_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_hour_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_day_entry", refreshStart, refreshEnd);

        // Assert: the boundary split itself is correct before checking derived metrics.
        assertBucketSplit(
                "service_call_day_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                firstDay,
                2,
                secondDay,
                1);

        // Assert: the two rows before the next day roll into the first day bucket.
        assertServiceCallAggregateRow(
                "service_call_day_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                firstDay,
                2,
                360,
                180,
                180,
                180,
                1);
        assertUniformPercentDurationRow(
                "service_call_day_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                firstDay,
                180);

        // Assert: the exact next-day row starts a new day bucket.
        assertServiceCallAggregateRow(
                "service_call_day_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                secondDay,
                1,
                360,
                360,
                360,
                360,
                0);
        assertUniformPercentDurationRow(
                "service_call_day_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                secondDay,
                360);
    }

    /**
     * Verifies dependency-side calculation and aggregation for the core scalar metrics.
     *
     * Starting from raw `service_call` rows with `span_kind = CLIENT`, this test checks that the
     * following dependency CAGGs are calculated correctly:
     * - `service_call_minute_dependency`
     * - `service_call_5min_dependency`
     * - `service_call_hour_dependency`
     * - `service_call_day_dependency`
     *
     * Fields checked on those dependency CAGGs:
     * - `cnt`
     * - `sum_duration`
     * - `avg_duration`
     * - `max_duration`
     * - `min_duration`
     * - `error_num`
     *
     * Insert rows at:
     * - `10:01:05` with `duration = 100` and `error = 0`
     * - `10:01:20` with `duration = 200` and `error = 1`
     * - `10:04:10` with `duration = 300` and `error = 0`
     * - `10:06:40` with `duration = 500` and `error = 1`
     *
     * Keep the service and caller dimensions identical so only the bucket timestamp changes.
     *
     * Assert minute dependency buckets:
     * - `10:01` has `cnt = 2`, `sum_duration = 300`, `avg_duration = 150`,
     *   `max_duration = 200`, `min_duration = 100`, and `error_num = 1`
     * - `10:04` has `cnt = 1`, `sum_duration = 300`, `avg_duration = 300`,
     *   `max_duration = 300`, `min_duration = 300`, and `error_num = 0`
     * - `10:06` has `cnt = 1`, `sum_duration = 500`, `avg_duration = 500`,
     *   `max_duration = 500`, `min_duration = 500`, and `error_num = 1`
     *
     * Assert wider dependency rollup buckets:
     * - 5-minute bucket `10:00` has `cnt = 3`, `sum_duration = 600`,
     *   `avg_duration = 200`, `max_duration = 300`, `min_duration = 100`, and `error_num = 1`
     * - 5-minute bucket `10:05` has `cnt = 1`, `sum_duration = 500`,
     *   `avg_duration = 500`, `max_duration = 500`, `min_duration = 500`, and `error_num = 1`
     * - hour bucket `10:00` has `cnt = 4`, `sum_duration = 1100`,
     *   `avg_duration = 275`, `max_duration = 500`, `min_duration = 100`, and `error_num = 2`
     * - day bucket has `cnt = 4`, `sum_duration = 1100`, `avg_duration = 275`,
     *   `max_duration = 500`, `min_duration = 100`, and `error_num = 2`
     */
    @Test
    void dependencyRollupsCalculateExpectedNumericMetrics() {
        String testId = "dependency_calc_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime minute101 = dayStart.plusHours(10).plusMinutes(1);
        OffsetDateTime minute104 = dayStart.plusHours(10).plusMinutes(4);
        OffsetDateTime minute106 = dayStart.plusHours(10).plusMinutes(6);

        List<ServiceCallEntity> rows = List.of(
                dependencyRow(testId, serviceName, callerServiceName, minute101.plusSeconds(5), 100, 0),
                dependencyRow(testId, serviceName, callerServiceName, minute101.plusSeconds(20), 200, 1),
                dependencyRow(testId, serviceName, callerServiceName, minute104.plusSeconds(10), 300, 0),
                dependencyRow(testId, serviceName, callerServiceName, minute106.plusSeconds(40), 500, 1)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        OffsetDateTime refreshStart = dayStart;
        OffsetDateTime refreshEnd = dayStart.plusDays(1);

        refreshContinuousAggregate("service_call_minute_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_5min_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_hour_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_day_dependency", refreshStart, refreshEnd);

        assertServiceCallAggregateRow(
                "service_call_minute_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                minute101,
                2,
                300,
                150,
                200,
                100,
                1);
        assertServiceCallAggregateRow(
                "service_call_minute_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                minute104,
                1,
                300,
                300,
                300,
                300,
                0);
        assertServiceCallAggregateRow(
                "service_call_minute_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                minute106,
                1,
                500,
                500,
                500,
                500,
                1);

        assertServiceCallAggregateRow(
                "service_call_5min_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                dayStart.plusHours(10),
                3,
                600,
                200,
                300,
                100,
                1);
        assertServiceCallAggregateRow(
                "service_call_5min_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                dayStart.plusHours(10).plusMinutes(5),
                1,
                500,
                500,
                500,
                500,
                1);

        assertServiceCallAggregateRow(
                "service_call_hour_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                dayStart.plusHours(10),
                4,
                1100,
                275,
                500,
                100,
                2);
        assertServiceCallAggregateRow(
                "service_call_day_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                dayStart,
                4,
                1100,
                275,
                500,
                100,
                2);
    }

    /**
     * Verifies dependency-side percentile rollups through the Toolkit percentile accessors.
     *
     * Starting from raw `service_call` rows with `span_kind = CLIENT`, this test checks:
     * - `service_call_minute_dependency`
     * - `service_call_5min_dependency`
     * - `service_call_hour_dependency`
     * - `service_call_day_dependency`
     *
     * Fields checked on those dependency CAGGs:
     * - `approx_percentile(0.90, percent_duration)`
     * - `approx_percentile(0.95, percent_duration)`
     * - `approx_percentile(0.99, percent_duration)`
     *
     * Insert 300 rows across 3 minute buckets:
     * - `10:01`
     * - `10:02`
     * - `10:04`
     *
     * Each minute bucket receives 100 rows with steadily increasing durations:
     * - `100, 200, ..., 10000`
     *
     * The three minute buckets collapse into one 5-minute bucket, one hour bucket, and one day
     * bucket. This keeps the source distribution deterministic while proving the percentile sketch
     * is preserved through each dependency rollup layer.
     *
     * Assert on every checked relation:
     * - p90 lands in the expected high-percentile range
     * - p95 lands above p90 in the expected range
     * - p99 lands above p95 near the top of the distribution
     * - the p90/p95/p99 gaps stay meaningfully separated
     */
    @Test
    void dependencyRollupsExposePercentDuration() {
        String testId = "dependency_percent_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime minute101 = dayStart.plusHours(10).plusMinutes(1);
        OffsetDateTime minute102 = dayStart.plusHours(10).plusMinutes(2);
        OffsetDateTime minute104 = dayStart.plusHours(10).plusMinutes(4);
        EntryFixtureDimensions dimensions = dependencyDimensions(serviceName, callerServiceName);

        List<ServiceCallEntity> rows = new ArrayList<>();
        addPercentileFixtureRows(rows, testId + "_101", dimensions, minute101);
        addPercentileFixtureRows(rows, testId + "_102", dimensions, minute102);
        addPercentileFixtureRows(rows, testId + "_104", dimensions, minute104);

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        OffsetDateTime refreshStart = dayStart;
        OffsetDateTime refreshEnd = dayStart.plusDays(1);

        refreshContinuousAggregate("service_call_minute_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_5min_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_hour_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_day_dependency", refreshStart, refreshEnd);

        assertPercentDurationRow("service_call_minute_dependency", serviceName, SpanKindEnum.CLIENT.getValue(), minute101);
        assertPercentDurationRow("service_call_minute_dependency", serviceName, SpanKindEnum.CLIENT.getValue(), minute102);
        assertPercentDurationRow("service_call_minute_dependency", serviceName, SpanKindEnum.CLIENT.getValue(), minute104);
        assertPercentDurationRow(
                "service_call_5min_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                dayStart.plusHours(10));
        assertPercentDurationRow(
                "service_call_hour_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                dayStart.plusHours(10));
        assertPercentDurationRow(
                "service_call_day_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                dayStart);
    }

    /**
     * Verifies that dependency unified views expose both recent hot data and older archived data.
     *
     * Starting from raw `service_call` rows with `span_kind = CLIENT`, this test checks:
     * - `service_call_minute_dependency_unified`
     * - `service_call_5min_dependency_unified`
     * - `service_call_hour_dependency_unified`
     *
     * Fields checked on those dependency unified views:
     * - row count per unified view
     * - `cnt`
     * - `sum_duration`
     * - `avg_duration`
     * - `max_duration`
     * - `min_duration`
     * - `error_num`
     * - `approx_percentile(0.90, percent_duration)`
     * - `approx_percentile(0.95, percent_duration)`
     * - `approx_percentile(0.99, percent_duration)`
     *
     * Fields checked on minute-layer dependency objects:
     * - `service_call_minute_dependency.last_insert_time`
     * - `service_call_minute_dependency_merged.last_insert_time`
     * - `service_call_minute_dependency_unified.last_insert_time`
     *
     * Insert rows into two time regions:
     * - recent hot bucket around `now() - 3 hours` with `duration = 210` and `error = 0`
     * - archived merged bucket around `now() - 20 days` with `duration = 420` and `error = 1`
     *
     * Put 2 rows in each bucket and use increasing `update_time` values so
     * `last_insert_time = max(update_time)` is deterministic.
     *
     * Assert each dependency unified view returns exactly 2 rows for the test service:
     * - one recent hot bucket
     * - one archived merged bucket
     *
     * Assert the merged CAGGs directly before reading through unified views:
     * - `service_call_minute_dependency_merged`
     * - `service_call_5min_dependency_merged`
     * - `service_call_hour_dependency_merged`
     *
     * This proves the dependency unified views cross the hot/archive boundary correctly while
     * keeping scalar aggregates, percentile sketches, and minute-layer `last_insert_time` intact.
     */
    @Test
    void dependencyUnifiedViewsExposeRecentAndArchivedBuckets() {
        String testId = "dependency_unified_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;

        OffsetDateTime nowUtc = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime recentTime = nowUtc.minusHours(3).withSecond(15).withNano(0);
        OffsetDateTime archivedTime = nowUtc.minusDays(20).withHour(4).withMinute(12).withSecond(20).withNano(0);
        OffsetDateTime recentLastInsertTime = recentTime.plusSeconds(22);
        OffsetDateTime archivedLastInsertTime = archivedTime.plusSeconds(22);

        List<ServiceCallEntity> rows = List.of(
                dependencyRow(
                        testId + "_recent_1",
                        serviceName,
                        callerServiceName,
                        recentTime,
                        210,
                        0,
                        recentTime.plusSeconds(11)),
                dependencyRow(
                        testId + "_recent_2",
                        serviceName,
                        callerServiceName,
                        recentTime.plusSeconds(10),
                        210,
                        0,
                        recentLastInsertTime),
                dependencyRow(
                        testId + "_archive_1",
                        serviceName,
                        callerServiceName,
                        archivedTime,
                        420,
                        1,
                        archivedTime.plusSeconds(11)),
                dependencyRow(
                        testId + "_archive_2",
                        serviceName,
                        callerServiceName,
                        archivedTime.plusSeconds(10),
                        420,
                        1,
                        archivedLastInsertTime)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        OffsetDateTime refreshStart = startOfUtcDay(archivedTime);
        OffsetDateTime refreshEnd = startOfUtcDay(recentTime).plusDays(1);

        refreshContinuousAggregate("service_call_minute_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_minute_dependency_merged", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_5min_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_5min_dependency_merged", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_hour_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_hour_dependency_merged", refreshStart, refreshEnd);

        OffsetDateTime recentMinuteBucket = recentTime.withSecond(0).withNano(0);
        OffsetDateTime archivedMinuteBucket = archivedTime.withSecond(0).withNano(0);
        OffsetDateTime recent5MinBucket = floorToFiveMinutes(recentTime);
        OffsetDateTime archived5MinBucket = floorToFiveMinutes(archivedTime);
        OffsetDateTime recentHourBucket = recentTime.withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime archivedHourBucket = archivedTime.withMinute(0).withSecond(0).withNano(0);

        assertViewRowCount("service_call_minute_dependency_unified", serviceName, 2);
        assertViewRowCount("service_call_5min_dependency_unified", serviceName, 2);
        assertViewRowCount("service_call_hour_dependency_unified", serviceName, 2);

        assertLastInsertTime(
                "service_call_minute_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                recentMinuteBucket,
                recentLastInsertTime);
        assertLastInsertTime(
                "service_call_minute_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                archivedMinuteBucket,
                archivedLastInsertTime);
        assertLastInsertTime(
                "service_call_minute_dependency_merged",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                recentMinuteBucket,
                recentLastInsertTime);
        assertLastInsertTime(
                "service_call_minute_dependency_merged",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                archivedMinuteBucket,
                archivedLastInsertTime);
        assertLastInsertTime(
                "service_call_minute_dependency_unified",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                recentMinuteBucket,
                recentLastInsertTime);
        assertLastInsertTime(
                "service_call_minute_dependency_unified",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                archivedMinuteBucket,
                archivedLastInsertTime);

        assertServiceCallAggregateRow(
                "service_call_minute_dependency_merged",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                archivedMinuteBucket,
                2,
                840,
                420,
                420,
                420,
                2);
        assertUniformPercentDurationRow(
                "service_call_minute_dependency_merged",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                archivedMinuteBucket,
                420);
        assertServiceCallAggregateRow(
                "service_call_5min_dependency_merged",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                archived5MinBucket,
                2,
                840,
                420,
                420,
                420,
                2);
        assertUniformPercentDurationRow(
                "service_call_5min_dependency_merged",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                archived5MinBucket,
                420);
        assertServiceCallAggregateRow(
                "service_call_hour_dependency_merged",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                archivedHourBucket,
                2,
                840,
                420,
                420,
                420,
                2);
        assertUniformPercentDurationRow(
                "service_call_hour_dependency_merged",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                archivedHourBucket,
                420);

        assertServiceCallAggregateRow(
                "service_call_minute_dependency_unified",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                recentMinuteBucket,
                2,
                420,
                210,
                210,
                210,
                0);
        assertServiceCallAggregateRow(
                "service_call_minute_dependency_unified",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                archivedMinuteBucket,
                2,
                840,
                420,
                420,
                420,
                2);
        assertUniformPercentDurationRow(
                "service_call_minute_dependency_unified",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                recentMinuteBucket,
                210);
        assertUniformPercentDurationRow(
                "service_call_minute_dependency_unified",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                archivedMinuteBucket,
                420);

        assertServiceCallAggregateRow(
                "service_call_5min_dependency_unified",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                recent5MinBucket,
                2,
                420,
                210,
                210,
                210,
                0);
        assertServiceCallAggregateRow(
                "service_call_5min_dependency_unified",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                archived5MinBucket,
                2,
                840,
                420,
                420,
                420,
                2);
        assertUniformPercentDurationRow(
                "service_call_5min_dependency_unified",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                recent5MinBucket,
                210);
        assertUniformPercentDurationRow(
                "service_call_5min_dependency_unified",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                archived5MinBucket,
                420);

        assertServiceCallAggregateRow(
                "service_call_hour_dependency_unified",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                recentHourBucket,
                2,
                420,
                210,
                210,
                210,
                0);
        assertServiceCallAggregateRow(
                "service_call_hour_dependency_unified",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                archivedHourBucket,
                2,
                840,
                420,
                420,
                420,
                2);
        assertUniformPercentDurationRow(
                "service_call_hour_dependency_unified",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                recentHourBucket,
                210);
        assertUniformPercentDurationRow(
                "service_call_hour_dependency_unified",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                archivedHourBucket,
                420);
    }

    /**
     * Verifies the exact 1-minute `time_bucket` boundary behavior for dependency CAGGs.
     *
     * Starting from raw `service_call` rows with `span_kind = CLIENT`, this test checks:
     * - `service_call_minute_dependency`
     *
     * Fields checked on the minute dependency CAGG:
     * - bucket `time`
     * - `cnt`
     * - `sum_duration`
     * - `avg_duration`
     * - `max_duration`
     * - `min_duration`
     * - `error_num`
     * - `approx_percentile(0.90, percent_duration)`
     * - `approx_percentile(0.95, percent_duration)`
     * - `approx_percentile(0.99, percent_duration)`
     * - `last_insert_time`
     *
     * Insert rows at:
     * - `10:01:00.000000` with `duration = 180` and `error = 0`
     * - `10:01:59.999999` with `duration = 180` and `error = 1`
     * - `10:02:00.000000` with `duration = 360` and `error = 0`
     *
     * Keep all non-time grouping columns identical.
     *
     * Assert exactly 2 minute buckets:
     * - `10:01:00` has `cnt = 2`, `sum_duration = 360`, `avg_duration = 180`,
     *   `max_duration = 180`, `min_duration = 180`, `error_num = 1`,
     *   all checked percentiles equal `180`, and `last_insert_time = 10:02:00`
     * - `10:02:00` has `cnt = 1`, `sum_duration = 360`, `avg_duration = 360`,
     *   `max_duration = 360`, `min_duration = 360`, `error_num = 0`,
     *   all checked percentiles equal `360`, and `last_insert_time = 10:02:11`
     *
     * This proves the bucket interval is closed on the start and open on the end:
     * `[10:01:00, 10:02:00)`.
     */
    @Test
    void minuteDependencyBucketBoundaryIsCorrect() {
        String testId = "dependency_minute_boundary_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime minute101 = dayStart.plusHours(10).plusMinutes(1);
        OffsetDateTime minute102 = minute101.plusMinutes(1);
        OffsetDateTime lastMicrosecondBefore102 = minute102.minusNanos(1_000);
        OffsetDateTime minute101LastInsertTime = minute102;
        OffsetDateTime minute102LastInsertTime = minute102.plusSeconds(11);

        List<ServiceCallEntity> rows = List.of(
                dependencyRow(
                        testId + "_start",
                        serviceName,
                        callerServiceName,
                        minute101,
                        180,
                        0,
                        minute101.plusSeconds(10)),
                dependencyRow(
                        testId + "_last_microsecond",
                        serviceName,
                        callerServiceName,
                        lastMicrosecondBefore102,
                        180,
                        1,
                        minute101LastInsertTime),
                dependencyRow(
                        testId + "_next_bucket",
                        serviceName,
                        callerServiceName,
                        minute102,
                        360,
                        0,
                        minute102LastInsertTime)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        refreshContinuousAggregate("service_call_minute_dependency", minute101.minusMinutes(1), minute102.plusMinutes(1));

        assertBucketSplit(
                "service_call_minute_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                minute101,
                2,
                minute102,
                1);

        assertServiceCallAggregateRow(
                "service_call_minute_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                minute101,
                2,
                360,
                180,
                180,
                180,
                1);
        assertUniformPercentDurationRow(
                "service_call_minute_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                minute101,
                180);
        assertLastInsertTime(
                "service_call_minute_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                minute101,
                minute101LastInsertTime);

        assertServiceCallAggregateRow(
                "service_call_minute_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                minute102,
                1,
                360,
                360,
                360,
                360,
                0);
        assertUniformPercentDurationRow(
                "service_call_minute_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                minute102,
                360);
        assertLastInsertTime(
                "service_call_minute_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                minute102,
                minute102LastInsertTime);
    }

    /**
     * Verifies the exact 5-minute `time_bucket` boundary behavior for dependency CAGGs.
     *
     * Starting from raw `service_call` rows with `span_kind = CLIENT`, this test checks:
     * - `service_call_5min_dependency`
     *
     * Fields checked on the 5-minute dependency CAGG:
     * - bucket `time`
     * - `cnt`
     * - `sum_duration`
     * - `avg_duration`
     * - `max_duration`
     * - `min_duration`
     * - `error_num`
     * - `approx_percentile(0.90, percent_duration)`
     * - `approx_percentile(0.95, percent_duration)`
     * - `approx_percentile(0.99, percent_duration)`
     *
     * Insert rows at:
     * - `10:00:00.000000` with `duration = 180` and `error = 0`
     * - `10:04:59.999999` with `duration = 180` and `error = 1`
     * - `10:05:00.000000` with `duration = 360` and `error = 0`
     *
     * Keep all non-time grouping columns identical.
     *
     * Assert exactly 2 five-minute buckets:
     * - `10:00:00` has `cnt = 2`, `sum_duration = 360`, `avg_duration = 180`,
     *   `max_duration = 180`, `min_duration = 180`, `error_num = 1`,
     *   and all checked percentiles equal `180`
     * - `10:05:00` has `cnt = 1`, `sum_duration = 360`, `avg_duration = 360`,
     *   `max_duration = 360`, `min_duration = 360`, `error_num = 0`,
     *   and all checked percentiles equal `360`
     *
     * This proves the 5-minute bucket interval is closed on the start and open on the end:
     * `[10:00:00, 10:05:00)`.
     */
    @Test
    void fiveMinuteDependencyBucketBoundaryIsCorrect() {
        String testId = "dependency_5min_boundary_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime fiveMin1000 = dayStart.plusHours(10);
        OffsetDateTime fiveMin1005 = fiveMin1000.plusMinutes(5);
        OffsetDateTime lastMicrosecondBefore1005 = fiveMin1005.minusNanos(1_000);

        List<ServiceCallEntity> rows = List.of(
                dependencyRow(
                        testId + "_start",
                        serviceName,
                        callerServiceName,
                        fiveMin1000,
                        180,
                        0),
                dependencyRow(
                        testId + "_last_microsecond",
                        serviceName,
                        callerServiceName,
                        lastMicrosecondBefore1005,
                        180,
                        1),
                dependencyRow(
                        testId + "_next_bucket",
                        serviceName,
                        callerServiceName,
                        fiveMin1005,
                        360,
                        0)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        OffsetDateTime refreshStart = fiveMin1000.minusMinutes(1);
        OffsetDateTime refreshEnd = fiveMin1005.plusMinutes(5);

        refreshContinuousAggregate("service_call_minute_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_5min_dependency", refreshStart, refreshEnd);

        assertBucketSplit(
                "service_call_5min_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                fiveMin1000,
                2,
                fiveMin1005,
                1);

        assertServiceCallAggregateRow(
                "service_call_5min_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                fiveMin1000,
                2,
                360,
                180,
                180,
                180,
                1);
        assertUniformPercentDurationRow(
                "service_call_5min_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                fiveMin1000,
                180);

        assertServiceCallAggregateRow(
                "service_call_5min_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                fiveMin1005,
                1,
                360,
                360,
                360,
                360,
                0);
        assertUniformPercentDurationRow(
                "service_call_5min_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                fiveMin1005,
                360);
    }

    /**
     * Verifies the exact 1-hour `time_bucket` boundary behavior for dependency CAGGs.
     *
     * Starting from raw `service_call` rows with `span_kind = CLIENT`, this test checks:
     * - `service_call_hour_dependency`
     *
     * Fields checked on the hour dependency CAGG:
     * - bucket `time`
     * - `cnt`
     * - `sum_duration`
     * - `avg_duration`
     * - `max_duration`
     * - `min_duration`
     * - `error_num`
     * - `approx_percentile(0.90, percent_duration)`
     * - `approx_percentile(0.95, percent_duration)`
     * - `approx_percentile(0.99, percent_duration)`
     *
     * Insert rows at:
     * - `10:00:00.000000` with `duration = 180` and `error = 0`
     * - `10:59:59.999999` with `duration = 180` and `error = 1`
     * - `11:00:00.000000` with `duration = 360` and `error = 0`
     *
     * Keep all non-time grouping columns identical.
     *
     * Assert exactly 2 hour buckets:
     * - `10:00:00` has `cnt = 2`, `sum_duration = 360`, `avg_duration = 180`,
     *   `max_duration = 180`, `min_duration = 180`, `error_num = 1`,
     *   and all checked percentiles equal `180`
     * - `11:00:00` has `cnt = 1`, `sum_duration = 360`, `avg_duration = 360`,
     *   `max_duration = 360`, `min_duration = 360`, `error_num = 0`,
     *   and all checked percentiles equal `360`
     *
     * This proves the hour bucket interval is closed on the start and open on the end:
     * `[10:00:00, 11:00:00)`.
     */
    @Test
    void hourDependencyBucketBoundaryIsCorrect() {
        String testId = "dependency_hour_boundary_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime hour10 = dayStart.plusHours(10);
        OffsetDateTime hour11 = hour10.plusHours(1);
        OffsetDateTime lastMicrosecondBefore11 = hour11.minusNanos(1_000);

        List<ServiceCallEntity> rows = List.of(
                dependencyRow(
                        testId + "_start",
                        serviceName,
                        callerServiceName,
                        hour10,
                        180,
                        0),
                dependencyRow(
                        testId + "_last_microsecond",
                        serviceName,
                        callerServiceName,
                        lastMicrosecondBefore11,
                        180,
                        1),
                dependencyRow(
                        testId + "_next_bucket",
                        serviceName,
                        callerServiceName,
                        hour11,
                        360,
                        0)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        OffsetDateTime refreshStart = hour10.minusHours(1);
        OffsetDateTime refreshEnd = hour11.plusHours(1);

        refreshContinuousAggregate("service_call_minute_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_5min_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_hour_dependency", refreshStart, refreshEnd);

        assertBucketSplit(
                "service_call_hour_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                hour10,
                2,
                hour11,
                1);

        assertServiceCallAggregateRow(
                "service_call_hour_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                hour10,
                2,
                360,
                180,
                180,
                180,
                1);
        assertUniformPercentDurationRow(
                "service_call_hour_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                hour10,
                180);

        assertServiceCallAggregateRow(
                "service_call_hour_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                hour11,
                1,
                360,
                360,
                360,
                360,
                0);
        assertUniformPercentDurationRow(
                "service_call_hour_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                hour11,
                360);
    }

    /**
     * Verifies the exact 1-day `time_bucket` boundary behavior for dependency CAGGs.
     *
     * Starting from raw `service_call` rows with `span_kind = CLIENT`, this test checks:
     * - `service_call_day_dependency`
     *
     * Fields checked on the day dependency CAGG:
     * - bucket `time`
     * - `cnt`
     * - `sum_duration`
     * - `avg_duration`
     * - `max_duration`
     * - `min_duration`
     * - `error_num`
     * - `approx_percentile(0.90, percent_duration)`
     * - `approx_percentile(0.95, percent_duration)`
     * - `approx_percentile(0.99, percent_duration)`
     *
     * Insert rows at:
     * - `firstDay 00:00:00.000000` with `duration = 180` and `error = 0`
     * - `firstDay 23:59:59.999999` with `duration = 180` and `error = 1`
     * - `secondDay 00:00:00.000000` with `duration = 360` and `error = 0`
     *
     * Keep all non-time grouping columns identical.
     *
     * Assert exactly 2 day buckets:
     * - `firstDay 00:00:00` has `cnt = 2`, `sum_duration = 360`, `avg_duration = 180`,
     *   `max_duration = 180`, `min_duration = 180`, `error_num = 1`,
     *   and all checked percentiles equal `180`
     * - `secondDay 00:00:00` has `cnt = 1`, `sum_duration = 360`, `avg_duration = 360`,
     *   `max_duration = 360`, `min_duration = 360`, `error_num = 0`,
     *   and all checked percentiles equal `360`
     *
     * This proves the day bucket interval is closed on the start and open on the end:
     * `[firstDay 00:00:00, secondDay 00:00:00)`.
     */
    @Test
    void dayDependencyBucketBoundaryIsCorrect() {
        String testId = "dependency_day_boundary_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime firstDay = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(2));

        OffsetDateTime secondDay = firstDay.plusDays(1);
        OffsetDateTime lastMicrosecondBeforeSecondDay = secondDay.minusNanos(1_000);

        List<ServiceCallEntity> rows = List.of(
                dependencyRow(
                        testId + "_start",
                        serviceName,
                        callerServiceName,
                        firstDay,
                        180,
                        0),
                dependencyRow(
                        testId + "_last_microsecond",
                        serviceName,
                        callerServiceName,
                        lastMicrosecondBeforeSecondDay,
                        180,
                        1),
                dependencyRow(
                        testId + "_next_bucket",
                        serviceName,
                        callerServiceName,
                        secondDay,
                        360,
                        0)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        OffsetDateTime refreshStart = firstDay.minusHours(1);
        OffsetDateTime refreshEnd = secondDay.plusDays(1);

        refreshContinuousAggregate("service_call_minute_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_5min_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_hour_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_day_dependency", refreshStart, refreshEnd);

        assertBucketSplit(
                "service_call_day_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                firstDay,
                2,
                secondDay,
                1);

        assertServiceCallAggregateRow(
                "service_call_day_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                firstDay,
                2,
                360,
                180,
                180,
                180,
                1);
        assertUniformPercentDurationRow(
                "service_call_day_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                firstDay,
                180);

        assertServiceCallAggregateRow(
                "service_call_day_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                secondDay,
                1,
                360,
                360,
                360,
                360,
                0);
        assertUniformPercentDurationRow(
                "service_call_day_dependency",
                serviceName,
                SpanKindEnum.CLIENT.getValue(),
                secondDay,
                360);
    }

    /**
     * Verifies entry-side host-list rollups preserve the `host_name` grouping.
     *
     * Starting from raw `service_call` rows with `span_kind = SERVER`, this test checks:
     * - `service_host_list_minute_entry`
     * - `service_host_list_5min_entry`
     * - `service_host_list_hour_entry`
     * - `service_host_list_day_entry`
     *
     * Fields checked on those host-list CAGGs:
     * - bucket `time`
     * - `service_name`
     * - `span_kind`
     * - `host_name`
     * - `cnt`
     *
     * Insert rows for one service and two different hosts:
     * - host A at `10:01:05`, `10:01:20`, `10:04:10`, and `10:06:40`
     * - host B at `10:01:25` and `10:06:50`
     *
     * Assert host A and host B keep separate counts at every rollup level:
     * - minute buckets preserve the raw per-host counts
     * - 5-minute buckets split at `10:00` and `10:05`
     * - hour and day buckets keep each host separate
     *
     * This proves the higher-grain host-list CAGGs do not collapse different hosts for the same
     * service and span kind.
     */
    @Test
    void hostListEntryRollupsCalculateExpectedCounts() {
        String testId = "host_list_calc_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        String hostA = "host_a_" + testId;
        String hostB = "host_b_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime minute101 = dayStart.plusHours(10).plusMinutes(1);
        OffsetDateTime minute104 = dayStart.plusHours(10).plusMinutes(4);
        OffsetDateTime minute106 = dayStart.plusHours(10).plusMinutes(6);
        EntryFixtureDimensions hostADimensions = entryDimensions(serviceName, callerServiceName, hostA);
        EntryFixtureDimensions hostBDimensions = entryDimensions(serviceName, callerServiceName, hostB);

        List<ServiceCallEntity> rows = List.of(
                entryRow(testId + "_a_101_1", hostADimensions, minute101.plusSeconds(5), 100, 0),
                entryRow(testId + "_a_101_2", hostADimensions, minute101.plusSeconds(20), 200, 1),
                entryRow(testId + "_a_104", hostADimensions, minute104.plusSeconds(10), 300, 0),
                entryRow(testId + "_a_106", hostADimensions, minute106.plusSeconds(40), 500, 1),
                entryRow(testId + "_b_101", hostBDimensions, minute101.plusSeconds(25), 700, 0),
                entryRow(testId + "_b_106", hostBDimensions, minute106.plusSeconds(50), 900, 1)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        OffsetDateTime refreshStart = dayStart;
        OffsetDateTime refreshEnd = dayStart.plusDays(1);

        refreshContinuousAggregate("service_host_list_minute_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_host_list_5min_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_host_list_hour_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_host_list_day_entry", refreshStart, refreshEnd);

        assertHostListAggregateRow("service_host_list_minute_entry", serviceName, SpanKindEnum.SERVER.getValue(), hostA, minute101, 2);
        assertHostListAggregateRow("service_host_list_minute_entry", serviceName, SpanKindEnum.SERVER.getValue(), hostA, minute104, 1);
        assertHostListAggregateRow("service_host_list_minute_entry", serviceName, SpanKindEnum.SERVER.getValue(), hostA, minute106, 1);
        assertHostListAggregateRow("service_host_list_minute_entry", serviceName, SpanKindEnum.SERVER.getValue(), hostB, minute101, 1);
        assertHostListAggregateRow("service_host_list_minute_entry", serviceName, SpanKindEnum.SERVER.getValue(), hostB, minute106, 1);

        assertHostListAggregateRow(
                "service_host_list_5min_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                hostA,
                dayStart.plusHours(10),
                3);
        assertHostListAggregateRow(
                "service_host_list_5min_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                hostA,
                dayStart.plusHours(10).plusMinutes(5),
                1);
        assertHostListAggregateRow(
                "service_host_list_5min_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                hostB,
                dayStart.plusHours(10),
                1);
        assertHostListAggregateRow(
                "service_host_list_5min_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                hostB,
                dayStart.plusHours(10).plusMinutes(5),
                1);

        assertHostListAggregateRow(
                "service_host_list_hour_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                hostA,
                dayStart.plusHours(10),
                4);
        assertHostListAggregateRow(
                "service_host_list_hour_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                hostB,
                dayStart.plusHours(10),
                2);
        assertHostListAggregateRow(
                "service_host_list_day_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                hostA,
                dayStart,
                4);
        assertHostListAggregateRow(
                "service_host_list_day_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                hostB,
                dayStart,
                2);
    }

    /**
     * Verifies the exact 1-minute `time_bucket` boundary behavior for entry host-list CAGGs.
     *
     * Starting from raw `service_call` rows with `span_kind = SERVER`, this test checks:
     * - `service_host_list_minute_entry`
     *
     * Insert rows for one service and host at:
     * - `10:01:00.000000`
     * - `10:01:59.999999`
     * - `10:02:00.000000`
     *
     * Keep `service_name`, `span_kind`, and `host_name` identical.
     *
     * Assert exactly 2 minute buckets:
     * - `10:01:00` has `cnt = 2`
     * - `10:02:00` has `cnt = 1`
     *
     * This proves the host-list minute bucket interval is closed on the start and open on the end:
     * `[10:01:00, 10:02:00)`.
     */
    @Test
    void hostListMinuteEntryBucketBoundaryIsCorrect() {
        String testId = "host_list_minute_boundary_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime minute101 = dayStart.plusHours(10).plusMinutes(1);
        OffsetDateTime minute102 = minute101.plusMinutes(1);
        OffsetDateTime lastMicrosecondBefore102 = minute102.minusNanos(1_000);

        List<ServiceCallEntity> rows = List.of(
                entryRow(testId + "_start", serviceName, callerServiceName, minute101, 180, 0),
                entryRow(testId + "_last_microsecond", serviceName, callerServiceName, lastMicrosecondBefore102, 180, 1),
                entryRow(testId + "_next_bucket", serviceName, callerServiceName, minute102, 360, 0)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        refreshContinuousAggregate("service_host_list_minute_entry", minute101.minusMinutes(1), minute102.plusMinutes(1));

        assertHostListBucketSplit(
                "service_host_list_minute_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                "host_" + serviceName,
                minute101,
                2,
                minute102,
                1);
    }

    /**
     * Verifies the exact 5-minute `time_bucket` boundary behavior for entry host-list CAGGs.
     *
     * Starting from raw `service_call` rows with `span_kind = SERVER`, this test checks:
     * - `service_host_list_5min_entry`
     *
     * Insert rows for one service and host at:
     * - `10:00:00.000000`
     * - `10:04:59.999999`
     * - `10:05:00.000000`
     *
     * Keep `service_name`, `span_kind`, and `host_name` identical.
     *
     * Assert exactly 2 five-minute buckets:
     * - `10:00:00` has `cnt = 2`
     * - `10:05:00` has `cnt = 1`
     *
     * This proves the host-list 5-minute bucket interval is closed on the start and open on the end:
     * `[10:00:00, 10:05:00)`.
     */
    @Test
    void hostListFiveMinuteEntryBucketBoundaryIsCorrect() {
        String testId = "host_list_5min_boundary_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime fiveMin1000 = dayStart.plusHours(10);
        OffsetDateTime fiveMin1005 = fiveMin1000.plusMinutes(5);
        OffsetDateTime lastMicrosecondBefore1005 = fiveMin1005.minusNanos(1_000);

        List<ServiceCallEntity> rows = List.of(
                entryRow(testId + "_start", serviceName, callerServiceName, fiveMin1000, 180, 0),
                entryRow(testId + "_last_microsecond", serviceName, callerServiceName, lastMicrosecondBefore1005, 180, 1),
                entryRow(testId + "_next_bucket", serviceName, callerServiceName, fiveMin1005, 360, 0)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        OffsetDateTime refreshStart = fiveMin1000.minusMinutes(1);
        OffsetDateTime refreshEnd = fiveMin1005.plusMinutes(5);

        refreshContinuousAggregate("service_host_list_minute_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_host_list_5min_entry", refreshStart, refreshEnd);

        assertHostListBucketSplit(
                "service_host_list_5min_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                "host_" + serviceName,
                fiveMin1000,
                2,
                fiveMin1005,
                1);
    }

    /**
     * Verifies the exact 1-hour `time_bucket` boundary behavior for entry host-list CAGGs.
     *
     * Starting from raw `service_call` rows with `span_kind = SERVER`, this test checks:
     * - `service_host_list_hour_entry`
     *
     * Insert rows for one service and host at:
     * - `10:00:00.000000`
     * - `10:59:59.999999`
     * - `11:00:00.000000`
     *
     * Keep `service_name`, `span_kind`, and `host_name` identical.
     *
     * Assert exactly 2 hour buckets:
     * - `10:00:00` has `cnt = 2`
     * - `11:00:00` has `cnt = 1`
     *
     * This proves the host-list hour bucket interval is closed on the start and open on the end:
     * `[10:00:00, 11:00:00)`.
     */
    @Test
    void hostListHourEntryBucketBoundaryIsCorrect() {
        String testId = "host_list_hour_boundary_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime hour10 = dayStart.plusHours(10);
        OffsetDateTime hour11 = hour10.plusHours(1);
        OffsetDateTime lastMicrosecondBefore11 = hour11.minusNanos(1_000);

        List<ServiceCallEntity> rows = List.of(
                entryRow(testId + "_start", serviceName, callerServiceName, hour10, 180, 0),
                entryRow(testId + "_last_microsecond", serviceName, callerServiceName, lastMicrosecondBefore11, 180, 1),
                entryRow(testId + "_next_bucket", serviceName, callerServiceName, hour11, 360, 0)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        OffsetDateTime refreshStart = hour10.minusHours(1);
        OffsetDateTime refreshEnd = hour11.plusHours(1);

        refreshContinuousAggregate("service_host_list_minute_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_host_list_5min_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_host_list_hour_entry", refreshStart, refreshEnd);

        assertHostListBucketSplit(
                "service_host_list_hour_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                "host_" + serviceName,
                hour10,
                2,
                hour11,
                1);
    }

    /**
     * Verifies the exact 1-day `time_bucket` boundary behavior for entry host-list CAGGs.
     *
     * Starting from raw `service_call` rows with `span_kind = SERVER`, this test checks:
     * - `service_host_list_day_entry`
     *
     * Insert rows for one service and host at:
     * - `firstDay 00:00:00.000000`
     * - `firstDay 23:59:59.999999`
     * - `secondDay 00:00:00.000000`
     *
     * Keep `service_name`, `span_kind`, and `host_name` identical.
     *
     * Assert exactly 2 day buckets:
     * - `firstDay 00:00:00` has `cnt = 2`
     * - `secondDay 00:00:00` has `cnt = 1`
     *
     * This proves the host-list day bucket interval is closed on the start and open on the end:
     * `[firstDay 00:00:00, secondDay 00:00:00)`.
     */
    @Test
    void hostListDayEntryBucketBoundaryIsCorrect() {
        String testId = "host_list_day_boundary_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime firstDay = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(2));

        OffsetDateTime secondDay = firstDay.plusDays(1);
        OffsetDateTime lastMicrosecondBeforeSecondDay = secondDay.minusNanos(1_000);

        List<ServiceCallEntity> rows = List.of(
                entryRow(testId + "_start", serviceName, callerServiceName, firstDay, 180, 0),
                entryRow(testId + "_last_microsecond", serviceName, callerServiceName, lastMicrosecondBeforeSecondDay, 180, 1),
                entryRow(testId + "_next_bucket", serviceName, callerServiceName, secondDay, 360, 0)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        OffsetDateTime refreshStart = firstDay.minusHours(1);
        OffsetDateTime refreshEnd = secondDay.plusDays(1);

        refreshContinuousAggregate("service_host_list_minute_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_host_list_5min_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_host_list_hour_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_host_list_day_entry", refreshStart, refreshEnd);

        assertHostListBucketSplit(
                "service_host_list_day_entry",
                serviceName,
                SpanKindEnum.SERVER.getValue(),
                "host_" + serviceName,
                firstDay,
                2,
                secondDay,
                1);
    }

    /**
     * Verifies entry-side topology CAGG rollups and the entry branch of public topology views.
     *
     * Starting from raw `service_call` rows with `span_kind = SERVER`, this test checks:
     * - `service_topology_relation_minute_entry`
     * - `service_topology_relation_5min_entry`
     * - `service_topology_relation_hour_entry`
     * - `service_topology_relation_day_entry`
     * - `service_topology_relation_5min`
     * - `service_topology_relation_hour`
     * - `service_topology_relation_day`
     *
     * Fields checked on topology CAGGs:
     * - `cnt`
     * - `sum_duration`
     * - `avg_duration`
     * - `error_num`
     *
     * Fields checked on public topology views:
     * - `cnt`
     * - `avg_duration`
     * - `error_num`
     *
     * Insert rows at:
     * - `10:01:05` with `duration = 100` and `error = 0`
     * - `10:01:20` with `duration = 200` and `error = 1`
     * - `10:04:10` with `duration = 300` and `error = 0`
     * - `10:06:40` with `duration = 500` and `error = 1`
     *
     * Keep topology grouping dimensions identical so only bucket timestamp changes.
     *
     * Assert direct entry topology CAGGs:
     * - 5-minute bucket `10:00` has `cnt = 3`, `sum_duration = 600`,
     *   `avg_duration = 200`, and `error_num = 1`
     * - 5-minute bucket `10:05` has `cnt = 1`, `sum_duration = 500`,
     *   `avg_duration = 500`, and `error_num = 1`
     * - hour and day buckets have `cnt = 4`, `sum_duration = 1100`,
     *   `avg_duration = 275`, and `error_num = 2`
     *
     * Assert the public 5-minute, hour, and day topology views expose the same entry-branch rows
     * through their `span_kind IN (2, 5)` branch.
     */
    @Test
    void topologyEntryRollupsCalculateExpectedMetrics() {
        String testId = "topology_entry_calc_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime minute101 = dayStart.plusHours(10).plusMinutes(1);
        OffsetDateTime minute104 = dayStart.plusHours(10).plusMinutes(4);
        OffsetDateTime minute106 = dayStart.plusHours(10).plusMinutes(6);
        String systemType = "http";
        String remoteAddr = "10.10.10.10";

        List<ServiceCallEntity> rows = List.of(
                entryRow(testId + "_101_1", serviceName, callerServiceName, minute101.plusSeconds(5), 100, 0),
                entryRow(testId + "_101_2", serviceName, callerServiceName, minute101.plusSeconds(20), 200, 1),
                entryRow(testId + "_104", serviceName, callerServiceName, minute104.plusSeconds(10), 300, 0),
                entryRow(testId + "_106", serviceName, callerServiceName, minute106.plusSeconds(40), 500, 1)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        OffsetDateTime refreshStart = dayStart;
        OffsetDateTime refreshEnd = dayStart.plusDays(1);

        refreshContinuousAggregate("service_call_minute_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_minute_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_5min_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_hour_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_day_entry", refreshStart, refreshEnd);

        assertTopologyCaggAggregateRow(
                "service_topology_relation_5min_entry",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                dayStart.plusHours(10),
                3,
                600,
                200,
                1);
        assertTopologyCaggAggregateRow(
                "service_topology_relation_5min_entry",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                dayStart.plusHours(10).plusMinutes(5),
                1,
                500,
                500,
                1);
        assertTopologyCaggAggregateRow(
                "service_topology_relation_hour_entry",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                dayStart.plusHours(10),
                4,
                1100,
                275,
                2);
        assertTopologyCaggAggregateRow(
                "service_topology_relation_day_entry",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                dayStart,
                4,
                1100,
                275,
                2);

        assertTopologyViewAggregateRow(
                "service_topology_relation_5min",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                dayStart.plusHours(10),
                3,
                200,
                1);
        assertTopologyViewAggregateRow(
                "service_topology_relation_5min",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                dayStart.plusHours(10).plusMinutes(5),
                1,
                500,
                1);
        assertTopologyViewAggregateRow(
                "service_topology_relation_hour",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                dayStart.plusHours(10),
                4,
                275,
                2);
        assertTopologyViewAggregateRow(
                "service_topology_relation_day",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                dayStart,
                4,
                275,
                2);
    }

    /**
     * Verifies dependency-side topology CAGG rollups and the dependency branch of public topology views.
     *
     * Starting from raw `service_call` rows with `span_kind = CLIENT`, this test checks:
     * - `service_topology_relation_minute_dependency`
     * - `service_topology_relation_5min_dependency`
     * - `service_topology_relation_hour_dependency`
     * - `service_topology_relation_day_dependency`
     * - `service_topology_relation_minute`
     * - `service_topology_relation_5min`
     * - `service_topology_relation_hour`
     * - `service_topology_relation_day`
     *
     * Insert rows with the same duration and error distribution as the entry topology test.
     *
     * Assert direct dependency topology CAGGs at minute, 5-minute, hour, and day grains.
     * Assert the public topology views expose those same rows through their
     * `span_kind IN (1, 3, 4)` branch.
     */
    @Test
    void topologyDependencyRollupsCalculateExpectedMetrics() {
        String testId = "topology_dependency_calc_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime minute101 = dayStart.plusHours(10).plusMinutes(1);
        OffsetDateTime minute104 = dayStart.plusHours(10).plusMinutes(4);
        OffsetDateTime minute106 = dayStart.plusHours(10).plusMinutes(6);
        String systemType = "http";
        String remoteAddr = "10.20.10.10";

        List<ServiceCallEntity> rows = List.of(
                dependencyRow(testId + "_101_1", serviceName, callerServiceName, minute101.plusSeconds(5), 100, 0),
                dependencyRow(testId + "_101_2", serviceName, callerServiceName, minute101.plusSeconds(20), 200, 1),
                dependencyRow(testId + "_104", serviceName, callerServiceName, minute104.plusSeconds(10), 300, 0),
                dependencyRow(testId + "_106", serviceName, callerServiceName, minute106.plusSeconds(40), 500, 1)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        OffsetDateTime refreshStart = dayStart;
        OffsetDateTime refreshEnd = dayStart.plusDays(1);

        refreshContinuousAggregate("service_call_minute_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_minute_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_5min_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_hour_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_day_dependency", refreshStart, refreshEnd);

        assertTopologyCaggAggregateRow(
                "service_topology_relation_minute_dependency",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                minute101,
                2,
                300,
                150,
                1);
        assertTopologyCaggAggregateRow(
                "service_topology_relation_minute_dependency",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                minute104,
                1,
                300,
                300,
                0);
        assertTopologyCaggAggregateRow(
                "service_topology_relation_minute_dependency",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                minute106,
                1,
                500,
                500,
                1);
        assertTopologyCaggAggregateRow(
                "service_topology_relation_5min_dependency",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                dayStart.plusHours(10),
                3,
                600,
                200,
                1);
        assertTopologyCaggAggregateRow(
                "service_topology_relation_5min_dependency",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                dayStart.plusHours(10).plusMinutes(5),
                1,
                500,
                500,
                1);
        assertTopologyCaggAggregateRow(
                "service_topology_relation_hour_dependency",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                dayStart.plusHours(10),
                4,
                1100,
                275,
                2);
        assertTopologyCaggAggregateRow(
                "service_topology_relation_day_dependency",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                dayStart,
                4,
                1100,
                275,
                2);

        assertTopologyViewAggregateRow(
                "service_topology_relation_minute",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                minute101,
                2,
                150,
                1);
        assertTopologyViewAggregateRow(
                "service_topology_relation_5min",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                dayStart.plusHours(10),
                3,
                200,
                1);
        assertTopologyViewAggregateRow(
                "service_topology_relation_5min",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                dayStart.plusHours(10).plusMinutes(5),
                1,
                500,
                1);
        assertTopologyViewAggregateRow(
                "service_topology_relation_hour",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                dayStart.plusHours(10),
                4,
                275,
                2);
        assertTopologyViewAggregateRow(
                "service_topology_relation_day",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                dayStart,
                4,
                275,
                2);
    }

    /**
     * Verifies public topology views route entry and dependency branches without double counting.
     *
     * Starting from raw `service_call` rows for the same service and caller names, this test inserts:
     * - one entry-side row with `span_kind = SERVER`
     * - one dependency-side row with `span_kind = CLIENT`
     *
     * Refresh both topology chains, then assert every public topology view returns exactly two rows
     * for the service:
     * - the entry row from the `span_kind IN (2, 5)` branch
     * - the dependency row from the `span_kind IN (1, 3, 4)` branch
     *
     * This proves the public `UNION ALL` views expose both branches and do not duplicate either
     * branch when both are present for the same service.
     */
    @Test
    void topologyPublicViewsRouteEntryAndDependencyBranches() {
        String testId = "topology_public_branch_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        OffsetDateTime eventTime = dayStart.plusHours(10).plusMinutes(1).plusSeconds(5);

        List<ServiceCallEntity> rows = List.of(
                entryRow(testId + "_entry", serviceName, callerServiceName, eventTime, 180, 0),
                dependencyRow(testId + "_dependency", serviceName, callerServiceName, eventTime, 360, 1)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        OffsetDateTime refreshStart = dayStart;
        OffsetDateTime refreshEnd = dayStart.plusDays(1);

        refreshContinuousAggregate("service_call_minute_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_call_minute_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_minute_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_minute_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_5min_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_5min_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_hour_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_hour_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_day_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_day_dependency", refreshStart, refreshEnd);

        OffsetDateTime minuteBucket = eventTime.withSecond(0).withNano(0);
        OffsetDateTime fiveMinBucket = floorToFiveMinutes(eventTime);
        OffsetDateTime hourBucket = eventTime.withMinute(0).withSecond(0).withNano(0);

        assertViewRowCount("service_topology_relation_minute", serviceName, 2);
        assertViewRowCount("service_topology_relation_5min", serviceName, 2);
        assertViewRowCount("service_topology_relation_hour", serviceName, 2);
        assertViewRowCount("service_topology_relation_day", serviceName, 2);

        assertTopologyViewAggregateRow(
                "service_topology_relation_minute",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                "http",
                "10.10.10.10",
                minuteBucket,
                1,
                180,
                0);
        assertTopologyViewAggregateRow(
                "service_topology_relation_minute",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                "http",
                "10.20.10.10",
                minuteBucket,
                1,
                360,
                1);
        assertTopologyViewAggregateRow(
                "service_topology_relation_5min",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                "http",
                "10.10.10.10",
                fiveMinBucket,
                1,
                180,
                0);
        assertTopologyViewAggregateRow(
                "service_topology_relation_5min",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                "http",
                "10.20.10.10",
                fiveMinBucket,
                1,
                360,
                1);
        assertTopologyViewAggregateRow(
                "service_topology_relation_hour",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                "http",
                "10.10.10.10",
                hourBucket,
                1,
                180,
                0);
        assertTopologyViewAggregateRow(
                "service_topology_relation_hour",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                "http",
                "10.20.10.10",
                hourBucket,
                1,
                360,
                1);
        assertTopologyViewAggregateRow(
                "service_topology_relation_day",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                "http",
                "10.10.10.10",
                dayStart,
                1,
                180,
                0);
        assertTopologyViewAggregateRow(
                "service_topology_relation_day",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                "http",
                "10.20.10.10",
                dayStart,
                1,
                360,
                1);
    }

    /**
     * Verifies the exact 1-minute `time_bucket` boundary behavior for entry topology relations.
     *
     * Starting from raw `service_call` rows with `span_kind = SERVER`, this test checks:
     * - `service_topology_relation_minute_entry`
     * - entry branch of `service_topology_relation_minute`
     *
     * Insert rows at:
     * - `10:01:00.000000` with `duration = 180` and `error = 0`
     * - `10:01:59.999999` with `duration = 180` and `error = 1`
     * - `10:02:00.000000` with `duration = 360` and `error = 0`
     *
     * Keep all topology grouping dimensions identical.
     *
     * Assert exactly 2 minute topology buckets:
     * - `10:01:00` has `cnt = 2`, `sum_duration = 360`, `avg_duration = 180`, and `error_num = 1`
     * - `10:02:00` has `cnt = 1`, `sum_duration = 360`, `avg_duration = 360`, and `error_num = 0`
     *
     * This proves the entry topology minute bucket interval is closed on the start and open on the end:
     * `[10:01:00, 10:02:00)`.
     */
    @Test
    void topologyMinuteEntryBucketBoundaryIsCorrect() {
        String testId = "topology_entry_minute_boundary_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime minute101 = dayStart.plusHours(10).plusMinutes(1);
        OffsetDateTime minute102 = minute101.plusMinutes(1);
        OffsetDateTime lastMicrosecondBefore102 = minute102.minusNanos(1_000);
        String systemType = "http";
        String remoteAddr = "10.10.10.10";

        List<ServiceCallEntity> rows = List.of(
                entryRow(testId + "_start", serviceName, callerServiceName, minute101, 180, 0),
                entryRow(testId + "_last_microsecond", serviceName, callerServiceName, lastMicrosecondBefore102, 180, 1),
                entryRow(testId + "_next_bucket", serviceName, callerServiceName, minute102, 360, 0)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        refreshContinuousAggregate("service_call_minute_entry", minute101.minusMinutes(1), minute102.plusMinutes(1));
        refreshContinuousAggregate("service_topology_relation_minute_entry", minute101.minusMinutes(1), minute102.plusMinutes(1));

        assertTopologyBucketSplit(
                "service_topology_relation_minute_entry",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                minute101,
                2,
                minute102,
                1);
        assertTopologyBucketSplit(
                "service_topology_relation_minute",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                minute101,
                2,
                minute102,
                1);

        assertTopologyCaggAggregateRow(
                "service_topology_relation_minute_entry",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                minute101,
                2,
                360,
                180,
                1);
        assertTopologyCaggAggregateRow(
                "service_topology_relation_minute_entry",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                minute102,
                1,
                360,
                360,
                0);
        assertTopologyViewAggregateRow(
                "service_topology_relation_minute",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                minute101,
                2,
                180,
                1);
        assertTopologyViewAggregateRow(
                "service_topology_relation_minute",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                minute102,
                1,
                360,
                0);
    }

    /**
     * Verifies the exact 5-minute `time_bucket` boundary behavior for entry topology relations.
     *
     * Starting from raw `service_call` rows with `span_kind = SERVER`, this test checks:
     * - `service_topology_relation_5min_entry`
     * - entry branch of `service_topology_relation_5min`
     *
     * Insert rows at:
     * - `10:00:00.000000` with `duration = 180` and `error = 0`
     * - `10:04:59.999999` with `duration = 180` and `error = 1`
     * - `10:05:00.000000` with `duration = 360` and `error = 0`
     *
     * Keep all topology grouping dimensions identical.
     *
     * Assert exactly 2 five-minute topology buckets:
     * - `10:00:00` has `cnt = 2`, `sum_duration = 360`, `avg_duration = 180`, and `error_num = 1`
     * - `10:05:00` has `cnt = 1`, `sum_duration = 360`, `avg_duration = 360`, and `error_num = 0`
     *
     * This proves the entry topology 5-minute bucket interval is closed on the start and open on the end:
     * `[10:00:00, 10:05:00)`.
     */
    @Test
    void topologyFiveMinuteEntryBucketBoundaryIsCorrect() {
        String testId = "topology_entry_5min_boundary_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime fiveMin1000 = dayStart.plusHours(10);
        OffsetDateTime fiveMin1005 = fiveMin1000.plusMinutes(5);
        OffsetDateTime lastMicrosecondBefore1005 = fiveMin1005.minusNanos(1_000);
        String systemType = "http";
        String remoteAddr = "10.10.10.10";

        List<ServiceCallEntity> rows = List.of(
                entryRow(testId + "_start", serviceName, callerServiceName, fiveMin1000, 180, 0),
                entryRow(testId + "_last_microsecond", serviceName, callerServiceName, lastMicrosecondBefore1005, 180, 1),
                entryRow(testId + "_next_bucket", serviceName, callerServiceName, fiveMin1005, 360, 0)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        OffsetDateTime refreshStart = fiveMin1000.minusMinutes(1);
        OffsetDateTime refreshEnd = fiveMin1005.plusMinutes(5);

        refreshContinuousAggregate("service_call_minute_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_minute_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_5min_entry", refreshStart, refreshEnd);

        assertTopologyBucketSplit(
                "service_topology_relation_5min_entry",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                fiveMin1000,
                2,
                fiveMin1005,
                1);
        assertTopologyBucketSplit(
                "service_topology_relation_5min",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                fiveMin1000,
                2,
                fiveMin1005,
                1);

        assertTopologyCaggAggregateRow(
                "service_topology_relation_5min_entry",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                fiveMin1000,
                2,
                360,
                180,
                1);
        assertTopologyCaggAggregateRow(
                "service_topology_relation_5min_entry",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                fiveMin1005,
                1,
                360,
                360,
                0);
        assertTopologyViewAggregateRow(
                "service_topology_relation_5min",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                fiveMin1000,
                2,
                180,
                1);
        assertTopologyViewAggregateRow(
                "service_topology_relation_5min",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                fiveMin1005,
                1,
                360,
                0);
    }

    /**
     * Verifies the exact 1-hour `time_bucket` boundary behavior for entry topology relations.
     *
     * Starting from raw `service_call` rows with `span_kind = SERVER`, this test checks:
     * - `service_topology_relation_hour_entry`
     * - entry branch of `service_topology_relation_hour`
     *
     * Insert rows at:
     * - `10:00:00.000000` with `duration = 180` and `error = 0`
     * - `10:59:59.999999` with `duration = 180` and `error = 1`
     * - `11:00:00.000000` with `duration = 360` and `error = 0`
     *
     * Keep all topology grouping dimensions identical.
     *
     * Assert exactly 2 hour topology buckets:
     * - `10:00:00` has `cnt = 2`, `sum_duration = 360`, `avg_duration = 180`, and `error_num = 1`
     * - `11:00:00` has `cnt = 1`, `sum_duration = 360`, `avg_duration = 360`, and `error_num = 0`
     *
     * This proves the entry topology hour bucket interval is closed on the start and open on the end:
     * `[10:00:00, 11:00:00)`.
     */
    @Test
    void topologyHourEntryBucketBoundaryIsCorrect() {
        String testId = "topology_entry_hour_boundary_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime hour10 = dayStart.plusHours(10);
        OffsetDateTime hour11 = hour10.plusHours(1);
        OffsetDateTime lastMicrosecondBefore11 = hour11.minusNanos(1_000);
        String systemType = "http";
        String remoteAddr = "10.10.10.10";

        List<ServiceCallEntity> rows = List.of(
                entryRow(testId + "_start", serviceName, callerServiceName, hour10, 180, 0),
                entryRow(testId + "_last_microsecond", serviceName, callerServiceName, lastMicrosecondBefore11, 180, 1),
                entryRow(testId + "_next_bucket", serviceName, callerServiceName, hour11, 360, 0)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        OffsetDateTime refreshStart = hour10.minusHours(1);
        OffsetDateTime refreshEnd = hour11.plusHours(1);

        refreshContinuousAggregate("service_call_minute_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_minute_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_5min_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_hour_entry", refreshStart, refreshEnd);

        assertTopologyBucketSplit(
                "service_topology_relation_hour_entry",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                hour10,
                2,
                hour11,
                1);
        assertTopologyBucketSplit(
                "service_topology_relation_hour",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                hour10,
                2,
                hour11,
                1);

        assertTopologyCaggAggregateRow(
                "service_topology_relation_hour_entry",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                hour10,
                2,
                360,
                180,
                1);
        assertTopologyCaggAggregateRow(
                "service_topology_relation_hour_entry",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                hour11,
                1,
                360,
                360,
                0);
        assertTopologyViewAggregateRow(
                "service_topology_relation_hour",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                hour10,
                2,
                180,
                1);
        assertTopologyViewAggregateRow(
                "service_topology_relation_hour",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                hour11,
                1,
                360,
                0);
    }

    /**
     * Verifies the exact 1-day `time_bucket` boundary behavior for entry topology relations.
     *
     * Starting from raw `service_call` rows with `span_kind = SERVER`, this test checks:
     * - `service_topology_relation_day_entry`
     * - entry branch of `service_topology_relation_day`
     *
     * Insert rows at:
     * - `firstDay 00:00:00.000000` with `duration = 180` and `error = 0`
     * - `firstDay 23:59:59.999999` with `duration = 180` and `error = 1`
     * - `secondDay 00:00:00.000000` with `duration = 360` and `error = 0`
     *
     * Keep all topology grouping dimensions identical.
     *
     * Assert exactly 2 day topology buckets:
     * - `firstDay 00:00:00` has `cnt = 2`, `sum_duration = 360`, `avg_duration = 180`, and `error_num = 1`
     * - `secondDay 00:00:00` has `cnt = 1`, `sum_duration = 360`, `avg_duration = 360`, and `error_num = 0`
     *
     * This proves the entry topology day bucket interval is closed on the start and open on the end:
     * `[firstDay 00:00:00, secondDay 00:00:00)`.
     */
    @Test
    void topologyDayEntryBucketBoundaryIsCorrect() {
        String testId = "topology_entry_day_boundary_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime firstDay = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(2));

        OffsetDateTime secondDay = firstDay.plusDays(1);
        OffsetDateTime lastMicrosecondBeforeSecondDay = secondDay.minusNanos(1_000);
        String systemType = "http";
        String remoteAddr = "10.10.10.10";

        List<ServiceCallEntity> rows = List.of(
                entryRow(testId + "_start", serviceName, callerServiceName, firstDay, 180, 0),
                entryRow(testId + "_last_microsecond", serviceName, callerServiceName, lastMicrosecondBeforeSecondDay, 180, 1),
                entryRow(testId + "_next_bucket", serviceName, callerServiceName, secondDay, 360, 0)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        OffsetDateTime refreshStart = firstDay.minusHours(1);
        OffsetDateTime refreshEnd = secondDay.plusDays(1);

        refreshContinuousAggregate("service_call_minute_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_minute_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_5min_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_hour_entry", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_day_entry", refreshStart, refreshEnd);

        assertTopologyBucketSplit(
                "service_topology_relation_day_entry",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                firstDay,
                2,
                secondDay,
                1);
        assertTopologyBucketSplit(
                "service_topology_relation_day",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                firstDay,
                2,
                secondDay,
                1);

        assertTopologyCaggAggregateRow(
                "service_topology_relation_day_entry",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                firstDay,
                2,
                360,
                180,
                1);
        assertTopologyCaggAggregateRow(
                "service_topology_relation_day_entry",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                secondDay,
                1,
                360,
                360,
                0);
        assertTopologyViewAggregateRow(
                "service_topology_relation_day",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                firstDay,
                2,
                180,
                1);
        assertTopologyViewAggregateRow(
                "service_topology_relation_day",
                serviceName,
                callerServiceName,
                SpanKindEnum.SERVER.getValue(),
                systemType,
                remoteAddr,
                secondDay,
                1,
                360,
                0);
    }

    /**
     * Verifies the exact 1-minute `time_bucket` boundary behavior for dependency topology relations.
     *
     * Starting from raw `service_call` rows with `span_kind = CLIENT`, this test checks:
     * - `service_topology_relation_minute_dependency`
     * - dependency branch of `service_topology_relation_minute`
     *
     * Insert rows at:
     * - `10:01:00.000000` with `duration = 180` and `error = 0`
     * - `10:01:59.999999` with `duration = 180` and `error = 1`
     * - `10:02:00.000000` with `duration = 360` and `error = 0`
     *
     * Keep all topology grouping dimensions identical.
     *
     * Assert exactly 2 minute topology buckets:
     * - `10:01:00` has `cnt = 2`, `sum_duration = 360`, `avg_duration = 180`, and `error_num = 1`
     * - `10:02:00` has `cnt = 1`, `sum_duration = 360`, `avg_duration = 360`, and `error_num = 0`
     *
     * This proves the dependency topology minute bucket interval is closed on the start and open on the end:
     * `[10:01:00, 10:02:00)`.
     */
    @Test
    void topologyMinuteDependencyBucketBoundaryIsCorrect() {
        String testId = "topology_dependency_minute_boundary_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime minute101 = dayStart.plusHours(10).plusMinutes(1);
        OffsetDateTime minute102 = minute101.plusMinutes(1);
        OffsetDateTime lastMicrosecondBefore102 = minute102.minusNanos(1_000);
        String systemType = "http";
        String remoteAddr = "10.20.10.10";

        List<ServiceCallEntity> rows = List.of(
                dependencyRow(testId + "_start", serviceName, callerServiceName, minute101, 180, 0),
                dependencyRow(testId + "_last_microsecond", serviceName, callerServiceName, lastMicrosecondBefore102, 180, 1),
                dependencyRow(testId + "_next_bucket", serviceName, callerServiceName, minute102, 360, 0)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        refreshContinuousAggregate("service_call_minute_dependency", minute101.minusMinutes(1), minute102.plusMinutes(1));
        refreshContinuousAggregate("service_topology_relation_minute_dependency", minute101.minusMinutes(1), minute102.plusMinutes(1));

        assertTopologyBucketSplit(
                "service_topology_relation_minute_dependency",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                minute101,
                2,
                minute102,
                1);
        assertTopologyBucketSplit(
                "service_topology_relation_minute",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                minute101,
                2,
                minute102,
                1);

        assertTopologyCaggAggregateRow(
                "service_topology_relation_minute_dependency",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                minute101,
                2,
                360,
                180,
                1);
        assertTopologyCaggAggregateRow(
                "service_topology_relation_minute_dependency",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                minute102,
                1,
                360,
                360,
                0);
        assertTopologyViewAggregateRow(
                "service_topology_relation_minute",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                minute101,
                2,
                180,
                1);
        assertTopologyViewAggregateRow(
                "service_topology_relation_minute",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                minute102,
                1,
                360,
                0);
    }

    /**
     * Verifies the exact 5-minute `time_bucket` boundary behavior for dependency topology relations.
     *
     * Starting from raw `service_call` rows with `span_kind = CLIENT`, this test checks:
     * - `service_topology_relation_5min_dependency`
     * - dependency branch of `service_topology_relation_5min`
     *
     * Insert rows at:
     * - `10:00:00.000000` with `duration = 180` and `error = 0`
     * - `10:04:59.999999` with `duration = 180` and `error = 1`
     * - `10:05:00.000000` with `duration = 360` and `error = 0`
     *
     * Keep all topology grouping dimensions identical.
     *
     * Assert exactly 2 five-minute topology buckets:
     * - `10:00:00` has `cnt = 2`, `sum_duration = 360`, `avg_duration = 180`, and `error_num = 1`
     * - `10:05:00` has `cnt = 1`, `sum_duration = 360`, `avg_duration = 360`, and `error_num = 0`
     *
     * This proves the dependency topology 5-minute bucket interval is closed on the start and open on the end:
     * `[10:00:00, 10:05:00)`.
     */
    @Test
    void topologyFiveMinuteDependencyBucketBoundaryIsCorrect() {
        String testId = "topology_dependency_5min_boundary_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime fiveMin1000 = dayStart.plusHours(10);
        OffsetDateTime fiveMin1005 = fiveMin1000.plusMinutes(5);
        OffsetDateTime lastMicrosecondBefore1005 = fiveMin1005.minusNanos(1_000);
        String systemType = "http";
        String remoteAddr = "10.20.10.10";

        List<ServiceCallEntity> rows = List.of(
                dependencyRow(testId + "_start", serviceName, callerServiceName, fiveMin1000, 180, 0),
                dependencyRow(testId + "_last_microsecond", serviceName, callerServiceName, lastMicrosecondBefore1005, 180, 1),
                dependencyRow(testId + "_next_bucket", serviceName, callerServiceName, fiveMin1005, 360, 0)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        OffsetDateTime refreshStart = fiveMin1000.minusMinutes(1);
        OffsetDateTime refreshEnd = fiveMin1005.plusMinutes(5);

        refreshContinuousAggregate("service_call_minute_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_minute_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_5min_dependency", refreshStart, refreshEnd);

        assertTopologyBucketSplit(
                "service_topology_relation_5min_dependency",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                fiveMin1000,
                2,
                fiveMin1005,
                1);
        assertTopologyBucketSplit(
                "service_topology_relation_5min",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                fiveMin1000,
                2,
                fiveMin1005,
                1);

        assertTopologyCaggAggregateRow(
                "service_topology_relation_5min_dependency",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                fiveMin1000,
                2,
                360,
                180,
                1);
        assertTopologyCaggAggregateRow(
                "service_topology_relation_5min_dependency",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                fiveMin1005,
                1,
                360,
                360,
                0);
        assertTopologyViewAggregateRow(
                "service_topology_relation_5min",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                fiveMin1000,
                2,
                180,
                1);
        assertTopologyViewAggregateRow(
                "service_topology_relation_5min",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                fiveMin1005,
                1,
                360,
                0);
    }

    /**
     * Verifies the exact 1-hour `time_bucket` boundary behavior for dependency topology relations.
     *
     * Starting from raw `service_call` rows with `span_kind = CLIENT`, this test checks:
     * - `service_topology_relation_hour_dependency`
     * - dependency branch of `service_topology_relation_hour`
     *
     * Insert rows at:
     * - `10:00:00.000000` with `duration = 180` and `error = 0`
     * - `10:59:59.999999` with `duration = 180` and `error = 1`
     * - `11:00:00.000000` with `duration = 360` and `error = 0`
     *
     * Keep all topology grouping dimensions identical.
     *
     * Assert exactly 2 hour topology buckets:
     * - `10:00:00` has `cnt = 2`, `sum_duration = 360`, `avg_duration = 180`, and `error_num = 1`
     * - `11:00:00` has `cnt = 1`, `sum_duration = 360`, `avg_duration = 360`, and `error_num = 0`
     *
     * This proves the dependency topology hour bucket interval is closed on the start and open on the end:
     * `[10:00:00, 11:00:00)`.
     */
    @Test
    void topologyHourDependencyBucketBoundaryIsCorrect() {
        String testId = "topology_dependency_hour_boundary_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime dayStart = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

        OffsetDateTime hour10 = dayStart.plusHours(10);
        OffsetDateTime hour11 = hour10.plusHours(1);
        OffsetDateTime lastMicrosecondBefore11 = hour11.minusNanos(1_000);
        String systemType = "http";
        String remoteAddr = "10.20.10.10";

        List<ServiceCallEntity> rows = List.of(
                dependencyRow(testId + "_start", serviceName, callerServiceName, hour10, 180, 0),
                dependencyRow(testId + "_last_microsecond", serviceName, callerServiceName, lastMicrosecondBefore11, 180, 1),
                dependencyRow(testId + "_next_bucket", serviceName, callerServiceName, hour11, 360, 0)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        OffsetDateTime refreshStart = hour10.minusHours(1);
        OffsetDateTime refreshEnd = hour11.plusHours(1);

        refreshContinuousAggregate("service_call_minute_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_minute_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_5min_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_hour_dependency", refreshStart, refreshEnd);

        assertTopologyBucketSplit(
                "service_topology_relation_hour_dependency",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                hour10,
                2,
                hour11,
                1);
        assertTopologyBucketSplit(
                "service_topology_relation_hour",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                hour10,
                2,
                hour11,
                1);

        assertTopologyCaggAggregateRow(
                "service_topology_relation_hour_dependency",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                hour10,
                2,
                360,
                180,
                1);
        assertTopologyCaggAggregateRow(
                "service_topology_relation_hour_dependency",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                hour11,
                1,
                360,
                360,
                0);
        assertTopologyViewAggregateRow(
                "service_topology_relation_hour",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                hour10,
                2,
                180,
                1);
        assertTopologyViewAggregateRow(
                "service_topology_relation_hour",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                hour11,
                1,
                360,
                0);
    }

    /**
     * Verifies the exact 1-day `time_bucket` boundary behavior for dependency topology relations.
     *
     * Starting from raw `service_call` rows with `span_kind = CLIENT`, this test checks:
     * - `service_topology_relation_day_dependency`
     * - dependency branch of `service_topology_relation_day`
     *
     * Insert rows at:
     * - `firstDay 00:00:00.000000` with `duration = 180` and `error = 0`
     * - `firstDay 23:59:59.999999` with `duration = 180` and `error = 1`
     * - `secondDay 00:00:00.000000` with `duration = 360` and `error = 0`
     *
     * Keep all topology grouping dimensions identical.
     *
     * Assert exactly 2 day topology buckets:
     * - `firstDay 00:00:00` has `cnt = 2`, `sum_duration = 360`, `avg_duration = 180`, and `error_num = 1`
     * - `secondDay 00:00:00` has `cnt = 1`, `sum_duration = 360`, `avg_duration = 360`, and `error_num = 0`
     *
     * This proves the dependency topology day bucket interval is closed on the start and open on the end:
     * `[firstDay 00:00:00, secondDay 00:00:00)`.
     */
    @Test
    void topologyDayDependencyBucketBoundaryIsCorrect() {
        String testId = "topology_dependency_day_boundary_" + UUID.randomUUID().toString().replace("-", "");
        String serviceName = "svc_" + testId;
        String callerServiceName = "caller_" + testId;
        OffsetDateTime firstDay = startOfUtcDay(OffsetDateTime.now(ZoneOffset.UTC).minusDays(2));

        OffsetDateTime secondDay = firstDay.plusDays(1);
        OffsetDateTime lastMicrosecondBeforeSecondDay = secondDay.minusNanos(1_000);
        String systemType = "http";
        String remoteAddr = "10.20.10.10";

        List<ServiceCallEntity> rows = List.of(
                dependencyRow(testId + "_start", serviceName, callerServiceName, firstDay, 180, 0),
                dependencyRow(testId + "_last_microsecond", serviceName, callerServiceName, lastMicrosecondBeforeSecondDay, 180, 1),
                dependencyRow(testId + "_next_bucket", serviceName, callerServiceName, secondDay, 360, 0)
        );

        assertEquals(rows.size(), serviceCallMapper.batchInsert(rows));

        OffsetDateTime refreshStart = firstDay.minusHours(1);
        OffsetDateTime refreshEnd = secondDay.plusDays(1);

        refreshContinuousAggregate("service_call_minute_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_minute_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_5min_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_hour_dependency", refreshStart, refreshEnd);
        refreshContinuousAggregate("service_topology_relation_day_dependency", refreshStart, refreshEnd);

        assertTopologyBucketSplit(
                "service_topology_relation_day_dependency",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                firstDay,
                2,
                secondDay,
                1);
        assertTopologyBucketSplit(
                "service_topology_relation_day",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                firstDay,
                2,
                secondDay,
                1);

        assertTopologyCaggAggregateRow(
                "service_topology_relation_day_dependency",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                firstDay,
                2,
                360,
                180,
                1);
        assertTopologyCaggAggregateRow(
                "service_topology_relation_day_dependency",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                secondDay,
                1,
                360,
                360,
                0);
        assertTopologyViewAggregateRow(
                "service_topology_relation_day",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                firstDay,
                2,
                180,
                1);
        assertTopologyViewAggregateRow(
                "service_topology_relation_day",
                serviceName,
                callerServiceName,
                SpanKindEnum.CLIENT.getValue(),
                systemType,
                remoteAddr,
                secondDay,
                1,
                360,
                0);
    }

    private void addPercentileFixtureRows(List<ServiceCallEntity> rows,
                                          String rowPrefix,
                                          String serviceName,
                                          String callerServiceName,
                                          OffsetDateTime bucketTime) {
        addPercentileFixtureRows(rows, rowPrefix, entryDimensions(serviceName, callerServiceName), bucketTime);
    }

    private void addPercentileFixtureRows(List<ServiceCallEntity> rows,
                                          String rowPrefix,
                                          EntryFixtureDimensions dimensions,
                                          OffsetDateTime bucketTime) {
        for (int i = 0; i < 100; i++) {
            rows.add(entryRow(
                    rowPrefix + "_pct_" + i,
                    dimensions,
                    bucketTime.plusNanos((i + 1L) * 1_000_000L),
                    (i + 1L) * 100L,
                    0));
        }
    }

    private ServiceCallEntity entryRow(String rowId,
                                       String serviceName,
                                       String callerServiceName,
                                       OffsetDateTime time,
                                       long duration,
                                       long error) {
        return entryRow(rowId, entryDimensions(serviceName, callerServiceName), time, duration, error, time.plusSeconds(2));
    }

    private ServiceCallEntity entryRow(String rowId,
                                       String serviceName,
                                       String callerServiceName,
                                       OffsetDateTime time,
                                       long duration,
                                       long error,
                                       OffsetDateTime updateTime) {
        return entryRow(rowId, entryDimensions(serviceName, callerServiceName), time, duration, error, updateTime);
    }

    private ServiceCallEntity entryRow(String rowId,
                                       EntryFixtureDimensions dimensions,
                                       OffsetDateTime time,
                                       long duration,
                                       long error) {
        return entryRow(rowId, dimensions, time, duration, error, time.plusSeconds(2));
    }

    private ServiceCallEntity entryRow(String rowId,
                                       EntryFixtureDimensions dimensions,
                                       OffsetDateTime time,
                                       long duration,
                                       long error,
                                       OffsetDateTime updateTime) {
        return ServiceCallEntity.builder()
                .time(time)
                .traceId("trace_" + rowId)
                .spanId("span_" + rowId)
                .serviceName(dimensions.serviceName())
                .hostName(dimensions.hostName())
                .actionName(dimensions.actionName())
                .targetNameId(dimensions.targetNameId())
                .remoteAddr(dimensions.remoteAddr())
                .systemType(dimensions.systemType())
                .component(dimensions.component())
                .spanKind(dimensions.spanKind())
                .callerTime(time.minusSeconds(1))
                .callerSpanId("caller_span_" + rowId)
                .callerServiceName(dimensions.callerServiceName())
                .callerHostName(dimensions.callerHostName())
                .callerActionName(dimensions.callerActionName())
                .callerTargetNameId(dimensions.callerTargetNameId())
                .callerRemoteAddr(dimensions.callerRemoteAddr())
                .callerSystemType(dimensions.callerSystemType())
                .callerComponent(dimensions.callerComponent())
                .callerSpanKind(dimensions.callerSpanKind())
                .cnt(1L)
                .duration(duration)
                .error(error)
                .createTime(time.plusSeconds(1))
                .updateTime(updateTime)
                .build();
    }

    private ServiceCallEntity dependencyRow(String rowId,
                                            String serviceName,
                                            String callerServiceName,
                                            OffsetDateTime time,
                                            long duration,
                                            long error) {
        return dependencyRow(
                rowId,
                dependencyDimensions(serviceName, callerServiceName),
                time,
                duration,
                error,
                time.plusSeconds(2));
    }

    private ServiceCallEntity dependencyRow(String rowId,
                                            String serviceName,
                                            String callerServiceName,
                                            SpanKindEnum dependencySpanKind,
                                            OffsetDateTime time,
                                            long duration,
                                            long error) {
        return dependencyRow(
                rowId,
                dependencyDimensions(serviceName, callerServiceName, dependencySpanKind),
                time,
                duration,
                error,
                time.plusSeconds(2));
    }

    private ServiceCallEntity dependencyRow(String rowId,
                                            String serviceName,
                                            String callerServiceName,
                                            OffsetDateTime time,
                                            long duration,
                                            long error,
                                            OffsetDateTime updateTime) {
        return dependencyRow(
                rowId,
                dependencyDimensions(serviceName, callerServiceName),
                time,
                duration,
                error,
                updateTime);
    }

    private ServiceCallEntity dependencyRow(String rowId,
                                            String serviceName,
                                            String callerServiceName,
                                            SpanKindEnum dependencySpanKind,
                                            OffsetDateTime time,
                                            long duration,
                                            long error,
                                            OffsetDateTime updateTime) {
        return dependencyRow(
                rowId,
                dependencyDimensions(serviceName, callerServiceName, dependencySpanKind),
                time,
                duration,
                error,
                updateTime);
    }

    private ServiceCallEntity dependencyRow(String rowId,
                                            EntryFixtureDimensions dimensions,
                                            OffsetDateTime time,
                                            long duration,
                                            long error) {
        return dependencyRow(rowId, dimensions, time, duration, error, time.plusSeconds(2));
    }

    private ServiceCallEntity dependencyRow(String rowId,
                                            EntryFixtureDimensions dimensions,
                                            OffsetDateTime time,
                                            long duration,
                                            long error,
                                            OffsetDateTime updateTime) {
        return entryRow(rowId, dimensions, time, duration, error, updateTime);
    }

    private EntryFixtureDimensions entryDimensions(String serviceName, String callerServiceName) {
        return new EntryFixtureDimensions(
                serviceName,
                "host_" + serviceName,
                "action_" + serviceName,
                stableUuid(serviceName + "_target"),
                "10.10.10.10",
                "http",
                "spring-web",
                SpanKindEnum.SERVER.getValue(),
                callerServiceName,
                "host_" + callerServiceName,
                "action_" + callerServiceName,
                stableUuid(callerServiceName + "_target"),
                "10.10.10.20",
                "http",
                "http-client",
                SpanKindEnum.CLIENT.getValue());
    }

    private EntryFixtureDimensions entryDimensions(String serviceName, String callerServiceName, String hostName) {
        EntryFixtureDimensions dimensions = entryDimensions(serviceName, callerServiceName);
        return new EntryFixtureDimensions(
                dimensions.serviceName(),
                hostName,
                dimensions.actionName(),
                dimensions.targetNameId(),
                dimensions.remoteAddr(),
                dimensions.systemType(),
                dimensions.component(),
                dimensions.spanKind(),
                dimensions.callerServiceName(),
                dimensions.callerHostName(),
                dimensions.callerActionName(),
                dimensions.callerTargetNameId(),
                dimensions.callerRemoteAddr(),
                dimensions.callerSystemType(),
                dimensions.callerComponent(),
                dimensions.callerSpanKind());
    }

    private EntryFixtureDimensions dependencyDimensions(String serviceName, String callerServiceName) {
        return dependencyDimensions(serviceName, callerServiceName, SpanKindEnum.CLIENT);
    }

    private EntryFixtureDimensions dependencyDimensions(String serviceName,
                                                       String callerServiceName,
                                                       SpanKindEnum dependencySpanKind) {
        return new EntryFixtureDimensions(
                serviceName,
                "host_" + serviceName,
                "action_" + serviceName,
                stableUuid(serviceName + "_target"),
                "10.20.10.10",
                "http",
                "http-client",
                dependencySpanKind.getValue(),
                callerServiceName,
                "host_" + callerServiceName,
                "action_" + callerServiceName,
                stableUuid(callerServiceName + "_target"),
                "10.20.10.20",
                "http",
                "spring-web",
                SpanKindEnum.SERVER.getValue());
    }

    private void refreshContinuousAggregate(String viewName,
                                            OffsetDateTime startInclusive,
                                            OffsetDateTime endExclusive) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (var statement = connection.prepareStatement(
                    "CALL refresh_continuous_aggregate('" + viewName + "', ?, ?)")) {
                statement.setObject(1, startInclusive);
                statement.setObject(2, endExclusive);
                statement.execute();
            }
            return null;
        });
    }

    private void assertServiceCallAggregateRow(String relationName,
                                               String serviceName,
                                               short spanKind,
                                               OffsetDateTime bucketTime,
                                               long expectedCnt,
                                               long expectedSumDuration,
                                               long expectedAvgDuration,
                                               long expectedMaxDuration,
                                               long expectedMinDuration,
                                               long expectedErrorNum) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        SELECT cnt, sum_duration, avg_duration, max_duration, min_duration, error_num
                        FROM %s
                        WHERE service_name = ?
                          AND span_kind = ?
                          AND time = ?
                        """.formatted(relationName),
                serviceName,
                spanKind,
                bucketTime);

        assertEquals(expectedCnt, longValue(row.get("cnt")), columnLabel(relationName, "cnt"));
        assertEquals(expectedSumDuration, longValue(row.get("sum_duration")), columnLabel(relationName, "sum_duration"));
        assertEquals(expectedAvgDuration, longValue(row.get("avg_duration")), columnLabel(relationName, "avg_duration"));
        assertEquals(expectedMaxDuration, longValue(row.get("max_duration")), columnLabel(relationName, "max_duration"));
        assertEquals(expectedMinDuration, longValue(row.get("min_duration")), columnLabel(relationName, "min_duration"));
        assertEquals(expectedErrorNum, longValue(row.get("error_num")), columnLabel(relationName, "error_num"));
    }

    private void assertHostListAggregateRow(String serviceName,
                                            short spanKind,
                                            OffsetDateTime bucketTime,
                                            long expectedCnt) {
        assertHostListAggregateRow(
                "service_host_list_minute_entry",
                serviceName,
                spanKind,
                "host_" + serviceName,
                bucketTime,
                expectedCnt);
    }

    private void assertHostListAggregateRow(String relationName,
                                            String serviceName,
                                            short spanKind,
                                            String hostName,
                                            OffsetDateTime bucketTime,
                                            long expectedCnt) {
        Long actualCnt = jdbcTemplate.queryForObject(
                """
                        SELECT cnt
                        FROM %s
                        WHERE service_name = ?
                          AND span_kind = ?
                          AND host_name = ?
                          AND time = ?
                        """.formatted(relationName),
                Long.class,
                serviceName,
                spanKind,
                hostName,
                bucketTime);

        assertEquals(expectedCnt, actualCnt, columnLabel(relationName, "cnt"));
    }

    private void assertHostListBucketSplit(String relationName,
                                           String serviceName,
                                           short spanKind,
                                           String hostName,
                                           OffsetDateTime firstBucketTime,
                                           long firstExpectedCnt,
                                           OffsetDateTime secondBucketTime,
                                           long secondExpectedCnt) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                        SELECT time, cnt
                        FROM %s
                        WHERE service_name = ?
                          AND span_kind = ?
                          AND host_name = ?
                        ORDER BY time
                        """.formatted(relationName),
                serviceName,
                spanKind,
                hostName);

        assertEquals(2, rows.size(), columnLabel(relationName, "bucket_row_count"));
        assertEquals(firstBucketTime, offsetDateTimeValue(rows.get(0).get("time")), columnLabel(relationName, "first_bucket.time"));
        assertEquals(firstExpectedCnt, longValue(rows.get(0).get("cnt")), columnLabel(relationName, "first_bucket.cnt"));
        assertEquals(secondBucketTime, offsetDateTimeValue(rows.get(1).get("time")), columnLabel(relationName, "second_bucket.time"));
        assertEquals(secondExpectedCnt, longValue(rows.get(1).get("cnt")), columnLabel(relationName, "second_bucket.cnt"));
    }

    private void assertTopologyAggregateRow(String serviceName,
                                            String callerServiceName,
                                            short spanKind,
                                            OffsetDateTime bucketTime,
                                            long expectedCnt,
                                            long expectedAvgDuration,
                                            long expectedErrorNum) {
        assertTopologyViewAggregateRow(
                "service_topology_relation_minute",
                serviceName,
                callerServiceName,
                spanKind,
                "http",
                "10.10.10.10",
                bucketTime,
                expectedCnt,
                expectedAvgDuration,
                expectedErrorNum);
    }

    private void assertTopologyCaggAggregateRow(String relationName,
                                                String serviceName,
                                                String callerServiceName,
                                                short spanKind,
                                                String systemType,
                                                String remoteAddr,
                                                OffsetDateTime bucketTime,
                                                long expectedCnt,
                                                long expectedSumDuration,
                                                long expectedAvgDuration,
                                                long expectedErrorNum) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        SELECT cnt, sum_duration, avg_duration, error_num
                        FROM %s
                        WHERE service_name = ?
                          AND caller_service_name = ?
                          AND span_kind = ?
                          AND system_type = ?
                          AND remote_addr = ?
                          AND time = ?
                        """.formatted(relationName),
                serviceName,
                callerServiceName,
                spanKind,
                systemType,
                remoteAddr,
                bucketTime);

        assertEquals(expectedCnt, longValue(row.get("cnt")), columnLabel(relationName, "cnt"));
        assertEquals(expectedSumDuration, longValue(row.get("sum_duration")), columnLabel(relationName, "sum_duration"));
        assertEquals(expectedAvgDuration, longValue(row.get("avg_duration")), columnLabel(relationName, "avg_duration"));
        assertEquals(expectedErrorNum, longValue(row.get("error_num")), columnLabel(relationName, "error_num"));
    }

    private void assertTopologyViewAggregateRow(String relationName,
                                                String serviceName,
                                                String callerServiceName,
                                                short spanKind,
                                                String systemType,
                                                String remoteAddr,
                                                OffsetDateTime bucketTime,
                                                long expectedCnt,
                                                long expectedAvgDuration,
                                                long expectedErrorNum) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        SELECT cnt, avg_duration, error_num
                        FROM %s
                        WHERE service_name = ?
                          AND caller_service_name = ?
                          AND span_kind = ?
                          AND system_type = ?
                          AND remote_addr = ?
                          AND time = ?
                        """.formatted(relationName),
                serviceName,
                callerServiceName,
                spanKind,
                systemType,
                remoteAddr,
                bucketTime);

        assertEquals(expectedCnt, longValue(row.get("cnt")), columnLabel(relationName, "cnt"));
        assertEquals(expectedAvgDuration, longValue(row.get("avg_duration")), columnLabel(relationName, "avg_duration"));
        assertEquals(expectedErrorNum, longValue(row.get("error_num")), columnLabel(relationName, "error_num"));
    }

    private void assertTopologyBucketSplit(String relationName,
                                           String serviceName,
                                           String callerServiceName,
                                           short spanKind,
                                           String systemType,
                                           String remoteAddr,
                                           OffsetDateTime firstBucketTime,
                                           long firstExpectedCnt,
                                           OffsetDateTime secondBucketTime,
                                           long secondExpectedCnt) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                        SELECT time, cnt
                        FROM %s
                        WHERE service_name = ?
                          AND caller_service_name = ?
                          AND span_kind = ?
                          AND system_type = ?
                          AND remote_addr = ?
                        ORDER BY time
                        """.formatted(relationName),
                serviceName,
                callerServiceName,
                spanKind,
                systemType,
                remoteAddr);

        assertEquals(2, rows.size(), columnLabel(relationName, "bucket_row_count"));
        assertEquals(firstBucketTime, offsetDateTimeValue(rows.get(0).get("time")), columnLabel(relationName, "first_bucket.time"));
        assertEquals(firstExpectedCnt, longValue(rows.get(0).get("cnt")), columnLabel(relationName, "first_bucket.cnt"));
        assertEquals(secondBucketTime, offsetDateTimeValue(rows.get(1).get("time")), columnLabel(relationName, "second_bucket.time"));
        assertEquals(secondExpectedCnt, longValue(rows.get(1).get("cnt")), columnLabel(relationName, "second_bucket.cnt"));
    }

    private void assertViewRowCount(String relationName, String serviceName, int expectedCount) {
        Integer actualCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + relationName + " WHERE service_name = ?",
                Integer.class,
                serviceName);

        assertEquals(expectedCount, actualCount, columnLabel(relationName, "row_count"));
    }

    private void assertBucketSplit(String relationName,
                                   String serviceName,
                                   short spanKind,
                                   OffsetDateTime firstBucketTime,
                                   long firstExpectedCnt,
                                   OffsetDateTime secondBucketTime,
                                   long secondExpectedCnt) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                        SELECT time, cnt
                        FROM %s
                        WHERE service_name = ?
                          AND span_kind = ?
                        ORDER BY time
                        """.formatted(relationName),
                serviceName,
                spanKind);

        assertEquals(2, rows.size(), columnLabel(relationName, "bucket_row_count"));
        assertEquals(firstBucketTime, offsetDateTimeValue(rows.get(0).get("time")), columnLabel(relationName, "first_bucket.time"));
        assertEquals(firstExpectedCnt, longValue(rows.get(0).get("cnt")), columnLabel(relationName, "first_bucket.cnt"));
        assertEquals(secondBucketTime, offsetDateTimeValue(rows.get(1).get("time")), columnLabel(relationName, "second_bucket.time"));
        assertEquals(secondExpectedCnt, longValue(rows.get(1).get("cnt")), columnLabel(relationName, "second_bucket.cnt"));
    }

    private void assertLastInsertTime(String relationName,
                                      String serviceName,
                                      short spanKind,
                                      OffsetDateTime bucketTime,
                                      OffsetDateTime expectedLastInsertTime) {
        OffsetDateTime actualLastInsertTime = jdbcTemplate.queryForObject(
                """
                        SELECT last_insert_time
                        FROM %s
                        WHERE service_name = ?
                          AND span_kind = ?
                          AND time = ?
                        """.formatted(relationName),
                OffsetDateTime.class,
                serviceName,
                spanKind,
                bucketTime);

        assertEquals(expectedLastInsertTime, actualLastInsertTime, columnLabel(relationName, "last_insert_time"));
    }

    private void assertPercentDurationRow(String relationName,
                                          String serviceName,
                                          short spanKind,
                                          OffsetDateTime bucketTime) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        SELECT approx_percentile(0.90, percent_duration) AS p90,
                               approx_percentile(0.95, percent_duration) AS p95,
                               approx_percentile(0.99, percent_duration) AS p99
                        FROM %s
                        WHERE service_name = ?
                          AND span_kind = ?
                          AND time = ?
                        """.formatted(relationName),
                serviceName,
                spanKind,
                bucketTime);

        double p90 = doubleValue(row.get("p90"));
        double p95 = doubleValue(row.get("p95"));
        double p99 = doubleValue(row.get("p99"));

        String p90Label = percentileLabel(relationName, "0.90");
        String p95Label = percentileLabel(relationName, "0.95");
        String p99Label = percentileLabel(relationName, "0.99");

        assertPercentileInRange(p90Label, p90, 8500.0, 9200.0);
        assertPercentileInRange(p95Label, p95, 9000.0, 9800.0);
        assertPercentileInRange(p99Label, p99, 9700.0, 10200.0);
        assertTrue(p90 < p95, p90Label + "/" + p95Label + " expected p90 < p95 but was " + p90 + " >= " + p95);
        assertTrue(p95 < p99, p95Label + "/" + p99Label + " expected p95 < p99 but was " + p95 + " >= " + p99);
        assertTrue(p95 - p90 >= 200.0, p95Label + " expected to be meaningfully above " + p90Label + " but gap was " + (p95 - p90));
        assertTrue(p99 - p95 >= 150.0, p99Label + " expected to be meaningfully above " + p95Label + " but gap was " + (p99 - p95));
    }

    private void assertUniformPercentDurationRow(String relationName,
                                                 String serviceName,
                                                 short spanKind,
                                                 OffsetDateTime bucketTime,
                                                 long expectedDuration) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        SELECT approx_percentile(0.90, percent_duration) AS p90,
                               approx_percentile(0.95, percent_duration) AS p95,
                               approx_percentile(0.99, percent_duration) AS p99
                        FROM %s
                        WHERE service_name = ?
                          AND span_kind = ?
                          AND time = ?
                        """.formatted(relationName),
                serviceName,
                spanKind,
                bucketTime);

        assertEquals(expectedDuration, Math.round(doubleValue(row.get("p90"))), percentileLabel(relationName, "0.90"));
        assertEquals(expectedDuration, Math.round(doubleValue(row.get("p95"))), percentileLabel(relationName, "0.95"));
        assertEquals(expectedDuration, Math.round(doubleValue(row.get("p99"))), percentileLabel(relationName, "0.99"));
    }

    private void assertPercentileInRange(String label,
                                         double actual,
                                         double minInclusive,
                                         double maxInclusive) {
        assertTrue(
                actual >= minInclusive && actual <= maxInclusive,
                label + " expected in range [" + minInclusive + ", " + maxInclusive + "] but was " + actual);
    }

    private long longValue(Object value) {
        return ((Number) value).longValue();
    }

    private double doubleValue(Object value) {
        return ((Number) value).doubleValue();
    }

    private String columnLabel(String relationName, String columnName) {
        return relationName + "." + columnName;
    }

    private String percentileLabel(String relationName, String percentile) {
        return relationName + ".approx_percentile(" + percentile + ", percent_duration)";
    }

    private OffsetDateTime offsetDateTimeValue(Object value) {
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
        throw new IllegalArgumentException("Unsupported timestamp value: " + value);
    }

    private OffsetDateTime startOfUtcDay(OffsetDateTime value) {
        return value.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    private OffsetDateTime floorToFiveMinutes(OffsetDateTime value) {
        return value.withMinute(value.getMinute() - value.getMinute() % 5)
                .withSecond(0)
                .withNano(0);
    }

    private UUID stableUuid(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }
}
