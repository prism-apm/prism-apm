package org.prism.apm.ui.server.index;

import com.google.common.collect.Lists;
import org.prism.apm.ui.server.common.IndexCheckTool;
import org.prism.apm.ui.server.common.NonTransactionalMybatisTestBase;
import org.prism.apm.ui.server.common.QueryPlanExplainMapper;
import org.prism.apm.ui.server.utils.JsonUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import java.util.List;

/**
 * This test
 *  1.Uses indexCheck/index-init.sql to create service_call and service_minute, and inserts a fixed 10k rows
 *  2.Disables the default groupBy indexes for continuous aggregates
 *  3.Uses custom indexes
 *  4.Checks index coverage with business statements using fixed conditions
 */
@RunWith(SpringRunner.class)
public class IndexCheckUnitStyleTest extends NonTransactionalMybatisTestBase {

    @Autowired
    private QueryPlanExplainMapper queryPlanExplainMapper;

    @Before
    public void setUp() {
        super.refreshContinuousAggregate();
    }

    /**
     * Checks index coverage; if no custom index is used, raises an error and outputs the problematic SQL
     */
    @Test
    @Sql(scripts = {"classpath:index-check/index-init.sql", "classpath:index-check/data-insert.sql"})
    public void checkIndexUsageWithAssert() {
        System.out.println("===== index-init.sql execute OVER =====");
        System.out.println("=================================");
        IndexCheckTool indexCheckTool = new IndexCheckTool(queryPlanExplainMapper);
        for (IndexCheckTool.TestSql sql : waitForTest) {
            IndexCheckTool.TestSql testSql_result = indexCheckTool.checkIndexUsage(sql
                    , IndexCheckTool.PrintMode.All
                    , IndexCheckTool.PrintOption.builder()
                            .printIndex(true)
                            .printQuerySql(true)
                            .printPlan(true)
                            .printFilter(true)
                            .build()
            );
            Assert.isTrue(testSql_result.isContainCustomIndex(), String.format("SQL failed validation:%s\n", JsonUtils.toJson(testSql_result)));
        }
        System.out.println("===== Unit Test execute OVER =====");
        System.out.println("==================================");
    }

    private static final List<IndexCheckTool.TestSql> waitForTest = Lists.newArrayList(
            // ======================= Corresponds to ServiceCallMinuteDOMapper.xml in queryServiceSummaryByServiceName =======================
            IndexCheckTool.TestSql.builder().sql(
                    "select\n" +
                            "    service_name as serviceName,\n" +
                            "    ROUND(SUM(avg_duration*cnt)/SUM(cnt), 2) as avgDuration,\n" +
                            "    sum(cnt) as totalCount,\n" +
                            "    sum(error_num) as errorCount\n" +
                            "  from service_call_minute\n" +
                            "  where\n" +
                            "    service_name in ('service_1', 'service_2')\n" +
                            "    and span_kind = 3\n" + // Fix: generated data does not contain span_kind 2
                            "    and time BETWEEN now() - interval '1 day' and now()\n" + // Fix: use a dynamic correct time range
                            "  group by service_name"
            ).mapperFunctionName("queryServiceSummaryByServiceName").build(),

            // ======================= Corresponds to ServiceCallMinuteDOMapper.xml in queryMachineByServiceName =======================
            IndexCheckTool.TestSql.builder().sql(
                    "select\n" +
                            "    host_name\n" +
                            "  from service_call_minute\n" +
                            "  where\n" +
                            "    service_name = 'service_1'\n" +
                            "    and span_kind = 3\n" + // Fix: generated data does not contain span_kind 2
                            "    and time BETWEEN now() - interval '1 day' and now()\n" + // Fix: use a dynamic correct time range
                            "  group by host_name"
            ).mapperFunctionName("queryMachineByServiceName").build(),

            // ======================= Corresponds to ServiceCallMinuteDOMapper.xml in queryServiceDetailByServiceName =======================
            IndexCheckTool.TestSql.builder().sql(
                    "select\n" +
                            "    time_bucket('1 minute', time) as timeb,\n" +
                            "    ROUND(SUM(avg_duration*cnt)/SUM(cnt), 2) as avgDuration,\n" +
                            "    sum(cnt) as totalCount,\n" +
                            "    ROUND(avg(error_num), 2) as avgError,\n" +
                            "    ROUND(avg(cnt),2) as avgTotal\n" +
                            "  from service_call_minute\n" +
                            "  where\n" +
                            "    service_name = 'service_1'\n" +
                            "    and span_kind = 3\n" + // Fix: generated data does not contain span_kind 2
                            "    and host_name = 'host_1'\n" +
                            "    and time BETWEEN now() - interval '1 day' and now()\n" + // Fix: use a dynamic correct time range
                            "  group by timeb"
            ).mapperFunctionName("queryServiceDetailByServiceName(with host_name)").build(),

            IndexCheckTool.TestSql.builder().sql(
                    "select\n" +
                            "    time_bucket('1 minute', time) as timeb,\n" +
                            "    ROUND(SUM(avg_duration*cnt)/SUM(cnt), 2) as avgDuration,\n" +
                            "    sum(cnt) as totalCount,\n" +
                            "    ROUND(avg(error_num), 2) as avgError,\n" +
                            "    ROUND(avg(cnt),2) as avgTotal\n" +
                            "  from service_call_minute\n" +
                            "  where\n" +
                            "    service_name = 'service_1'\n" +
                            "    and span_kind = 3\n" + // Fix: generated data does not contain span_kind 2
                            "    and time BETWEEN now() - interval '1 day' and now()\n" + // Fix: use a dynamic correct time range
                            "  group by timeb"
            ).mapperFunctionName("queryServiceDetailByServiceName(without host_name)").build(),

            // ======================= Corresponds to ServiceCallMinuteDOMapper.xml in queryTargetSummaryByServiceName =======================
            IndexCheckTool.TestSql.builder().sql(
                    "select\n" +
                            "    target_name as targetName,\n" +
                            "    ROUND(SUM(avg_duration*cnt)/SUM(cnt), 2) as avgDuration,\n" +
                            "    ROUND(avg(cnt), 2) as avgTotal,\n" +
                            "    ROUND(max(max_duration), 2) as max,\n" +
                            "    ROUND(min(min_duration), 2) as min,\n" +
                            "    sum(cnt) as totalCount,\n" +
                            "    sum(error_num) as errorCount,\n" +
                            "    approx_percentile(0.90, rollup(percent_duration)) AS p90,\n" +
                            "    approx_percentile(0.95, rollup(percent_duration)) AS p95,\n" +
                            "    approx_percentile(0.99, rollup(percent_duration)) AS p99\n" +
                            "  from service_call_minute\n" +
                            "  where\n" +
                            "    service_name = 'service_1'\n" +
                            "    and span_kind = 3\n" + // Fix: generated data does not contain span_kind 2
                            "    and host_name = 'host_2'\n" +
                            "    and time BETWEEN now() - interval '1 day' and now()\n" + // Fix: use a dynamic correct time range
                            "    group by target_name"
            ).mapperFunctionName("queryTargetSummaryByServiceName(with host_name)").build(),

            IndexCheckTool.TestSql.builder().sql(
                    "select\n" +
                            "    target_name as targetName,\n" +
                            "    ROUND(SUM(avg_duration*cnt)/SUM(cnt), 2) as avgDuration,\n" +
                            "    ROUND(avg(cnt), 2) as avgTotal,\n" +
                            "    ROUND(max(max_duration), 2) as max,\n" +
                            "    ROUND(min(min_duration), 2) as min,\n" +
                            "    sum(cnt) as totalCount,\n" +
                            "    sum(error_num) as errorCount,\n" +
                            "    approx_percentile(0.90, rollup(percent_duration)) AS p90,\n" +
                            "    approx_percentile(0.95, rollup(percent_duration)) AS p95,\n" +
                            "    approx_percentile(0.99, rollup(percent_duration)) AS p99\n" +
                            "  from service_call_minute\n" +
                            "  where\n" +
                            "    service_name = 'service_1'\n" +
                            "    and span_kind = 3\n" + // Fix: generated data does not contain span_kind 2
                            "    and time BETWEEN now() - interval '1 day' and now()\n" + // Fix: use a dynamic correct time range
                            "    group by target_name"
            ).mapperFunctionName("queryTargetSummaryByServiceName(without host_name)").build(),

            // ======================= Corresponds to ServiceCallMinuteDOMapper.xml in queryTargetDetailByTargetName =======================
            IndexCheckTool.TestSql.builder().sql(
                    "select\n" +
                            "    time_bucket('1 minute', time) as timeb,\n" +
                            "    ROUND(SUM(avg_duration*cnt)/SUM(cnt), 2) as avgDuration,\n" +
                            "    sum(cnt) as totalCount,\n" +
                            "    sum(error_num) as errorCount,\n" +
                            "    approx_percentile(0.90, rollup(percent_duration)) AS p90,\n" +
                            "    approx_percentile(0.95, rollup(percent_duration)) AS p95,\n" +
                            "    approx_percentile(0.99, rollup(percent_duration)) AS p99\n" +
                            "  from service_call_minute\n" +
                            "  where\n" +
                            "    service_name = 'service_2'\n" +
                            "    and span_kind = 3\n" + // Fix: generated data does not contain span_kind 2
                            "    and hashtext(target_name) = hashtext('target_1')\n" +
                            "    and host_name = 'host_3'\n" +
                            "    and time BETWEEN now() - interval '1 day' and now()\n" + // Fix: use a dynamic correct time range
                            "  group by timeb\n" +
                            "  order by timeb asc"
            ).mapperFunctionName("queryTargetDetailByTargetName(with host_name)").build(),

            IndexCheckTool.TestSql.builder().sql(
                    "select\n" +
                            "    time_bucket('1 minute', time) as timeb,\n" +
                            "    ROUND(SUM(avg_duration*cnt)/SUM(cnt), 2) as avgDuration,\n" +
                            "    sum(cnt) as totalCount,\n" +
                            "    sum(error_num) as errorCount,\n" +
                            "    approx_percentile(0.90, rollup(percent_duration)) AS p90,\n" +
                            "    approx_percentile(0.95, rollup(percent_duration)) AS p95,\n" +
                            "    approx_percentile(0.99, rollup(percent_duration)) AS p99\n" +
                            "  from service_call_minute\n" +
                            "  where\n" +
                            "    service_name = 'service_2'\n" +
                            "    and span_kind = 3\n" + // Fix: generated data does not contain span_kind 2
                            "    and hashtext(target_name) = hashtext('target_1')\n" +
                            "    and time BETWEEN now() - interval '1 day' and now()\n" + // Fix: use a dynamic correct time range
                            "  group by timeb\n" +
                            "  order by timeb asc"
            ).mapperFunctionName("queryTargetDetailByTargetName(without host_name)").build(),

            // ======================= Corresponds to ServiceCallMinuteDOMapper.xml in queryInnerActionSummaryByTargetName =======================
            IndexCheckTool.TestSql.builder().sql(
                    "select\n" +
                            "    target_name as targetName,\n" +
                            "    system_type as SystemType,\n" +
                            "    sum(cnt) as totalCount,\n" +
                            "    ROUND(SUM(avg_duration*cnt)/SUM(cnt), 2) as avgDuration\n" +
                            "  from service_call_minute\n" +
                            "  where\n" +
                            "    caller_service_name = 'caller_service_1'\n" +
                            "    and caller_span_kind = 2\n" + // OK: caller_span_kind may be 2
                            "    and span_kind = 3\n" +
                            "    and hashtext(caller_target_name) = hashtext('caller_target_1')\n" +
                            "    and host_name = 'host_1'\n" +
                            "    and time BETWEEN now() - interval '1 day' and now()\n" + // Fix: use a dynamic correct time range
                            "  group by system_type, target_name"
            ).mapperFunctionName("queryInnerActionSummaryByTargetName(with host_name)").build(),

            IndexCheckTool.TestSql.builder().sql(
                    "select\n" +
                            "    target_name as targetName,\n" +
                            "    system_type as SystemType,\n" +
                            "    sum(cnt) as totalCount,\n" +
                            "    ROUND(SUM(avg_duration*cnt)/SUM(cnt), 2) as avgDuration\n" +
                            "  from service_call_minute\n" +
                            "  where\n" +
                            "    caller_service_name = 'caller_service_1'\n" +
                            "    and caller_span_kind = 2\n" + // OK: caller_span_kind may be 2
                            "    and span_kind = 3\n" +
                            "    and hashtext(caller_target_name) = hashtext('caller_target_1')\n" +
                            "    and time BETWEEN now() - interval '1 day' and now()\n" + // Fix: use a dynamic correct time range
                            "  group by system_type, target_name"
            ).mapperFunctionName("queryInnerActionSummaryByTargetName(without host_name)").build(),

            // ======================= Corresponds to ServiceCallMinuteDOMapper.xml in queryInnerTargetDetailByTargetName =======================
            IndexCheckTool.TestSql.builder().sql(
                    "select\n" +
                            "    time_bucket('1 minute', time) as timeb,\n" +
                            "    target_name as targetName,\n" +
                            "    system_type as systemType,\n" +
                            "    sum(cnt) as totalCount,\n" +
                            "    sum(error_num) as errorCount,\n" +
                            "    ROUND(SUM(avg_duration*cnt)/SUM(cnt), 2) as avgDuration\n" +
                            "  from service_call_minute\n" +
                            "  where\n" +
                            "    caller_service_name = 'caller_service_1'\n" +
                            "    and caller_span_kind = 2\n" + // OK: caller_span_kind may be 2
                            "    and span_kind = 3\n" +
                            "    and hashtext(caller_target_name) = hashtext('caller_target_1')\n" +
                            "    and host_name = 'host_1'\n" +
                            "    and time BETWEEN now() - interval '1 day' and now()\n" + // Fix: use a dynamic correct time range
                            "  group by target_name, timeb, systemType\n" +
                            "  order by timeb"
            ).mapperFunctionName("queryInnerTargetDetailByTargetName(with host_name)").build(),

            IndexCheckTool.TestSql.builder().sql(
                    "select\n" +
                            "    time_bucket('1 minute', time) as timeb,\n" +
                            "    target_name as targetName,\n" +
                            "    system_type as systemType,\n" +
                            "    sum(cnt) as totalCount,\n" +
                            "    sum(error_num) as errorCount,\n" +
                            "    ROUND(SUM(avg_duration*cnt)/SUM(cnt), 2) as avgDuration\n" +
                            "  from service_call_minute\n" +
                            "  where\n" +
                            "    caller_service_name = 'caller_service_1'\n" +
                            "    and caller_span_kind = 2\n" + // OK: caller_span_kind may be 2
                            "    and span_kind = 3\n" +
                            "    and hashtext(caller_target_name) = hashtext('caller_target_1')\n" +
                            "    and time BETWEEN now() - interval '1 day' and now()\n" + // Fix: use a dynamic correct time range
                            "  group by target_name, timeb, systemType\n" +
                            "  order by timeb"
            ).mapperFunctionName("queryInnerTargetDetailByTargetName(without host_name)").build(),

            // ======================= Corresponds to ServiceCallMinuteDOMapper.xml in queryDbSummaryByServiceName =======================
            IndexCheckTool.TestSql.builder().sql(
                    "select\n" +
                            "   target_name                 as targetName,\n" +
                            "   caller_target_name          as callerTargetName,\n" +
                            "   ROUND(SUM(avg_duration*cnt)/SUM(cnt), 2) as avgDuration,\n" +
                            "   ROUND(avg(cnt), 2)          as avgTotal,\n" +
                            "   sum(cnt)                    as totalCount,\n" +
                            "   sum(error_num)              as errorCount\n" +
                            "from service_call_minute\n" +
                            "where\n" +
                            "  service_name = 'service_1'\n" +
                            "  and system_type = 'OS'\n" +
                            "  and span_kind = 3\n" +
                            "  and time BETWEEN now() - interval '1 day' and now()\n" + // Fix: use a dynamic correct time range
                            "group by caller_target_name, target_name"
            ).mapperFunctionName("queryDbSummaryByServiceName").build(),

            // ======================= Corresponds to ServiceCallMinuteDOMapper.xml in queryDbDetailByServiceName =======================
            IndexCheckTool.TestSql.builder().sql(
                    "select\n" +
                            "    time_bucket('1 minute', time) as timeb,\n" +
                            "    ROUND(avg(cnt), 2) as avgTotal,\n" +
                            "    ROUND(avg(error_num), 2) as avgError,\n" +
                            "    ROUND(SUM(avg_duration*cnt)/SUM(cnt), 2) as avgDuration\n" +
                            "  from service_call_minute\n" +
                            "  where\n" +
                            "    service_name = 'service_1'\n" +
                            "    and span_kind = 3\n" +
                            "    and hashtext(target_name) = hashtext('target_1')\n" +
                            "    and system_type = 'OS'\n" +
                            "    and host_name = 'host_1'\n" +
                            "    and time BETWEEN now() - interval '1 day' and now()\n" + // Fix: use a dynamic correct time range
                            "    and hashtext(caller_target_name) = hashtext('caller_target_1')\n" +
                            "  group by timeb\n" +
                            "  order by timeb asc"
            ).mapperFunctionName("queryDbDetailByServiceName(with host_name and system_type)").build(),

            IndexCheckTool.TestSql.builder().sql(
                    "select\n" +
                            "    time_bucket('1 minute', time) as timeb,\n" +
                            "    ROUND(avg(cnt), 2) as avgTotal,\n" +
                            "    ROUND(avg(error_num), 2) as avgError,\n" +
                            "    ROUND(SUM(avg_duration*cnt)/SUM(cnt), 2) as avgDuration\n" +
                            "  from service_call_minute\n" +
                            "  where\n" +
                            "    service_name = 'service_1'\n" +
                            "    and span_kind = 3\n" +
                            "    and hashtext(target_name) = hashtext('target_1')\n" +
                            "    and host_name = 'host_1'\n" +
                            "    and time BETWEEN now() - interval '1 day' and now()\n" + // Fix: use a dynamic correct time range
                            "    and hashtext(caller_target_name) = hashtext('caller_target_1')\n" +
                            "  group by timeb\n" +
                            "  order by timeb asc"
            ).mapperFunctionName("queryDbDetailByServiceName(without system_type)").build(),

            // ======================= Corresponds to ServiceCallMinuteDOMapper.xml in queryServiceIfaceWhoCallMe (core logic) =======================
            IndexCheckTool.TestSql.builder().sql(
                    "SELECT\n" +
                            "    caller_service_name as callerServiceName,\n" +
                            "    caller_target_name as callerTargetName,\n" +
                            "    SUM ( avg_duration * cnt ) AS totalTime,\n" +
                            "    SUM ( cnt ) AS totalCount ,\n" +
                            "    ROUND( SUM ( avg_duration * cnt )/SUM ( cnt ), 2) as avgTotal\n" +
                            "  FROM\n" +
                            "    service_call_minute\n" +
                            "  WHERE\n" +
                            "    service_name = 'service_1'\n" +
                            "    and span_kind = 3\n" + // Fix: generated data does not contain span_kind 2
                            "    and hashtext(target_name) = hashtext('target_1')\n" +
                            "    and host_name = 'host_1'\n" +
                            "    and time BETWEEN now() - interval '1 day' and now()\n" + // Fix: use a dynamic correct time range
                            "  GROUP BY\n" +
                            "    caller_service_name, caller_target_name\n" +
                            "  order by totalTime desc\n" +
                            "  LIMIT 10 OFFSET 0"
            ).mapperFunctionName("queryServiceIfaceWhoCallMe(with host_name)").build(),

            IndexCheckTool.TestSql.builder().sql(
                    "SELECT\n" +
                            "    caller_service_name as callerServiceName,\n" +
                            "    caller_target_name as callerTargetName,\n" +
                            "    SUM ( avg_duration * cnt ) AS totalTime,\n" +
                            "    SUM ( cnt ) AS totalCount ,\n" +
                            "    ROUND( SUM ( avg_duration * cnt )/SUM ( cnt ), 2) as avgTotal\n" +
                            "  FROM\n" +
                            "    service_call_minute\n" +
                            "  WHERE\n" +
                            "    service_name = 'service_1'\n" +
                            "    and span_kind = 3\n" + // Fix: generated data does not contain span_kind 2
                            "    and hashtext(target_name) = hashtext('target_1')\n" +
                            "    and time BETWEEN now() - interval '1 day' and now()\n" + // Fix: use a dynamic correct time range
                            "  GROUP BY\n" +
                            "    caller_service_name, caller_target_name\n" +
                            "  order by totalTime desc\n" +
                            "  LIMIT 10 OFFSET 0"
            ).mapperFunctionName("queryServiceIfaceWhoCallMe(without host_name)").build()
    );

}
