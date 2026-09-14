package org.prism.apm.parser.service.save.bo;

import org.prism.apm.parser.enums.SpanKindEnum;
import org.prism.apm.parser.mapper.ServiceCallEntityMapper;
import org.prism.apm.parser.model.entity.ServiceCallEntity;
import org.prism.apm.parser.model.span.SpanInfo;
import org.prism.apm.parser.service.stats.StatsService;
import org.prism.apm.parser.utils.CommonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * @author john
 * @date 2024-08-29 16:41
 */
@Component
@Slf4j
public class SpanSaveProcessor {

    @Resource
    private SpanSaveBo spanSaveBo;

    /**
     * unit: second
     */
    @Value("${dependency.timeoutThreshold:60000}")
    private long timeoutThreshold;

    @Resource
    private SpanSaveServerProcessor serverProcessor;

    @Resource
    private SpanSaveOtherProcessor otherProcessor;

    @Resource
    private StatsService statsService;


    public SpanSaveProcessor() {
    }

    public SpanSaveProcessor(SpanSaveBo spanSaveBo, SpanSaveServerProcessor serverProcessor, SpanSaveOtherProcessor otherProcessor) {
        this.spanSaveBo = spanSaveBo;
        this.serverProcessor = serverProcessor;
        this.otherProcessor = otherProcessor;
    }

    public SpanSaveProcessor(SpanSaveBo spanSaveBo, SpanSaveServerProcessor serverProcessor, SpanSaveOtherProcessor otherProcessor, long timeoutThreshold) {
        this.spanSaveBo = spanSaveBo;
        this.serverProcessor = serverProcessor;
        this.otherProcessor = otherProcessor;
        this.timeoutThreshold = timeoutThreshold;
    }

    public SpanSaveProcessor(SpanSaveBo spanSaveBo, SpanSaveServerProcessor serverProcessor, SpanSaveOtherProcessor otherProcessor, long timeoutThreshold, StatsService statsService) {
        this.spanSaveBo = spanSaveBo;
        this.serverProcessor = serverProcessor;
        this.otherProcessor = otherProcessor;
        this.timeoutThreshold = timeoutThreshold;
        this.statsService = statsService;
    }

    public void process(SpanInfo caller, SpanInfo callee,
                        List<ServiceCallEntity> sysCallEntites,
                        List<String> delayedProcessingSpanIds) {

        SpanSaveProcessorInterface processor;
        if (Objects.equals(callee.getSpanKind(), SpanKindEnum.SERVER.getValue())) {
            processor = serverProcessor;
        } else {
            processor = otherProcessor;
        }


        // TODO: 2024/8/28 If "current time" is compared with the span generation time, these spans will all be discarded and not processed when replaying MQ for span compensation.
        // TODO: 2024/8/28 The change is complete and needs verification.
        long currTime = CommonUtil.getCurrentTimeMillis();
        if (spanSaveBo.isTimeout(callee, currTime, timeoutThreshold)) {
            log.debug("spanInfo span has timed out. currTime:{} span.time:{} span.serverArrivalTime:{} span:{}",
                    currTime, callee.getTime(), callee.getServerArrivalTime(),
                    callee);
            ServiceCallEntity svcCallEntity = ServiceCallEntityMapper.from(callee);
            sysCallEntites.add(svcCallEntity);

            // add to stats
            statsService.countTimeout();
            return;
        }

        // if caller span does not exist yet, maybe callers span has not arrived server. delay processing this span.
//        if (!spanSaveBo.isNoCallerSpan(callee)) {
//            if (caller == null) {
//                delayedProcessingSpanIds.add(callee.getSpanId());
//                return;
//            } else {
//                // the span which parentSpanId does exist is the first span of whole trace. directly save it.
//                ServiceCallEntity svcCallEntity = ServiceCallEntityMapper.from(callee);
//                sysCallEntites.add(svcCallEntity);
//                return;
//
//            }
//        }



        // the span which parentSpanId does exist is the first span of whole trace. directly save it.
        if (spanSaveBo.isNoCallerSpan(callee)) {
            ServiceCallEntity svcCallEntity = ServiceCallEntityMapper.from(callee);
            sysCallEntites.add(svcCallEntity);
            return;
        } else {
            // if caller span does not exist yet, maybe callers span has not arrived server. delay processing this span.
            if (caller == null) {
                delayedProcessingSpanIds.add(callee.getSpanId());
                return;
            }
        }

        processor.process(caller, callee, sysCallEntites, delayedProcessingSpanIds);

    }

}
