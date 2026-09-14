package org.prism.apm.parser.service.save.bo;

import org.prism.apm.parser.mapper.ServiceCallEntityMapper;
import org.prism.apm.parser.model.span.SpanInfo;
import org.prism.apm.parser.model.entity.ServiceCallEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * process spankind:
 * - server
 * @author john
 * @date 2024-08-29 16:41
 */
@Component
@Slf4j
public class SpanSaveServerProcessor implements SpanSaveProcessorInterface {

    @Resource
    private SpanSaveBo spanSaveBo;

    public SpanSaveServerProcessor(SpanSaveBo spanSaveBo) {
        this.spanSaveBo = spanSaveBo;
    }

    @Override
    public void process(SpanInfo caller, SpanInfo callee, List<ServiceCallEntity> sysCallEntites, List<String> delayedProcessingSpanIds) {

        // first server span
        // the span that spankind is server and parentSpanId is empty is the first span of whole trace. directly save it.
//        if (spanSaveBo.isNoParentSpan(callee)) {
//            // assemble data
//            ServiceCallEntity svcCallEntity = ServiceCallEntityMapper.from(callee);
//            sysCallEntites.add(svcCallEntity);
//            return;
//        }


        // process server span which have caller span
        ServiceCallEntity svcCallEntity = ServiceCallEntityMapper.from(callee, caller);
        sysCallEntites.add(svcCallEntity);
    }
}
