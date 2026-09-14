package org.prism.apm.parser.service.init.bo;

import org.prism.apm.parser.config.FieldMatchRuleConfig;
import org.prism.apm.parser.model.replacer.MatchRuleBase;
import org.prism.apm.parser.model.replacer.MatchRuleInterface;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * @author john
 * @date 2024-10-29 15:59
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@Service
public class SpanFieldReplacer {

    @Resource
    private FieldMatchRuleConfig config;

    public String match(String serviceName, String component,
                        String fieldName, String fieldContent) {

        if (config == null) {
            return null;
        }

        MatchRuleInterface rule = config.getRule(serviceName, component, fieldName);

        if (rule == null) {
            return null;  // No matching rule found
        }


        String rtn = rule.replace(fieldContent);
        return rtn;

    }
}
