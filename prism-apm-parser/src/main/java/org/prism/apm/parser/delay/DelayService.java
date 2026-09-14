package org.prism.apm.parser.delay;

/**
 * @author john
 * @date 2024-10-23 10:53
 */
public interface DelayService {


    void processSpan(String spanId, String processType);

}
