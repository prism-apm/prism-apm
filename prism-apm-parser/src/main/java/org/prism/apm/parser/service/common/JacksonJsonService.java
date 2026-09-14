package org.prism.apm.parser.service.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.prism.apm.parser.exception.JsonException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class JacksonJsonService implements JsonService {

    private final ObjectMapper objectMapper;

    public JacksonJsonService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            // Throw a custom runtime exception when serialization fails
            throw new JsonException("Failed to serialize object to JSON", e);
        }
    }

    @Override
    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            // Throw a custom runtime exception when deserialization fails
            log.error("Failed to deserialize JSON to object of class {}", clazz.getSimpleName(), e);
            throw new JsonException("Failed to deserialize JSON to object of class " + clazz.getSimpleName(), e);
        }
    }

    @Override
    public Map<String, Object> fromJsonToMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            // Throw a custom runtime exception when deserialization fails
            log.error("Failed to deserialize JSON to Map", e);
            throw new JsonException("Failed to deserialize JSON to Map", e);
        }
    }
}
