package org.prism.apm.parser.service.reprocessing.bo;

import org.prism.apm.parser.config.JacksonConfig;
import org.prism.apm.parser.config.ServiceParseConfig;
import org.prism.apm.parser.constants.Constants;
import org.prism.apm.parser.service.common.JacksonJsonService;
import org.prism.apm.parser.service.common.JsonService;
import org.prism.apm.parser.service.init.SpanInitProcessService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author john
 * @date 2024-10-14 15:54
 */
@ExtendWith(MockitoExtension.class)
public class ReprocessingBoTest {

    @Mock
    private KafkaConsumer<String, String> mockConsumer;

    @Mock
    private SpanInitProcessService spanInitProcessService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate; // Added this mock

    @InjectMocks
    private ReprocessingBo reprocessingBo;

    @Mock
    private ServiceParseConfig serviceParseConfig;

    private JsonService jsonService;

    private Instant spanStartTimeParam;
    private Instant spanEndTimeParam;

    @BeforeEach
    public void setUp() {
        spanStartTimeParam = Instant.parse("2024-09-20T07:16:00.000Z");
        spanEndTimeParam = Instant.parse("2024-09-20T07:17:00.000Z");

        jsonService = new JacksonJsonService(new JacksonConfig().objectMapper());

        reprocessingBo.setJsonService(jsonService);

    }

    private ConsumerRecords<String, String> createConsumerRecords(List<ConsumerRecord<String, String>> records) {
        Map<TopicPartition, List<ConsumerRecord<String, String>>> recordMap = new HashMap<>();
        recordMap.put(new TopicPartition("test-topic", 0), records);
        return new ConsumerRecords<>(recordMap);
    }

    private String createRecordValue(String traceId, String spanId, String operationName, String startTime) {
        Map<String, String> jsonMap = new HashMap<>();
        jsonMap.put("traceId", traceId);
        jsonMap.put("spanId", spanId);
        jsonMap.put("operationName", operationName);
        jsonMap.put("startTime", startTime);
        return jsonService.toJson(jsonMap);
    }

    // todo: modify test
//    @Test
    public void testConsumerSpans() {

        // init serviceParseConfig
        when(serviceParseConfig.getReprocessingKafkaTopic())
                .thenReturn(ServiceParseConfig.DEFAULT_REPROCESSING_TOPIC);


        // Two records from the first poll, startTime < spanStartTimeParam.
        List<ConsumerRecord<String, String>> firstPollRecords = Arrays.asList(

                // invalid data: startTime < spanStartTimeParam
                new ConsumerRecord<>("test-topic", 0, 0, "key1", createRecordValue("traceId_11", "spanId_11", "operationName_11", "2024-09-20T07:15:00.000Z")),
                // invalid data: startTime < spanStartTimeParam
                new ConsumerRecord<>("test-topic", 0, 1, "key2", createRecordValue("traceId_12", "spanId_12", "operationName_12", "2024-09-20T07:15:30.000Z"))
        );

        // Four records from the second poll, covering multiple time conditions.
        List<ConsumerRecord<String, String>> secondPollRecords = Arrays.asList(
                // valid data: startTime == spanStartTimeParam
                new ConsumerRecord<>("test-topic", 0, 2, "key3", createRecordValue("traceId_21", "spanId_21", "operationName_21", "2024-09-20T07:16:00.000Z")),
                // valid data: spanStartTimeParam < startTime < spanEndTimeParam
                new ConsumerRecord<>("test-topic", 0, 3, "key4", createRecordValue("traceId_22", "spanId_22", "operationName_22", "2024-09-20T07:16:30.000Z")),
                // invalid data: startTime == spanEndTimeParam
                new ConsumerRecord<>("test-topic", 0, 4, "key5", createRecordValue("traceId_23", "spanId_23", "operationName_23", "2024-09-20T07:17:00.000Z")),
                // invalid data: startTime > spanEndTimeParam
                new ConsumerRecord<>("test-topic", 0, 5, "key6", createRecordValue("traceId_24", "spanId_24", "operationName_24", "2024-09-20T07:17:30.000Z"))
        );

        // Two records from the third poll, startTime > spanEndTimeParam.
        List<ConsumerRecord<String, String>> thirdPollRecords = Arrays.asList(
                // invalid data: startTime > spanEndTimeParam
                new ConsumerRecord<>("test-topic", 0, 6, "key7", createRecordValue("traceId_31", "spanId_31", "operationName_31", "2024-09-20T07:18:00.000Z")),

                // invalid data: startTime > spanEndTimeParam
                new ConsumerRecord<>("test-topic", 0, 7, "key8", createRecordValue("traceId_32", "spanId_32", "operationName_32", "2024-09-20T07:18:30.000Z"))
        );

        // Configure mock behavior.
        when(mockConsumer.poll(Duration.ofMillis(1000)))
                .thenReturn(createConsumerRecords(firstPollRecords))  // First poll.
                .thenReturn(createConsumerRecords(secondPollRecords)) // Second poll.
                .thenReturn(createConsumerRecords(thirdPollRecords)); // Third poll.

        // Execute the test method
        long result = reprocessingBo.consumerSpans(mockConsumer, spanStartTimeParam, spanEndTimeParam);

        // Verify kafkaTemplate.send was called the expected number of times.
        verify(kafkaTemplate, times(2)).send(eq(ServiceParseConfig.DEFAULT_REPROCESSING_TOPIC), anyString());

        // Assert that the consumed count is correct.
        assertEquals(2, result);  // Only two records from the second poll match the conditions.
    }



//    @Test
    public void testConsumerSpansWithEmptyPoll() {
        // Mock poll() to return empty ConsumerRecords.
        when(mockConsumer.poll(Duration.ofMillis(1000)))
                .thenReturn(ConsumerRecords.empty());

        // Execute the test method
        long result = reprocessingBo.consumerSpans(mockConsumer, spanStartTimeParam, spanEndTimeParam);

        // Verify handleSpan was never called.
        verify(spanInitProcessService, never()).handleSpan(anyList(), anyString());

        // Assert that the consumed count is 0.
        assertEquals(0, result);
    }

}
