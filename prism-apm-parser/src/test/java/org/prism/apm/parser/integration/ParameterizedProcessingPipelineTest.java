package org.prism.apm.parser.integration;

import org.prism.apm.parser.IntegrationTestBase;
import org.prism.apm.parser.cache.CacheService;
import org.prism.apm.parser.cache.EhcacheService;
import org.prism.apm.parser.config.*;
import org.prism.apm.parser.service.save.ResourceHashResolver;
import org.prism.apm.parser.repository.ResourceHashDictionaryMapper;
import org.prism.apm.parser.constants.Constants;
import org.prism.apm.parser.delay.DelayQueueDelayService;
import org.prism.apm.parser.delay.DelayService;
import org.prism.apm.parser.delay.DelayedSpan;
import org.prism.apm.parser.model.entity.ServiceCallEntity;
import org.prism.apm.parser.repository.ServiceCallMapper;
import org.prism.apm.parser.service.common.JsonService;
import org.prism.apm.parser.service.init.SpanInitProcessService;
import org.prism.apm.parser.service.init.bo.SpanFieldReplacer;
import org.prism.apm.parser.service.parser.*;
import org.prism.apm.parser.service.save.SpanSaveService;
import org.prism.apm.parser.service.save.bo.SpanSaveBo;
import org.prism.apm.parser.service.save.bo.SpanSaveOtherProcessor;
import org.prism.apm.parser.service.save.bo.SpanSaveProcessor;
import org.prism.apm.parser.service.save.bo.SpanSaveServerProcessor;
import org.prism.apm.parser.service.stats.StatsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource(properties = {
        // Warning: do not set delayMillis below 1000.
        // A smaller value may cause the first case to fail because it won't find the
        // inserted data.
        "delayMillis = 1000",
        "dependency.timeoutThreshold = 3000",
        "executor.service.shutdown.timeout_ms = 3000",
        "logging.level.root=debug",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
public class ParameterizedProcessingPipelineTest extends IntegrationTestBase {

    @MockBean
    private KafkaProperties kafkaProperties;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private SpanInitProcessService spanInitService;

    @Autowired
    private ServiceCallMapper mapper;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private ResourceHashDictionaryMapper resourceHashDictionaryMapper;

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        public OtelParserConfigLoader otelParserConfigLoader() throws FileNotFoundException {
            return new OtelParserConfigLoader("integration-test-cases/integration-parser-component-config.yml");
        }
    }

    @Autowired
    private JdbcTemplate jdbcTemplate; // JdbcTemplate is simpler and more direct here.

    @AfterEach
    public void cleanup() {
        // 1. Clear the service_call table in the database.
        // Use JdbcTemplate to execute the delete operation so the Mapper interface does not need changes.
        // Replace "service_call" with your actual table name.
        jdbcTemplate.execute("DELETE FROM service_call");

        cacheService.clear();

    }

    public static Stream<Arguments> pipelineTestCases() throws IOException {

        boolean allowlistEnable = false;
        List<String> includedCases = Arrays.asList();
        // List<String> includedCases = Arrays.asList("test-case-to-test");

        List<Arguments> testCases = new ArrayList<>();
        String basePath = "src/test/resources/integration-test-cases";
        File baseDir = new File(basePath);
        File[] caseDirs = baseDir.listFiles(File::isDirectory);

        Arrays.sort(caseDirs);

        if (caseDirs != null) {
            for (File caseDir : caseDirs) {
                String caseName = caseDir.getName();

                // Skip folders whose names are in the blacklist
                if (allowlistEnable && !includedCases.contains(caseName)) {
                    System.out.println("Skipping test case: " + caseName);
                    continue;
                }

                String tracePath = Paths.get(caseDir.getPath(), "trace.json").toString();
                String expectedPath = Paths.get(caseDir.getPath(), "expected.yml").toString();

                File traceFile = new File(tracePath);
                File expectedFile = new File(expectedPath);

                if (traceFile.exists() && expectedFile.exists()) {
                    // read multi lines from trace file
                    List<String> traceJsonLines = Files.readAllLines(Paths.get(tracePath));
                    String expectedYaml = new String(Files.readAllBytes(Paths.get(expectedPath)));
                    testCases.add(Arguments.of(caseName, traceJsonLines, expectedYaml));
                }
            }
        }
        return testCases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pipelineTestCases")
    // Changed: accept multi-line otelJsons
    public void testOpenTelemetryPipeline(String caseName, List<String> otelJsons, String expectedYaml)
            throws Exception {
        System.out.println("Running test case: " + caseName);
        // Changed: print multi-line otelJsons
        System.out.println("otelJsons:" + otelJsons);

        // Changed: pass multi-line otelJsons directly
        spanInitService.handleSpan(otelJsons, Constants.SPAN_PROCESS_TYPE_FIRST);

        // --- Assert ---
        List<Map<String, Object>> expectedResults = new Yaml().load(expectedYaml);
        assertNotNull(expectedResults, "Yaml.load() returned null.");
        assertFalse(expectedResults.isEmpty(), "Yaml.load() returned empty list.");

        await().atMost(10, TimeUnit.SECONDS) // Set the maximum wait time to 10 seconds
                .pollInterval(1, TimeUnit.SECONDS) // Set the polling interval to 1 second
                .until(() -> mapper.selectAll().size(), equalTo(expectedResults.size()));

        List<ServiceCallEntity> allResults = mapper.selectAll();
        assertServiceCallsMatch(allResults, expectedResults, caseName);
    }

    private void assertServiceCallsMatch(List<ServiceCallEntity> actual, List<Map<String, Object>> expected,
            String caseName) {
        assertEquals(expected.size(), actual.size(), "Mismatch in number of records for case: " + caseName);

        actual.sort(Comparator.comparing(ServiceCallEntity::getSpanId));
        expected.sort(Comparator.comparing(e -> (String) e.get("spanId")));

        for (int i = 0; i < actual.size(); i++) {
            ServiceCallEntity actualRecord = actual.get(i);
            Map<String, Object> expectedRecord = expected.get(i);
            String spanId = actualRecord.getSpanId();

            assertField(spanId, "time", actualRecord.getTime(), expectedRecord.get("time"));
            assertField(spanId, "spanId", actualRecord.getSpanId(), expectedRecord.get("spanId"));
            assertField(spanId, "traceId", actualRecord.getTraceId(), expectedRecord.get("traceId"));
            assertField(spanId, "serviceName", actualRecord.getServiceName(), expectedRecord.get("serviceName"));
            assertField(spanId, "hostName", actualRecord.getHostName(), expectedRecord.get("hostName"));
            assertField(spanId, "spanKind", actualRecord.getSpanKind(), expectedRecord.get("spanKind"));
            assertField(spanId, "component", actualRecord.getComponent(), expectedRecord.get("component"));
            assertField(spanId, "systemType", actualRecord.getSystemType(), expectedRecord.get("systemType"));
            assertField(spanId, "actionName", actualRecord.getActionName(), expectedRecord.get("actionName"));
            // targetName is stored as UUID hash, verify via dictionary lookup
            assertHashedField(spanId, "targetName", actualRecord.getTargetNameId(), expectedRecord.get("targetName"));
            assertField(spanId, "remoteAddr", actualRecord.getRemoteAddr(), expectedRecord.get("remoteAddr"));
            assertField(spanId, "caller_time", actualRecord.getCallerTime(), expectedRecord.get("caller_time"));
            assertField(spanId, "caller_spanId", actualRecord.getCallerSpanId(), expectedRecord.get("caller_spanId"));
            assertField(spanId, "caller_serviceName", actualRecord.getCallerServiceName(),
                    expectedRecord.get("caller_serviceName"));
            assertField(spanId, "caller_hostName", actualRecord.getCallerHostName(),
                    expectedRecord.get("caller_hostName"));
            assertField(spanId, "caller_spanKind", actualRecord.getCallerSpanKind(),
                    expectedRecord.get("caller_spanKind"));
            assertField(spanId, "caller_component", actualRecord.getCallerComponent(),
                    expectedRecord.get("caller_component"));
            assertField(spanId, "caller_systemType", actualRecord.getCallerSystemType(),
                    expectedRecord.get("caller_systemType"));
            assertField(spanId, "caller_actionName", actualRecord.getCallerActionName(),
                    expectedRecord.get("caller_actionName"));
            // callerTargetName is stored as UUID hash, verify via dictionary lookup
            assertHashedField(spanId, "caller_targetName", actualRecord.getCallerTargetNameId(),
                    expectedRecord.get("caller_targetName"));
            assertField(spanId, "caller_remoteAddr", actualRecord.getCallerRemoteAddr(),
                    expectedRecord.get("caller_remoteAddr"));
            assertField(spanId, "duration", actualRecord.getDuration(), expectedRecord.get("duration"));
            assertField(spanId, "cnt", actualRecord.getCnt(), expectedRecord.get("cnt"));
            assertField(spanId, "error", actualRecord.getError(), expectedRecord.get("error"));
        }
    }

    private void assertField(String spanId, String fieldName, Object actual, Object expected) {
        if (expected == null)
            return;
        Object coercedExpected = actual.getClass().equals(Long.class) && expected instanceof Integer
                ? ((Integer) expected).longValue()
                : expected;
        coercedExpected = actual.getClass().equals(Short.class) && expected instanceof Integer
                ? ((Integer) expected).shortValue()
                : coercedExpected;

        coercedExpected = actual.getClass().equals(OffsetDateTime.class) && expected instanceof String
                ? OffsetDateTime.parse(((String) expected))
                : coercedExpected;

        assertEquals(coercedExpected, actual, String.format("Field '%s' mismatch for spanId %s", fieldName, spanId));
    }

    private static final String CATEGORY_TARGET_NAME = "target_name";

    /**
     * Verify a hashed field by looking up the original text via
     * ResourceHashDictionaryMapper.
     * For callerTargetName, if the expected value is null or empty, we expect the
     * default UUID (all zeros).
     */
    private void assertHashedField(String spanId, String fieldName, UUID actualHashId, Object expected) {
        if (expected == null) {
            // For caller fields, null expected means we should have the default UUID
            if (fieldName.startsWith("caller_")) {
                assertEquals(ResourceHashResolver.DEFAULT_UUID, actualHashId,
                        String.format("Field '%s' should be default UUID for spanId %s", fieldName, spanId));
            }
            return;
        }

        String expectedText = (String) expected;
        if (expectedText.isEmpty()) {
            // For caller fields, empty expected means we should have the default UUID
            if (fieldName.startsWith("caller_")) {
                assertEquals(ResourceHashResolver.DEFAULT_UUID, actualHashId,
                        String.format("Field '%s' should be default UUID for empty value, spanId %s", fieldName,
                                spanId));
            }
            return;
        }

        // Look up the actual text from the dictionary
        var entity = resourceHashDictionaryMapper.selectByHashKeyAndCategory(actualHashId, CATEGORY_TARGET_NAME);
        assertNotNull(entity, String.format("Hash dictionary entry not found for field '%s', hashId=%s, spanId=%s",
                fieldName, actualHashId, spanId));
        assertEquals(expectedText, entity.getTextValue(),
                String.format("Field '%s' mismatch for spanId %s", fieldName, spanId));
    }

    private SpanInitProcessService generateSpanInitProcessService(CacheService cacheService,
            DelayQueue<DelayedSpan> delayQueue, JsonService jsonService) throws FileNotFoundException {
        OtelParserConfigLoader loader = new OtelParserConfigLoader(
                "integration-test-cases/integration-parser-component-config.yml");
        OtelValueExtractor extractor = new OtelValueExtractor();
        OtelRuleProvider provider = new OtelRuleProvider(loader, extractor);
        OpenTelemetryTraceDeserializer deserializer = new OpenTelemetryTraceDeserializer();
        SpanFieldReplacer replacer = new SpanFieldReplacer();
        OpenTelemetrySpanInfoBuilder builder = new OpenTelemetrySpanInfoBuilder(provider, extractor, replacer);
        ISpanParser otelParser = new OpenTelemetrySpanParser(deserializer, builder);

        DelayService delayService = new DelayQueueDelayService(delayQueue, null);

        SpanInitProcessService spanInitService = new SpanInitProcessService();
        spanInitService.setParser(otelParser);
        spanInitService.setCacheService(cacheService);
        spanInitService.setDelayService(delayService);
        spanInitService.setServiceParseConfig(new ServiceParseConfig());
        spanInitService.setServiceParseCheck(false);
        spanInitService.setJsonService(jsonService);
        return spanInitService;
    }

    private SpanSaveService generateSpanSaveService(CacheService cacheService, ServiceCallMapper svcCallMapper,
            long timeoutThreshold, JsonService jsonService) {
        StatsService statsService = new StatsService();
        SpanSaveBo spanSaveBo = new SpanSaveBo();
        SpanSaveServerProcessor serverProcessor = new SpanSaveServerProcessor(spanSaveBo);
        SpanSaveOtherProcessor otherProcessor = new SpanSaveOtherProcessor(spanSaveBo);
        SpanSaveProcessor spanSaveProcessor = new SpanSaveProcessor(spanSaveBo, serverProcessor, otherProcessor,
                timeoutThreshold, statsService);

        ResourceHashResolver resourceHashResolver = new ResourceHashResolver();
        resourceHashResolver.setCacheService(cacheService);

        return new SpanSaveService(svcCallMapper, cacheService, spanSaveProcessor, statsService, jsonService,
                resourceHashDictionaryMapper, resourceHashResolver);
    }

    private EhcacheService generateEhcacheService() {
        CacheConfig cacheConfig = new CacheConfig(1000, 600, 25, 1000);
        EhcacheConfig ehcacheConfig = new EhcacheConfig(cacheConfig);
        ehcacheConfig.init();
        return new EhcacheService(ehcacheConfig);
    }

    private long getTimeoutThresholdByCaseName(String name) {
        // Some cases need fast timeout threshold
        List<String> needFastTimeoutCases = Collections
                .singletonList("only-one-span-and-kind-server-with-parentSpanId");
        return needFastTimeoutCases.contains(name) ? 1L : TimeUnit.SECONDS.toMillis(60);
    }

}
