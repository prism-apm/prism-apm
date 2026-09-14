package org.prism.apm.parser.constants;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class SpanInfoFields {

    private SpanInfoFields() {
        // Private constructor to prevent instantiation
    }

    public static final String SERVICE_NAME = "serviceName";
    public static final String HOST_NAME = "hostName";
    public static final String SPAN_KIND = "spanKind";
    public static final String ACTION_NAME = "actionName";
    public static final String TARGET_NAME = "targetName";
    public static final String COMPONENT = "component";
    public static final String SYSTEM_TYPE = "systemType";
    public static final String REMOTE_ADDR = "remoteAddr";
    public static final String ERROR = "error";

    public static final Set<String> ALLOWED_FIELDS;
    static {
        Set<String> fields = new HashSet<>();
        fields.add(SERVICE_NAME);
        fields.add(HOST_NAME);
        fields.add(SPAN_KIND);
        fields.add(ACTION_NAME);
        fields.add(TARGET_NAME);
        fields.add(COMPONENT);
        fields.add(SYSTEM_TYPE);
        fields.add(REMOTE_ADDR);
        fields.add(ERROR);
        ALLOWED_FIELDS = Collections.unmodifiableSet(fields);
    }
}
