package org.prism.apm.parser.config;

import org.prism.apm.common.model.entity.FieldContentReplacerRuleEntity;
import org.prism.apm.common.repository.FieldContentReplacerRuleMapper;
import org.prism.apm.parser.constants.SpanInfoFields;
import org.prism.apm.parser.model.replacer.DirectMatchRule;
import org.prism.apm.parser.model.replacer.MatchRuleBase;
import org.prism.apm.parser.model.replacer.MatchRuleInterface;
import org.prism.apm.parser.model.replacer.RegexMatchRule;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.*;

/**
 * @author john
 * @date 2024-10-30 14:39
 */
@Slf4j
@Data
@NoArgsConstructor
@Configuration
public class FieldMatchRuleConfig {

    public static final String ANY_VALUE = "__ANY__";



    public static final String FIELD_NAME_ACTION_NAME = SpanInfoFields.ACTION_NAME;

    public static final String FIELD_NAME_SERVICE_NAME = SpanInfoFields.SERVICE_NAME;

    public static final String FIELD_NAME_COMPONENT = SpanInfoFields.COMPONENT;

    public static final String FIELD_NAME_SPAN_KIND = SpanInfoFields.SPAN_KIND;
    public static final String MATCH_TYPE_REGEX = "regex";
    public static final String MATCH_TYPE_DIRECT_RETURN = "direct_return";

    private volatile Set<String> anyValueInField = new HashSet<>();

    private volatile Map<String, MatchRuleInterface> ruleMap = new HashMap<>();

    @Resource
    private FieldContentReplacerRuleMapper mapper;

    public FieldMatchRuleConfig(Set<String> anyValueInField, Map<String, MatchRuleInterface> ruleMap) {
        this.anyValueInField = anyValueInField;
        this.ruleMap = ruleMap;
    }

    @PostConstruct
    public void init() {
        refresh();
    }


    public void refresh() {
        Set<String> tempAnyValueInField = new HashSet<>();
        Map<String, MatchRuleInterface> tempRuleMap = new HashMap<>();

        FieldContentReplacerRuleEntity condition = FieldContentReplacerRuleEntity.builder()
                .enabled(true)
                .build();
        List<FieldContentReplacerRuleEntity> results = mapper.selectByAll(condition);
        if (results == null || results.isEmpty()) {
            return;
        }


        boolean hasInitError = false;
        for (FieldContentReplacerRuleEntity result : results) {
            if (ANY_VALUE.equals(result.getServiceName())) {
                tempAnyValueInField.add(FIELD_NAME_SERVICE_NAME);
            }

            if (ANY_VALUE.equals(result.getComponent())) {
                tempAnyValueInField.add(FIELD_NAME_COMPONENT);
            }


            String key = generateKey(result.getServiceName(), result.getComponent(),
                    result.getFieldName());

            // Validate rule conflicts:
            // - Do not allow multiple DirectMatchRule entries with the same key.
            // - Do not allow DirectMatchRule and RegexMatchRule to coexist for the same key.
            if (tempRuleMap.containsKey(key)) {
                // todo: what does checkResult1 want to check
                MatchRuleInterface matchRule = tempRuleMap.get(key);
                boolean checkResult1 = matchRule instanceof DirectMatchRule;
                boolean checkResult2 = matchRule instanceof RegexMatchRule &&
                        result.getMatchType().equals(MATCH_TYPE_DIRECT_RETURN);
                if (checkResult1 || checkResult2) {
                    log.error("can not exist multiple {} rule, or {} and {} exist at the same time. Key:{}. Existed rule matchType:{}, New Rule matchType:{}",
                            MATCH_TYPE_DIRECT_RETURN, MATCH_TYPE_DIRECT_RETURN, MATCH_TYPE_REGEX,
                            key, MATCH_TYPE_DIRECT_RETURN, result.getMatchType()
                            );
                    hasInitError = true;
                    break;
                }
            }


            MatchRuleInterface rule = null;
            if (MATCH_TYPE_DIRECT_RETURN.equals(result.getMatchType())) {
                rule = new DirectMatchRule(
                        result.getServiceName(),
                        result.getComponent(),
                        result.getSpanKind(),
                        result.getFieldName(),
                        result.getReplacement());

                tempRuleMap.put(key, rule);
            } else if (MATCH_TYPE_REGEX.equals(result.getMatchType())) {
                if (!tempRuleMap.containsKey(key)) {
                    rule = new RegexMatchRule(
                            result.getServiceName(),
                            result.getComponent(),
                            result.getSpanKind(),
                            result.getFieldName(),
                            result.getSearchContent(),
                            result.getReplacement());

                    tempRuleMap.put(key, rule);
                } else {
                    RegexMatchRule matchRule = (RegexMatchRule)tempRuleMap.get(key);
                    matchRule.addPatternReplacement(result.getSearchContent(),
                            result.getReplacement());
                }
            } else {
                log.error("unexpected match type:{}", result.getMatchType());
            }

        }

        if (hasInitError) {
            return;
        }

        anyValueInField = tempAnyValueInField;
        ruleMap = tempRuleMap;
    }

    public MatchRuleInterface getRule(String serviceName, String component, String fieldName) {

        // find the exact-match rule
        MatchRuleInterface rule = ruleMap.get(generateKey(serviceName, component, fieldName));
        if (rule != null) {
            return rule;
        }

        // find the wildcard-match rule
        if (anyValueInField.contains(FIELD_NAME_SERVICE_NAME)) {
            rule = ruleMap.get(generateKey(ANY_VALUE, component, fieldName));
        } else if (anyValueInField.contains(FIELD_NAME_COMPONENT)) {
            rule = ruleMap.get(generateKey(serviceName, ANY_VALUE, fieldName));
        }

        return rule;

    }

    private String generateKey(String serviceName, String component, String fieldName) {
        return serviceName + "::" + component + "::" + fieldName;
    }


}
