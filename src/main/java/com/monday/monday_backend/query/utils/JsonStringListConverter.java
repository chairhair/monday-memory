package com.monday.monday_backend.query.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.*;

@Converter(autoApply = false)
public class JsonStringListConverter implements AttributeConverter<List<String>, String> {
    private static final ObjectMapper om = new ObjectMapper();
    @Override public String convertToDatabaseColumn(List<String> attribute) {
        try { return attribute == null ? null : om.writeValueAsString(attribute); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }
    @Override public List<String> convertToEntityAttribute(String dbData) {
        try { return dbData == null ? null : om.readValue(dbData, List.class); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }
}
