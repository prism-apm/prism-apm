package org.prism.apm.parser.model.replacer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces field content using regex.
 * Multiple regex replacement rules can be configured for one field.
 *
 * @author john
 * @date 2025-01-14 15:36
 */
@Slf4j
@Data
@NoArgsConstructor
public class RegexMatchRule extends MatchRuleBase implements MatchRuleInterface {

    private List<PatternReplacement> patternReplacements = new ArrayList<>();

    public RegexMatchRule(String serviceName, String component, Short spankind,
                          String fieldName, String searchContent, String replacement) {
        this.serviceName = serviceName;
        this.component = component;
        this.spankind = spankind;
        this.fieldName = fieldName;

        patternReplacements.add(new PatternReplacement(searchContent, replacement));
    }

    @Override
    public String replace(String content) {

        for (PatternReplacement patternReplacement : patternReplacements) {
            Matcher matcher = patternReplacement.getPattern().matcher(content);
            if (matcher.find()) {
                content = matcher.replaceAll(Matcher.quoteReplacement(patternReplacement.getReplacement()));
                log.debug("Regex match. content:{}, regex:{}", content, patternReplacement.getSearchContent());
            }
        }

        return content;
    }

    public void addPatternReplacement(String searchContent, String replacement) {
        patternReplacements.add(new PatternReplacement(searchContent, replacement));
    }

    @Data
    @AllArgsConstructor
    private class PatternReplacement {
        private String searchContent;
        private String replacement;
        private Pattern pattern;

        public PatternReplacement(String searchContent, String replacement) {
            this.searchContent = searchContent;
            this.replacement = replacement;
            this.pattern = Pattern.compile(searchContent);
        }
    }

}
