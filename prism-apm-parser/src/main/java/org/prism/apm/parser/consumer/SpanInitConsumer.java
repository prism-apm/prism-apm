package org.prism.apm.parser.consumer;

import org.prism.apm.parser.config.ServiceParseConfig;
import org.prism.apm.parser.constants.Constants;
import org.prism.apm.parser.service.init.SpanInitProcessService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


/**
 * @author peter
 * @create 2019-07-01
 */

@Component
@Slf4j
public class SpanInitConsumer {

    @Autowired
    private SpanInitProcessService spanInitProcessService;

//    @KafkaListener(topics = {ServiceParseConfig.TRACES_TOPIC}, containerFactory = "batchFactory")
    @KafkaListener(topics = {ServiceParseConfig.TRACES_TOPIC})
    public void consumerBatch(List<ConsumerRecord<?, String>> list, Acknowledgment ack) {
        log.trace("Preparing to init data count: {}", list.size());
        List<String> paramList = new ArrayList<>();
        for (ConsumerRecord<?, String> record : list) {
            paramList.add(record.value());
        }

        spanInitProcessService.handleSpan(
                paramList, Constants.SPAN_PROCESS_TYPE_FIRST);
        ack.acknowledge();
        log.trace("initialized record count: {}", list.size());

    }


//    @KafkaListener(topics = {ServiceParseConfig.REPROCESSING_TOPIC}, containerFactory = "batchFactory")
    @KafkaListener(topics = {ServiceParseConfig.REPROCESSING_TOPIC})
    public void reprocessingBatch(List<ConsumerRecord<?, String>> list, Acknowledgment ack) {
        log.trace("Preparing to init data count: {}", list.size());
        List<String> paramList = new ArrayList<>();
        for (ConsumerRecord<?, String> record : list) {
            paramList.add(record.value());
        }

        spanInitProcessService.handleSpan(
                paramList, Constants.SPAN_PROCESS_TYPE_AGAIN);
        ack.acknowledge();
        log.trace("initialized record count: {}", list.size());

    }


}
