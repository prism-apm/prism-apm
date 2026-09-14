package org.prism.apm.parser.service.init;

import org.prism.apm.parser.cache.CacheService;
import org.prism.apm.parser.config.ServiceParseConfig;
import org.prism.apm.parser.delay.DelayService;
import org.prism.apm.parser.model.span.SpanInfo;
import org.prism.apm.parser.service.common.JsonService;
import org.prism.apm.parser.service.parser.ISpanParser;
import org.prism.apm.parser.utils.CommonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpanInitProcessService {

    @Value("${service.parse.check:false}")
    private boolean serviceParseCheck;

    @Autowired
    private ISpanParser parser; // <-- Change: inject the new interface

    @Autowired
    private CacheService cacheService;

    @Autowired
    private DelayService delayService;

    @Autowired
    private ServiceParseConfig serviceParseConfig;

    @Autowired
    private JsonService jsonService;

    public void handleSpan(List<String> list, String type) {
        log.trace("handleSpan size:" + list.size());

        List<SpanInfo> spanInfoList = new ArrayList<>();

        for (String spanStr : list) {
            try {
                log.debug("processing span:" + spanStr);

                // --- Change: call the new parser ---
                List<SpanInfo> parsedSpans = parser.parse(spanStr);
                if (parsedSpans == null || parsedSpans.isEmpty()) {
                    continue;
                }

                // --- Change: add inner loop ---
                for (SpanInfo spanInfo : parsedSpans) {
                    if (serviceParseCheck && !serviceParseConfig.needParsed(spanInfo.getServiceName())) {
                        continue;
                    }
                    spanInfoList.add(spanInfo);
                }

            } catch (Exception e) {
                log.error("span parse error :[" + spanStr + "]", e);
            }
        }

        for (SpanInfo spanInfo : spanInfoList) {
                String spanCacheStr = jsonService.toJson(spanInfo);
                cacheService.set(spanInfo.getSpanId(), spanCacheStr);
                delayService.processSpan(spanInfo.getSpanId(), type);
        }

    }
}