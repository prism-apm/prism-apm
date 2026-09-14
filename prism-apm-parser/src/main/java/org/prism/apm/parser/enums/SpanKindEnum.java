package org.prism.apm.parser.enums;

/**
 *
 * @author john
 * @date 2024-05-10 16:13
 */
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

    public short getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static short getValueByName(String name) {
        for (SpanKindEnum component : SpanKindEnum.values()) {
            if (component.getName().equals(name)) {
                return component.getValue();
            }
        }
        return UNKNOWN.getValue();
    }

    public static SpanKindEnum getEnumByName(String name) {
        for (SpanKindEnum component : SpanKindEnum.values()) {
            if (component.getName().equals(name)) {
                return component;
            }
        }
        return UNKNOWN;
    }

}
