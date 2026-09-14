package org.prism.apm.parser.service.save.bo;

import org.prism.apm.parser.model.span.SpanInfo;
import org.prism.apm.parser.model.entity.ServiceCallEntity;

import java.util.List;

/**
 * @author john
 * @date 2024-08-29 16:40
 */
public interface SpanSaveProcessorInterface {

    void process(SpanInfo caller, SpanInfo callee,
                 List<ServiceCallEntity> sysCallEntites,
                 List<String> delayedProcessingSpanIds);
}
