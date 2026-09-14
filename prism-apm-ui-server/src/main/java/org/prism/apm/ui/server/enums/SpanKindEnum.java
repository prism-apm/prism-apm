package org.prism.apm.ui.server.enums;

import lombok.Getter;

@Getter
public enum SpanKindEnum {

    INTERNAL((short)1, "internal"),
    SERVER((short)2, "server"),
    CLIENT((short)3, "client"),
    PRODUCER((short)4, "producer"),
    CONSUMER((short)5, "consumer"),
    UNKNOWN((short)0, "unknown");

    private final short value;
    private final String name;

    SpanKindEnum(short value, String name) {
        this.value = value;
        this.name = name;
    }

    public static short getValueByName(String name) {
        for (SpanKindEnum item : SpanKindEnum.values()) {
            if (item.getName().equals(name)) {
                return item.getValue();
            }
        }
        return UNKNOWN.getValue();
    }

    public static SpanKindEnum getEnumByName(String name) {
        for (SpanKindEnum item : SpanKindEnum.values()) {
            if (item.getName().equals(name)) {
                return item;
            }
        }
        return UNKNOWN;
    }

    public static SpanKindEnum getEnumByValue(Short value) {
        if (value == null) {
            return UNKNOWN;
        }
        for (SpanKindEnum item : SpanKindEnum.values()) {
            if (item.getValue() == value) {
                return item;
            }
        }
        return UNKNOWN;
    }

}
