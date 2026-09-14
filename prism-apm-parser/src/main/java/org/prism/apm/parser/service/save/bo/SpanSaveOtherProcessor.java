package org.prism.apm.parser.service.save.bo;

import org.prism.apm.parser.mapper.ServiceCallEntityMapper;
import org.prism.apm.parser.model.span.SpanInfo;
import org.prism.apm.parser.model.entity.ServiceCallEntity;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * process spankind:
 * - client
 * - producer
 * - consumer
 * @author john
 * @date 2024-08-29 16:41
 */
@Component
public class SpanSaveOtherProcessor implements SpanSaveProcessorInterface {

    @Resource
    private SpanSaveBo spanSaveBo;

    public SpanSaveOtherProcessor(SpanSaveBo spanSaveBo) {
        this.spanSaveBo = spanSaveBo;
    }

    @Override
    public void process(SpanInfo caller, SpanInfo callee, List<ServiceCallEntity> sysCallEntites, List<String> delayedProcessingSpanIds) {


        // assemble data
        ServiceCallEntity svcCallEntity = ServiceCallEntityMapper.from(callee, caller);
        sysCallEntites.add(svcCallEntity);
    }
}
