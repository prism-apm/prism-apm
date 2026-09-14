package org.prism.apm.parser.service.reprocessing.bo;

import org.prism.apm.parser.config.ServiceParseConfig;
import org.prism.apm.parser.constants.Constants;
import org.prism.apm.parser.exception.JsonException;
import org.prism.apm.parser.model.span.SpanInfo;
import org.prism.apm.parser.service.common.JsonService;
import org.prism.apm.parser.service.init.SpanInitProcessService;
import org.prism.apm.parser.service.parser.ISpanParser;
import org.prism.apm.parser.utils.CommonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author john
 * @date 2024-10-14 15:32
 */
@Slf4j
@Data
@Service
public class ReprocessingBo {


    @Autowired
    private SpanInitProcessService spanInitProcessService;

    @Autowired
    private ISpanParser parser;


    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private JsonService jsonService;

    @Autowired
    private ServiceParseConfig serviceParseConfig;

    @Getter
    @AllArgsConstructor
    private static class TraceToSend {
        private final String traceId;
        private final String trace;
    }


    public long consumerSpans(KafkaConsumer<String, String> consumer,
                              Instant spanStartTimeParam,
                              Instant spanEndTimeParam) {
        // Start consuming messages
        int consumptionCount = 0;
        while (true) {
            // Check whether all message times are greater than endTime
            List<TraceToSend> tracesToSend = new ArrayList<>();

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            int spansAfterSpanEndtimeCount = 0;
            for (ConsumerRecord<String, String> record : records) {

                String trace = record.value();
                log.debug("reprocessing trace:" + trace);

                try {
                    // Extract the startTime field value
                    List<SpanInfo> spanInfos = parser.parse(trace);
                    if (spanInfos == null || spanInfos.isEmpty()) {
                        log.debug("no span is created. trace:{}", trace);
                        continue;
                    }

                    // just use the first span(the earliest span in current trace) to compare.
                    // even though the other spans may be older than spanEndTimeParam
                    SpanInfo spanInfo = spanInfos.get(0);
                    Instant recordTime = CommonUtil.fromEpochMicroToInstant(spanInfo.getTime());

                    // drop it if span starttime earlier than specified spanStartTimeParam
                    if (recordTime.isBefore(spanStartTimeParam)) {
                        continue;
                    }

                    if (recordTime.equals(spanEndTimeParam) || recordTime.isAfter(spanEndTimeParam)) {
                        spansAfterSpanEndtimeCount++;
                        continue;
                    }

                    consumptionCount++;
                    // Get traceId as the partition key
                    String traceId = spanInfo.getTraceId();
                    tracesToSend.add(new TraceToSend(traceId, trace));

                } catch (Exception e) {
                    log.error("Failed to process record: {}", trace, e);
                }

            }

            // handle span again.
            if (!tracesToSend.isEmpty()) {
                for (TraceToSend traceToSend : tracesToSend) {
                    // Specify the partition key (traceId) when sending
                    kafkaTemplate.send(serviceParseConfig.getReprocessingKafkaTopic(), traceToSend.getTraceId(), traceToSend.getTrace());
                }
            }


            // If all message times are greater than endTime, exit the loop
            if (spansAfterSpanEndtimeCount == records.count()) {
                log.info("All messages are greater than end time. Exiting consumption.");
                break;
            }

        }

        return consumptionCount;

    }
}
