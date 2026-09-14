package org.prism.apm.parser.model.replacer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchRuleBase {
    protected String serviceName;
    protected String component;
    protected Short spankind;
    protected String fieldName;

}