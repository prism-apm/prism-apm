package org.prism.apm.parser.model.replacer;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author john
 * @date 2025-01-14 15:37
 */
@Data
@NoArgsConstructor
public class DirectMatchRule extends MatchRuleBase implements MatchRuleInterface {


    private String replacement;

    public DirectMatchRule(String serviceName, String component, Short spankind,
                          String fieldName, String replacement) {
        this.serviceName = serviceName;
        this.component = component;
        this.spankind = spankind;
        this.fieldName = fieldName;
        this.replacement = replacement;
    }


    @Override
    public String replace(String value) {

        return replacement;
    }
}
