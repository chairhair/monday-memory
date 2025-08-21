package com.monday.monday_backend.query.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.*;

@Converter(autoApply = false)
public class JsonMapConverter implements AttributeConverter<Map<String,Object>, String> {
    private static final ObjectMapper om = new ObjectMapper();
    @Override public String convertToDatabaseColumn(Map<String,Object> attribute) {
        try { return attribute == null ? null : om.writeValueAsString(attribute); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }
    @Override public Map<String,Object> convertToEntityAttribute(String dbData) {
        try { return dbData == null ? null : om.readValue(dbData, Map.class); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }
}