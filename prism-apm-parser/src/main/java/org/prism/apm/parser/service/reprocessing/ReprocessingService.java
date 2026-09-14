package org.prism.apm.parser.service.reprocessing;

import org.prism.apm.parser.config.ServiceParseConfig;
import org.prism.apm.parser.repository.ServiceCallMapper;
import org.prism.apm.parser.service.reprocessing.bo.ReprocessingBo;
import org.prism.apm.parser.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReprocessingService {


    @Autowired
    private ReprocessingBo reprocessingBo;

    @Autowired
    private ServiceCallMapper serviceCallMapper;

    @Autowired
    private KafkaProperties kafkaProperties;

    @Autowired
    private ServiceParseConfig serviceParseConfig;


    public String checkConsumeFromTimeParam(Instant kafkaStartTimeParam,
                                            Instant spanStartTimeParam,
                                            Instant spanEndTimeParam) {
        if (kafkaStartTimeParam.isAfter(spanEndTimeParam)) {
            return String.format("kafkaStartTimeParam must not later than spanStartTimeParam. kafkaStartTimeParam:%s, spanStartTimeParam:%s", kafkaStartTimeParam, spanStartTimeParam);
        }

        if (spanStartTimeParam.isAfter(spanEndTimeParam)) {
            return String.format("spanStartTimeParam must not later than spanEndTimeParam. spanStartTimeParam:%s, spanEndTimeParam:%s", spanStartTimeParam, spanEndTimeParam);
        }

        return "";
    }

    public void consumeFromTime(Instant kafkaStartTimeParam,
                                Instant spanStartTimeParam,
                                Instant spanEndTimeParam) {
        long reprocessingStartTime = System.currentTimeMillis();
        // TODO: 2024/10/8 Add log output to the logic below.
        log.info("kafkaStartTime:{}, spanStartTime:{}, spanEndTime:{}", kafkaStartTimeParam, spanStartTimeParam, spanEndTimeParam);


        // using custom kafka config, with different consumer group
        Map<String, Object> modifiedProps = kafkaProperties.buildConsumerProperties();
        // it's fine not to remove group id, as long as you don't commit offset.
        modifiedProps.remove(ConsumerConfig.GROUP_ID_CONFIG);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(modifiedProps)) {
            // Get all partitions for the specified topic
            List<TopicPartition> partitions = new ArrayList<>();
            consumer.partitionsFor(serviceParseConfig.getKafkaTopic()).forEach(partitionInfo ->
                partitions.add(new TopicPartition(serviceParseConfig.getKafkaTopic(), partitionInfo.partition())));

            // Assign the consumer to these partitions
            consumer.assign(partitions);

            // Set the timestamp for each partition
            Map<TopicPartition, Long> timestampsToSearch = new HashMap<>();
            for (TopicPartition partition : partitions) {
                timestampsToSearch.put(partition, kafkaStartTimeParam.toEpochMilli());
            }

            // Get the offset for the specified time
            Map<TopicPartition, OffsetAndTimestamp> offsetsForTimes = consumer.offsetsForTimes(timestampsToSearch);
            boolean hasParitionToConsume = false;
            for (TopicPartition partition : partitions) {
                OffsetAndTimestamp offsetAndTimestamp = offsetsForTimes.get(partition);
                if (offsetAndTimestamp != null) {
                    consumer.seek(partition, offsetAndTimestamp.offset());
                    hasParitionToConsume = true;
                } else {
                    // If no offset is found for the timestamp
                    log.info("No offset found for partition: {}", partition);
                }
            }


            if (!hasParitionToConsume) {
                log.info("no partion fit time:{}", kafkaStartTimeParam);
                return;
            }

            // delete old reprocessed data
//            reprocessingServiceCallMapper.deleteAll();

            // consume spans
            long consumptionCount = reprocessingBo.consumerSpans(consumer,
                    spanStartTimeParam, spanEndTimeParam);


            log.info("Consumption completed. data count:{} , startTime:{}, startTime:{}, consumption process startTime:{},consumption process  endTime:{}",
                    consumptionCount, kafkaStartTimeParam.toString(), spanEndTimeParam.toString(),
                    CommonUtil.formatEpochMilli(reprocessingStartTime, "yyyyMMdd HH:mm:ss"),
                    CommonUtil.formatEpochMilli(System.currentTimeMillis(), "yyyyMMdd HH:mm:ss")

            );
        }
    }


    /**
     * cation: because of refresh_continuous_aggregate, this method can not use transaction.
     * @param startTime
     * @param endTime
     */
    public void transferData(OffsetDateTime startTime, OffsetDateTime endTime) {

//        // delete old data
//        serviceCallMapper.deleteDataInRange(
//                startTime, endTime);
//        // transfer data
//        reprocessingServiceCallMapper.insertDataToServiceCall(
//                startTime, endTime);
        // refresh ServiceCallMinute view with specified time
        serviceCallMapper.refreshServiceCallMinute(startTime, endTime);
    }

    public String[] parseTimeInterval(String timeInterval) {
        // Check the timeInterval length
        if (timeInterval.length() < 2) {
            throw new IllegalArgumentException("Invalid timeInterval format: '" + timeInterval + "'. It should be a number followed by 'm' (minutes) or 'd' (days).");
        }

        // Parse timeInterval
        String unit = timeInterval.substring(timeInterval.length() - 1).toLowerCase(); // Get the last character and convert it to lowercase
        int interval;

        try {
            interval = Integer.parseInt(timeInterval.substring(0, timeInterval.length() - 1)); // Extract the numeric part
        } catch (NumberFormatException e) {
            // Catch number parsing exceptions and throw detailed error information
            throw new IllegalArgumentException("Invalid interval format in timeInterval: '" + timeInterval + "'. The interval part should be a valid number.");
        }

        // Get the current time and remove milliseconds
        ZonedDateTime endTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
        // The endTime format is：2024-10-24T06:26:03Z
        String endTimeStr = endTime.format(DateTimeFormatter.ISO_INSTANT);

        ZonedDateTime startTime;
        if ("m".equals(unit)) {
            // Move back by interval minutes
            startTime = endTime.minusMinutes(interval);
        } else if ("d".equals(unit)) {
            // Move back by interval days
            startTime = endTime.minusDays(interval);
        } else {
            throw new IllegalArgumentException("Invalid time interval unit. Use 'm' for minutes or 'd' for days.");
        }

        // Output startTime and endTime without milliseconds
        String startTimeStr = startTime.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_INSTANT);

        return new String[]{startTimeStr, endTimeStr};
    }



}