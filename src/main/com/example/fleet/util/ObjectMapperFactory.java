package com.example.fleet.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Factory for creating configured ObjectMapper instances.
 * 
 * Configuration:
 * - Java 8 time support (Instant, etc.)
 * - Ignore unknown properties (forward compatibility)
 * - ISO 8601 date format
 */
public final class ObjectMapperFactory {

    private ObjectMapperFactory() {
        // Utility class
    }

    /**
     * Creates a configured ObjectMapper instance.
     */
    public static ObjectMapper create() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Java 8 time support
        mapper.registerModule(new JavaTimeModule());
        
        // Serialize dates as ISO 8601 strings, not timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Ignore unknown properties for forward compatibility
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        
        return mapper;
    }
}
