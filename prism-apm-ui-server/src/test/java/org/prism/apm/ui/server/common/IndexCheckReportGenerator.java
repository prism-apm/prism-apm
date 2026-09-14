package org.prism.apm.ui.server.common;

import com.google.common.collect.Lists;
import org.prism.apm.ui.server.Application;
import org.prism.apm.ui.server.utils.DateUtils;
import org.prism.apm.ui.server.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This tool checks index coverage against real data in test and production environments and outputs a report
 * How to switch environments:
 *  Edit VM options in the IDEA run configuration: -Dspring.profiles.active=xxx
 */
@SpringBootTest(classes = Application.class)
@Component
public class IndexCheckReportGenerator {

    private static final List<IndexCheckTool.TestSql> waitForTest = new ArrayList<>();

    @Autowired
    private IndexCheckTool indexCheckTool;

    public static void main(String[] args) {
        // Start the Spring Boot application and get the context
        // Note: Application.class is the main Spring Boot startup class
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);

        // Get the current class Bean instance from the context
        IndexCheckReportGenerator generator = context.getBean(IndexCheckReportGenerator.class);

        prepareTestData().forEach(
                test -> test.setSql(replaceCondition(test.getSql()))
        );

        try {
            // Now methods can be called
            System.out.println("--- Starting printQueryPlanCheckResult ---");
            generator.printQueryPlanCheckResult();

            System.out.println("\n--- Starting checkIndexUsageWithAssert ---");
            generator.checkIndexUsageWithAssert();

            System.out.println("\n--- All tasks completed ---");

        } finally {
            // Close the Spring context and release resources
            context.close();
        }
    }

    /**
     * Output the index coverage report
     */
    public void printQueryPlanCheckResult() {
        System.out.printf("Checking index coverage..., total cases: %s, current time: %s%n", waitForTest.size(), DateUtils.formatDateTime(new Date()));
        for (IndexCheckTool.TestSql sql : waitForTest) {
            indexCheckTool.checkIndexUsage(sql
                    , IndexCheckTool.PrintMode.All //Switch output mode to control reports for uncovered indexes
                    , IndexCheckTool.PrintOption.builder()
                            .printIndex(true)
                            .printQuerySql(true)
                            .printPlan(true)
                            .printFilter(true).build()
            );
        }
    }

    /**
     * Check SQL coverage
     * If uncovered SQL is detected, abort and output the uncovered SQL information
     */
    public void checkIndexUsageWithAssert() {
        for (IndexCheckTool.TestSql sql : waitForTest) {
            System.out.println("Checking index coverage: " + sql.getSql());
            IndexCheckTool.TestSql testSql_result = indexCheckTool.checkIndexUsage(sql
                    , IndexCheckTool.PrintMode.All
                    , IndexCheckTool.PrintOption.builder()
                            .printSwitch(false)
                            .build()
            );
            Assert.isTrue(testSql_result.isContainCustomIndex(), String.format("SQL failed validation:%s\n", JsonUtils.toJson(testSql_result)));
        }
    }

    private static String replaceCondition(String sql) {
        // Use a regex to remove time conditions; insensitive to surrounding spaces and newlines for better robustness
        String updatedSql = sql.replaceAll("and time BETWEEN '2024-10-01'::timestamp and '2024-10-31'::timestamp", "");

        // Continue handling other replacements
        return updatedSql
                .replace("#{target_name}", "/data/permission/config") // Note: SQL string values usually require quotes
                .replace("#{caller_target_name}", "/staffService/queryTaskTodoNum") // Note: SQL string values usually require quotes
                .replace("#{host_name}", "sample-rce-app-test-v-56d89984c6-nx2dz"); // Note: SQL string values usually require quotes
    }

    private static List<IndexCheckTool.TestSql> prepareTestData() {
        waitForTest.addAll(
                Lists.newArrayList(
                        //Add the SQL you want to test here; conditions need to be written according to actual data.
                )
        );
        return waitForTest;
    }

}
