package org.prism.apm.parser.model.otel.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom deserializer for a Span object, converting it into a Map.
 * It specifically handles the 'attributes' field by converting it from a list of objects to a map.
 */
public class OtelSpanMapDeserializer extends JsonDeserializer<Map<String, Object>> {
    @Override
    public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Map<String, Object> spanMap = new HashMap<>();
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw JsonMappingException.from(ctxt, "Expected an object to deserialize span");
        }
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();
            if ("attributes".equals(fieldName)) {
                Map<String, Object> attributesMap = parseAttributes(p, ctxt);
                spanMap.put(fieldName, attributesMap);
            } else {
                Object value = p.readValueAs(Object.class);
                spanMap.put(fieldName, value);
            }
        }
        return spanMap;
    }

    private Map<String, Object> parseAttributes(JsonParser p, DeserializationContext ctxt) throws IOException {
        Map<String, Object> attributesMap = new HashMap<>();
        if (p.currentToken() != JsonToken.START_ARRAY) {
            if (p.currentToken() == JsonToken.VALUE_NULL) {
                return attributesMap;
            }
            throw JsonMappingException.from(ctxt, "Expected an array for attributes");
        }
        while (p.nextToken() != JsonToken.END_ARRAY) {
            if (p.currentToken() != JsonToken.START_OBJECT) {
                 throw JsonMappingException.from(ctxt, "Expected an object for attribute entry");
            }
            String key = null;
            Object value = null;
            while (p.nextToken() != JsonToken.END_OBJECT) {
                String attrFieldName = p.currentName();
                p.nextToken();
                if ("key".equals(attrFieldName)) {
                    key = p.getText();
                } else if ("value".equals(attrFieldName)) {
                    value = extractStructuredValue(p);
                } else {
                    p.skipChildren();
                }
            }
            if (key != null) {
                attributesMap.put(key, value);
            }
        }
        return attributesMap;
    }

    private Object extractStructuredValue(JsonParser p) throws IOException {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            return p.readValueAs(Object.class);
        }
        Object finalValue = null;
        p.nextToken();
        String valueTypeKey = p.currentName();
        p.nextToken();
        if ("arrayValue".equals(valueTypeKey)) {
            finalValue = parseArrayValue(p);
        } else {
            finalValue = p.getText();
        }
        p.nextToken();
        return finalValue;
    }

    private List<String> parseArrayValue(JsonParser p) throws IOException {
        if (p.currentToken() != JsonToken.START_OBJECT) return new ArrayList<>();
        List<String> list = new ArrayList<>();
        while(p.nextToken() != JsonToken.END_OBJECT) {
            if ("values".equals(p.currentName())) {
                p.nextToken();
                if (p.currentToken() == JsonToken.START_ARRAY) {
                    while (p.nextToken() != JsonToken.END_ARRAY) {
                        if (p.currentToken() == JsonToken.START_OBJECT) {
                            p.nextToken();
                            p.nextToken();
                            list.add(p.getText());
                            p.nextToken();
                        }
                    }
                }
            } else {
                p.skipChildren();
            }
        }
        return list;
    }
}
