package org.prism.apm.ui.server.enums;

import lombok.Getter;

@Getter
public enum SystemTypeEnum {

    SPRING_SCHEDULE("spring-schedule", "spring-schedule"),
    MYSQL("database", "mysql"),
    POSTGRESQL("database", "postgresql"),
    MONGODB("database", "mongodb"),
    REDIS("database", "redis"),
    ELASTICSEARCH("database", "elasticsearch"),
    ROCKETMQ("message", "rocketmq"),
    KAFKA("message", "kafka"),
    APACHE_DUBBO("RPC", "apache_dubbo"),
    GRPC("RPC", "grpc"),
    HTTP("http", "http"),
    UNKNOWN("unknown", "unknown");

    private final String type;
    private final String name;

    SystemTypeEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public static String getTypeByName(String name) {
        for (SystemTypeEnum item : SystemTypeEnum.values()) {
            if (item.name.equals(name)) {
                return item.type;
            }
        }
        return UNKNOWN.getType();
    }

    public static SystemTypeEnum getEnumByName(String name) {
        for (SystemTypeEnum item : SystemTypeEnum.values()) {
            if (item.getName().equals(name)) {
                return item;
            }
        }
        return UNKNOWN;
    }

}
