package org.prism.apm.parser.enums;

/**
 * Span component values are as follows：
 * java-web-servlet
 * dubbo
 * java-jdbc
 * java-redis
 * java-spring-rest-template
 * java-mongo
 * feign
 * java-kafka
 * java-rocketmq
 * java-elasticsearch
 *
 * @author john
 * @date 2024-05-10 16:13
 */
public enum SpanComponentEnum {

    WEB_SERVLET(1001, "java-web-servlet"),
    DUBBO(1002, "dubbo"),
    FEIGN(1003, "feign"),
    SPRING_REST_TEMPLATE(1004, "java-spring-rest-template"),
    JDBC(2001, "java-jdbc"),
    REDIS(2002, "java-redis"),
    MONGO(2003, "java-mongo"),
    KAFKA(2004, "java-kafka"),
    ROCKETMQ(2005, "java-rocketmq"),
    ELASTICSEARCH(2006, "java-elasticsearch"),


    UNKNOWN(-1, "unknown");

    private final int value;
    private final String name;

    SpanComponentEnum(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static int getValueByName(String name) {
        for (SpanComponentEnum component : SpanComponentEnum.values()) {
            if (component.getName().equals(name)) {
                return component.getValue();
            }
        }
        return UNKNOWN.getValue();
    }

    public static SpanComponentEnum getEnumByName(String name) {
        for (SpanComponentEnum component : SpanComponentEnum.values()) {
            if (component.getName().equals(name)) {
                return component;
            }
        }
        return UNKNOWN;
    }

}
