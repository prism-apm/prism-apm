package org.prism.apm.parser.service.init.bo;

import org.prism.apm.parser.MybatisTestBase;
import org.prism.apm.common.model.entity.FieldContentReplacerRuleEntity;
import org.prism.apm.common.repository.FieldContentReplacerRuleMapper;
import org.prism.apm.parser.config.FieldMatchRuleConfig;
import org.prism.apm.parser.constants.SpanInfoFields;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class SpanFieldReplacerTest extends MybatisTestBase {

    @Autowired
    private FieldContentReplacerRuleMapper mapper;

    @Mock
    private FieldMatchRuleConfig fieldMatchRuleConfig;

    private SpanFieldReplacer replacer;

    @BeforeEach
    public void setUp() {
        replacer = new SpanFieldReplacer(fieldMatchRuleConfig);
    }

    @Test
    public void testReplaceWithRegexMatch_withRealDb() {
        fieldMatchRuleConfig = new FieldMatchRuleConfig();
        fieldMatchRuleConfig.setMapper(mapper);

        FieldContentReplacerRuleEntity rule = new FieldContentReplacerRuleEntity();
        rule.setServiceName("__ANY__");
        rule.setComponent("io.opentelemetry.http-url-connection");
        rule.setSpanKind((short) 3);
        rule.setFieldName("targetName");
        rule.setMatchType(FieldMatchRuleConfig.MATCH_TYPE_REGEX);
        rule.setSearchContent("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(:\\d+)?");
        rule.setReplacement("${ip}:${port}");
        rule.setEnabled(true);
        mapper.insert(rule);

        FieldContentReplacerRuleEntity rule2 = new FieldContentReplacerRuleEntity();
        rule2.setServiceName("__ANY__");
        rule2.setComponent("io.opentelemetry.http-url-connection");
        rule2.setSpanKind((short) 3);
        rule2.setFieldName("targetName");
        rule2.setMatchType(FieldMatchRuleConfig.MATCH_TYPE_REGEX);
        rule2.setSearchContent("=([^&]+)");
        rule2.setReplacement("=${value}");
        rule2.setEnabled(true);
        mapper.insert(rule2);

        fieldMatchRuleConfig.refresh();

        SpanFieldReplacer replacer = new SpanFieldReplacer();
        replacer.setConfig(fieldMatchRuleConfig);


        String serviceName = "mdm-service";
        String component = "io.opentelemetry.http-url-connection";
        String fieldName = SpanInfoFields.TARGET_NAME;
        String fieldContent = "http://10.225.162.143:8080/user/authorization/querylist/subApp?appKey=teams&domainUserName=huangdi&subAppKey=teamsApp";


        String rtn = replacer.match(serviceName, component, fieldName, fieldContent);
        System.out.println(rtn);

    }

    @Test
    public void testReplaceWithRegexMatch_withRealDb2() {
        fieldMatchRuleConfig = new FieldMatchRuleConfig();
        fieldMatchRuleConfig.setMapper(mapper);

        FieldContentReplacerRuleEntity rule = new FieldContentReplacerRuleEntity();
        rule.setServiceName("__ANY__");
        rule.setComponent("io.opentelemetry.tomcat-7.0");
        rule.setSpanKind((short) 0);
        rule.setFieldName(SpanInfoFields.REMOTE_ADDR);
        rule.setMatchType(FieldMatchRuleConfig.MATCH_TYPE_REGEX);
        rule.setSearchContent("\\b(?!10\\.|11\\.|127\\.|169\\.254\\.|192\\.168\\.|172\\.(?:1[6-9]|2[0-9]|3[0-1])\\.)(?:(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.){3}(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\b");
        rule.setReplacement("${public_ip}");
        rule.setEnabled(true);
        mapper.insert(rule);

        fieldMatchRuleConfig.refresh();

        SpanFieldReplacer replacer = new SpanFieldReplacer();
        replacer.setConfig(fieldMatchRuleConfig);

        String serviceName = "mdm-service";
        String component = "io.opentelemetry.tomcat-7.0";
        String fieldName = SpanInfoFields.REMOTE_ADDR;

        // ==========================================
        // 1. standard private IP (should not match the regex)
        // ==========================================
        List<String> privateIps = Arrays.asList(
                "10.0.0.1",
                "10.255.255.255",
                "172.16.0.1",        // Class B lower bound
                "172.20.10.10",
                "172.31.255.255",    // Class B upper bound
                "192.168.0.1",
                "192.168.100.50",
                "127.0.0.1",         // Localhost
                "11.53.80.89",       // specified reserved segment
                "11.1.2.3"
        );

        // ==========================================
        // 2. easily misclassified public IP (should match the regex) - key test area
        // ==========================================
        List<String> trapPublicIps = Arrays.asList(
                "36.60.127.164",     // previously failing case: contains .127.
                "1.10.2.3",          // contains .10.
                "5.192.168.1",       // contains .192.168.
                "200.11.1.1",        // contains .11.
                "8.8.8.8",           // regular public IP
                "1.1.1.1",           // regular public IP
                "223.5.5.5"
        );

        // ==========================================
        // 3. boundary public IP (looks private but should match)
        // ==========================================
        List<String> boundaryPublicIps = Arrays.asList(
                "172.15.255.255",    // less than 172.16
                "172.32.0.1",        // greater than 172.31
                "192.169.1.1",       // not 192.168
                "192.167.1.1"
        );

        // ==========================================
        // Execute the test
        // ==========================================
        //System.out.println("------ Test private IPs (expected: no regex match) ------");
        for (String ip : privateIps) {
            String result = replacer.match(serviceName, component, fieldName, ip);
            assertEquals(ip, result);
        }

//        System.out.println("\n------ Test trap public IPs (expected: regex match) ------");
        for (String ip : trapPublicIps) {
            String result = replacer.match(serviceName, component, fieldName, ip);
            assertEquals("${public_ip}", result);
        }

//        System.out.println("\n------ Test boundary public IPs (expected: regex match) ------");
        for (String ip : boundaryPublicIps) {
            String result = replacer.match(serviceName, component, fieldName, ip);
            assertEquals("${public_ip}", result);
        }

    }


//    @Test
//    public void testReplaceWithDirectMatch() {
//        // Prepare test data
//        String serviceName = "test-service";
//        String component = "java-jdbc";
//        String fieldName = "db.statement";
//        String originalValue = "SELECT * FROM users WHERE id = 123";
//        String expectedValue = "SELECT * FROM users WHERE id = ?";
//
//        // Mock exact-match rule.
//        DirectMatchRule directRule = new DirectMatchRule(
//                serviceName,
//                component,
//                "client",
//                fieldName,
//                expectedValue
//        );
//
//        when(fieldMatchRuleConfig.getRule(serviceName, component, fieldName))
//                .thenReturn(directRule);
//
//        // Execute the test
//        String result = replacer.replace(serviceName, component, fieldName, originalValue);
//
//        // Verify the result
//        assertEquals(expectedValue, result);
//    }
//
//    @Test
//    public void testReplaceWithRegexMatch() {
//        // Prepare test data
//        String serviceName = "test-service";
//        String component = "java-jdbc";
//        String fieldName = "db.statement";
//        String originalValue = "SELECT * FROM users WHERE id = 123 AND name = 'John'";
//
//        // Mock regex-match rule.
//        RegexMatchRule regexRule = new RegexMatchRule(
//                serviceName,
//                component,
//                "client",
//                fieldName,
//                "id = \\d+",
//                "id = ?"
//        );
//        regexRule.addPatternReplacement("name = '[^']*'", "name = ?");
//
//        when(fieldMatchRuleConfig.getRule(serviceName, component, fieldName))
//                .thenReturn(regexRule);
//
//        // Execute the test
//        String result = replacer.replace(serviceName, component, fieldName, originalValue);
//
//        // Verify the result
//        assertEquals("SELECT * FROM users WHERE id = ? AND name = ?", result);
//    }
//
//    @Test
//    public void testReplaceWithNoMatchingRule() {
//        // Prepare test data
//        String serviceName = "test-service";
//        String component = "java-jdbc";
//        String fieldName = "db.statement";
//        String originalValue = "SELECT * FROM users";
//
//        when(fieldMatchRuleConfig.getRule(anyString(), anyString(), anyString()))
//                .thenReturn(null);
//
//        // Execute the test
//        String result = replacer.replace(serviceName, component, fieldName, originalValue);
//
//        // Verify the result - should return the original value.
//        assertEquals(originalValue, result);
//    }
//
//    @Test
//    public void testReplaceWithNullValue() {
//        // Execute the test
//        String result = replacer.replace("service", "component", "field", null);
//
//        // Verify the result - should return null.
//        assertNull(result);
//    }
//
//    @Test
//    public void testReplaceWithEmptyValue() {
//        // Execute the test
//        String result = replacer.replace("service", "component", "field", "");
//
//        // Verify the result - should return an empty string.
//        assertEquals("", result);
//    }
//
//    @Test
//    public void testReplaceWithMultipleRegexPatterns() {
//        // Prepare test data
//        String serviceName = "test-service";
//        String component = "java-jdbc";
//        String fieldName = "db.statement";
//        String originalValue = "INSERT INTO users(id, name, password) VALUES(123, 'John', 'secret123')";
//
//        // Create a rule with multiple regular expressions
//        RegexMatchRule regexRule = new RegexMatchRule(
//                serviceName,
//                component,
//                "client",
//                fieldName,
//                "VALUES\\(\\d+",
//                "VALUES(?");
//        regexRule.addPatternReplacement("'[^']*'", "?");
//
//        when(fieldMatchRuleConfig.getRule(serviceName, component, fieldName))
//                .thenReturn(regexRule);
//
//        // Execute the test
//        String result = replacer.replace(serviceName, component, fieldName, originalValue);
//
//        // Verify the result
//        assertEquals("INSERT INTO users(id, name, password) VALUES(?, ?, ?)", result);
//    }
//
//    @Test
//    public void testReplaceWithInvalidRegexPattern() {
//        // Prepare test data
//        String serviceName = "test-service";
//        String component = "java-jdbc";
//        String fieldName = "db.statement";
//        String originalValue = "SELECT * FROM users";
//
//        // Create a rule with an invalid regular expression
//        RegexMatchRule regexRule = new RegexMatchRule(
//                serviceName,
//                component,
//                "client",
//                fieldName,
//                "[invalid regex",  // invalid regular expression
//                "replacement"
//        );
//
//        when(fieldMatchRuleConfig.getRule(serviceName, component, fieldName))
//                .thenReturn(regexRule);
//
//        // Execute the test
//        String result = replacer.replace(serviceName, component, fieldName, originalValue);
//
//        // Verify the result - should return the original value.
//        assertEquals(originalValue, result);
//    }
}
