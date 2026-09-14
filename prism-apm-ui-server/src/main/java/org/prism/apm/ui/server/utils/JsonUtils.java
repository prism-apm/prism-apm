package org.prism.apm.ui.server.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            // Throw a custom runtime exception when serialization fails
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            // Throw a custom runtime exception when deserialization fails
            throw new RuntimeException("Failed to deserialize JSON to object of class " + clazz.getSimpleName(), e);
        }
    }

    public static Map<String, Object> fromJsonToMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            // Throw a custom runtime exception when deserialization fails
            log.error("Failed to deserialize JSON to Map", e);
            throw new RuntimeException("Failed to deserialize JSON to Map", e);
        }
    }

    public static <T> List<T> parseArray(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
            return objectMapper.readValue(json, listType);
        } catch (Exception e) {
            throw new RuntimeException("JSON parse Array error", e);
        }
    }

}
