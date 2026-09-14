package org.prism.apm.parser.consumer;

import org.prism.apm.parser.config.ServiceParseConfig;
import org.prism.apm.parser.constants.Constants;
import org.prism.apm.parser.service.init.SpanInitProcessService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SpanInitConsumerTest {

    @Mock
    private SpanInitProcessService spanInitProcessService;

    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private SpanInitConsumer spanInitConsumer;

    /**
     * Test that consumerBatch method can successfully process first-time consumption records and correctly invoke the downstream service and ack.
     */
    @Test
    public void consumerBatch_shouldProcessRecordsAndAcknowledgeSuccessfully() {
        // Arrange
        List<ConsumerRecord<?, String>> records = Arrays.asList(
                new ConsumerRecord<>("jaeger-spans", 0, 0, null, "value1"),
                new ConsumerRecord<>("jaeger-spans", 0, 1, null, "value2")
        );
        List<String> expectedValues = Arrays.asList("value1", "value2");

        // Act
        spanInitConsumer.consumerBatch(records, ack);

        // Assert
        ArgumentCaptor<List<String>> listCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);

        verify(spanInitProcessService, times(1)).handleSpan(listCaptor.capture(), typeCaptor.capture());
        assertEquals(expectedValues, listCaptor.getValue(), "The captured list of values should match the expected list.");
        assertEquals(Constants.SPAN_PROCESS_TYPE_FIRST, typeCaptor.getValue(), "The process type should be for first-time processing.");

        verify(ack, times(1)).acknowledge();
    }

    /**
     * Test that reprocessingBatch method can successfully process reprocessing records and correctly invoke the downstream service and ack.
     */
    @Test
    public void reprocessingBatch_shouldProcessRecordsAndAcknowledgeSuccessfully() {
        // Arrange
        List<ConsumerRecord<?, String>> records = Arrays.asList(
                new ConsumerRecord<>(ServiceParseConfig.DEFAULT_REPROCESSING_TOPIC, 0, 0, null, "value3"),
                new ConsumerRecord<>(ServiceParseConfig.DEFAULT_REPROCESSING_TOPIC, 0, 1, null, "value4")
        );
        List<String> expectedValues = Arrays.asList("value3", "value4");

        // Act
        spanInitConsumer.reprocessingBatch(records, ack);

        // Assert
        ArgumentCaptor<List<String>> listCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);

        verify(spanInitProcessService, times(1)).handleSpan(listCaptor.capture(), typeCaptor.capture());
        assertEquals(expectedValues, listCaptor.getValue(), "The captured list of values should match the expected list.");
        assertEquals(Constants.SPAN_PROCESS_TYPE_AGAIN, typeCaptor.getValue(), "The process type should be for reprocessing.");

        verify(ack, times(1)).acknowledge();
    }

    /**
     * Test that when the received record list is empty, the downstream service is still called (with an empty list) and ack is performed.
     */
    @Test
    public void consumerBatch_shouldHandleEmptyRecordListGracefully() {
        // Arrange
        List<ConsumerRecord<?, String>> records = Collections.emptyList();

        // Act
        spanInitConsumer.consumerBatch(records, ack);

        // Assert
        ArgumentCaptor<List<String>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(spanInitProcessService, times(1)).handleSpan(listCaptor.capture(), anyString());
        assertEquals(0, listCaptor.getValue().size(), "The captured list of values should be empty.");

        verify(ack, times(1)).acknowledge();
    }

    /**
     * Test that when the handleSpan service throws an exception, ack is not performed to ensure the message can be re-consumed.
     */
    @Test
    public void consumerBatch_shouldNotAcknowledgeWhenServiceThrowsException() {
        // Arrange
        List<ConsumerRecord<?, String>> records = Arrays.asList(
                new ConsumerRecord<>("jaeger-spans", 0, 0, null, "value1")
        );
        doThrow(new RuntimeException("Simulated DB error")).when(spanInitProcessService).handleSpan(anyList(), anyString());

        // Act
        try {
            spanInitConsumer.consumerBatch(records, ack);
        } catch (Exception e) {
            // Expected exception
        }

        // Assert
        verify(spanInitProcessService, times(1)).handleSpan(anyList(), anyString());
        verify(ack, never()).acknowledge();
    }
}
