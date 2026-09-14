package org.prism.apm.ui.server.common;

import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This service parses execution plans for specified SQL statements
 */
@Component
public class IndexCheckTool {

    private final QueryPlanExplainMapper queryPlanExplainMapper;

    @Autowired
    public IndexCheckTool(QueryPlanExplainMapper queryPlanExplainMapper) {
        this.queryPlanExplainMapper = queryPlanExplainMapper;
    }

    public TestSql checkIndexUsage(TestSql sql, PrintMode printMode, PrintOption printOption) {
        List<String> query_plan_after_format = getQueryPlan(sql).stream()
                .map(s -> s.replaceAll("\t", "").replaceAll(" ", ""))
                .collect(Collectors.toList());

        //Search for filter-related content
        List<String> filter_content = query_plan_after_format.stream().filter(s -> s.toLowerCase().contains("filter")).collect(Collectors.toList());
        sql.setFilterContent(filter_content);

        //Search for index-related content
        List<String> index_content = query_plan_after_format.stream().filter(s -> s.toLowerCase().contains("index")).collect(Collectors.toList());

        //Keep lines related to index usage
        List<String> index_name_content = index_content.stream()
                .filter(c -> !c.contains("IndexCond"))
                .collect(Collectors.toList());
        sql.setContainCustomIndex(String.join(",", index_name_content).contains(sql.getIndexName()));
        if (index_name_content.isEmpty()) {
            index_name_content = query_plan_after_format.stream()
                    .filter(c -> c.contains("ParallelSeqScan"))
                    .collect(Collectors.toList());
        }
        sql.setUsingIndex(index_name_content);

        if (printOption.isPrintSwitch()) {
            printResult(sql, printMode, printOption);
        }
        return sql;
    }

    private List<String> getQueryPlan(TestSql sql) {
        List<Map<String, String>> maps = queryPlanExplainMapper.explain(sql.getSql());
        List<String> plan = maps.stream().map(m -> m.get("QUERY PLAN")).collect(Collectors.toList());
        sql.setQueryPlan(plan);
        return plan;
    }

    private String getIndexName(String line) {
        int is_index_only_scan = line.indexOf("->IndexOnlyScanusing");
        if (is_index_only_scan != -1) {
            return "Using index only: " + line.substring(is_index_only_scan + "->IndexOnlyScanusing".length(), line.indexOf("(cost"));
        }
        //->IndexScanusing_hyper_76_138_chunk_shl_test_idxon_hyper_76_138_chunk(cost=0.42..9.43rows=1width=21)(actualtime=0.042..0.042rows=0loops=1)
        int is_index_scan = line.indexOf("->IndexScanusing");
        if (is_index_scan != -1) {
            return "Using index scan: " + line.substring(is_index_scan + "->IndexScanusing".length(), line.indexOf("(cost"));
        }
        int is_bitmap_scan = line.indexOf("BitmapIndexScanon");
        if (is_bitmap_scan != -1) {
            return "Using bitmapIndex: " + line.substring(is_bitmap_scan + "BitmapIndexScanon".length(), line.indexOf("(cost"));
        }
        int is_Parallel_Seq_Scan = line.indexOf("ParallelSeqScanon");
        if (is_Parallel_Seq_Scan != -1) {
            return "Using ParallelSeqScan: " + line.substring(is_Parallel_Seq_Scan + "ParallelSeqScanon".length(), line.indexOf("(cost"));
        }
        return "Used another index type: " + line;
    }

    private void printResult(TestSql sql, PrintMode printMode, PrintOption printOption) {
        switch (printMode) {
            case CustomIndex:
                if (sql.containCustomIndex) {
                    doPrint(sql, printOption);
                }
                break;
            case NotCustomIndex:
                if (!sql.containCustomIndex) {
                    doPrint(sql, printOption);
                }
                break;
            case All:
                doPrint(sql, printOption);
                break;
        }
    }

    private void doPrint(TestSql sql, PrintOption printOption) {
        System.out.println();
        System.out.println();
        System.out.printf("## >>>>> Test case: %s %n", sql.getMapperFunctionName());
        if (printOption.isPrintIndex()) {
            System.out.printf("### ---------- Used custom index: %s----------%n", sql.isContainCustomIndex());
            sql.getUsingIndex().stream().map(this::getIndexName).forEach(System.out::println);
        }
        if (printOption.isPrintQuerySql()) {
            System.out.println();
            System.out.println("### ---------- Query SQL content ----------");
            System.out.println("```sql");
            System.out.println(sql.getSql());
            System.out.println("```");
        }
        if (printOption.printFilter) {
            System.out.printf("### ---------- Used filter: %s----------%n", !sql.getFilterContent().isEmpty());
            System.out.println("```sql");
            sql.getFilterContent().forEach(
                    f -> System.out.printf("%s%n", f)
            );
            System.out.println("```");
        }
        if (printOption.printPlan) {
            System.out.println("### ---------- Query Plan ----------");
            System.out.println("```sql");
            sql.getQueryPlan().forEach(
                    f -> System.out.printf("%s%n", f)
            );
            System.out.println("```");
        }
    }

    @Builder
    @Data
    public static class TestSql {
        private String mapperFunctionName;//SQL id in mapper
        @Builder.Default
        private String indexName = "prism";//custom index name
        private String sql;//SQL to check

        //result below
        private boolean containCustomIndex;//whether a custom index is used
        private List<String> usingIndex;//indexes used
        private List<String> filterContent;//filters used
        private List<String> queryPlan;//raw query plan
    }

    public enum PrintMode {
        All//print all check results
        , CustomIndex//only output statements using custom indexes
        , NotCustomIndex//only output statements not using custom indexes
    }

    @Data
    @Builder
    public static class PrintOption {
        @Builder.Default
        private boolean printSwitch = true;//master print switch
        @Builder.Default
        private boolean printIndex = true;//whether to print index content
        private boolean printFilter;//whether to print filter content
        private boolean printPlan;//whether to print the raw plan
        private boolean printQuerySql;//whether to print executed SQL
    }

}
