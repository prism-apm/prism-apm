package org.prism.apm.parser.service.common;

import java.util.Map;

public interface JsonService {
    String toJson(Object object);

    <T> T fromJson(String json, Class<T> clazz);

    Map<String, Object> fromJsonToMap(String json);
}
