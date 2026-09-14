package org.prism.apm.parser.controller;

import org.prism.apm.parser.service.reprocessing.ReprocessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.OffsetDateTime;

@Slf4j
@RestController
@RequestMapping(value = "/reprocessing")
public class ReprocessingController {


    @Autowired
    private ReprocessingService reprocessingService;

    /**
     * curl -X GET "http://localhost:8090/reprocessing/quick/6m"
     * @param timeInterval
     * @return
     */
    @GetMapping("/quick/{timeInterval}")
    public ResponseEntity<String> quickConsume(@PathVariable String timeInterval) {
        log.info("timeInterval:{}, spanStartTime:{}, spanEndTime:{}", timeInterval);
        try {
            String[] times = reprocessingService.parseTimeInterval(timeInterval);
            String startTime = times[0];
            String endTime = times[1];

            // Call the Kafka consume method
            this.consumeFromTime(startTime, startTime, endTime);

            String rtnMessage = String.format("quick reprocessing completed. startTime:%s, endTime:%s", startTime, endTime);
            log.info(rtnMessage);
            return ResponseEntity.ok(rtnMessage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }


    /**
     * consume spans from kafka, and handle spans again.
     *
     * @param kafkaStartTimeParam the time that you want to consume from kafka
     * @param spanStartTimeParam startTime
     * @param spanEndTimeParam
     */
    /**
     * Consume spans from kafka, and processes data within a specified time range.
     *
     * <p>This method filters and processes only the data records whose
     * `time` field falls within the given range, defined by the parameters
     * `startTime` and `endTime`. The time range is inclusive, meaning both
     * the start and end boundaries are considered part of the range.</p>
     *
     * @param kafkaStartTimeParam the time that you want to consume from kafka
     *
     * @param spanStartTimeParam the start of the time range (inclusive).
     *                  Only records with a `time` field greater than or equal to this value will be processed.
     * @param spanEndTimeParam   the end of the time range (exclusive).
     *                  Only records with a `time` field less than or equal to this value will be processed.
     *
     * @throws IllegalArgumentException if `startTime` is after `endTime`.
     * @throws NullPointerException if either `startTime` or `endTime` is null.
     */
    @GetMapping("/consume")
    public ResponseEntity<String> consumeFromTime(
            @RequestParam("kafkaStartTime") String kafkaStartTimeParam,
            @RequestParam("spanStartTime") String spanStartTimeParam,
            @RequestParam("spanEndTime") String spanEndTimeParam) {

        log.info("kafkaStartTime:{}, spanStartTime:{}, spanEndTime:{}", kafkaStartTimeParam, spanStartTimeParam, spanEndTimeParam);
        try {
            // Convert request time parameters to Instant objects
            Instant kafkaStartTime = Instant.parse(kafkaStartTimeParam);
            Instant spanStartTime = Instant.parse(spanStartTimeParam);
            Instant spanEndTime = Instant.parse(spanEndTimeParam);

            String checkResult = reprocessingService.checkConsumeFromTimeParam(
                    kafkaStartTime, spanStartTime, spanEndTime);
            if (!StringUtils.isEmpty(checkResult)) {
                log.info(checkResult);
                return ResponseEntity.ok(String.format(checkResult));
            }

            // Call the Kafka consume method
            reprocessingService.consumeFromTime(kafkaStartTime, spanStartTime, spanEndTime);

            String rtnMessage = String.format("reprocessing consume completed. kafkaStartTime:%s, startTime:%s, endTime:%s", kafkaStartTime, spanStartTimeParam, spanEndTimeParam);
            log.info(rtnMessage);
            return ResponseEntity.ok(rtnMessage);
        } catch (Exception e) {
            log.error("Failed to consume messages.", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }


    /**
     * transfer consumed data to ServiceCall, and refresh ServiceCallMinute.
     *
     * @param startTimeParam startTime(inclusive) of data to transfer and refresh
     * @param endTimeParam endTime(exclusive) of data to transfer and refresh
     */
    @GetMapping("/transferData")
    public ResponseEntity<String> transferData(
            @RequestParam("startTime") String startTimeParam,
            @RequestParam("endTime") String endTimeParam) {


        log.info("start to trafer data: spanStartTime:{}, spanEndTime:{}", startTimeParam, endTimeParam);
        try {
            OffsetDateTime startTime = OffsetDateTime.parse(startTimeParam);
            OffsetDateTime endTime = OffsetDateTime.parse(endTimeParam);

            reprocessingService.transferData(startTime, endTime);

            String rtnMessage = String.format("transferData completed. startTime:%s, endTime:%s", startTime, endTime);
            log.info(rtnMessage);
            return ResponseEntity.ok(rtnMessage);

        } catch (Exception e) {
            log.error("Failed to transfer data.", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }


    }


}
