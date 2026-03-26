package com.example.fleet.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.example.fleet.model.ValidatedTelemetry;
import com.example.fleet.repository.TelemetryRepository;
import com.example.fleet.service.NormalizationService;
import com.example.fleet.service.TelemetryService;
import com.example.fleet.service.ValidationService;
import com.example.fleet.util.ObjectMapperFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DataIngestHandler.
 * 
 * Tests Lambda handler behavior with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class DataIngestHandlerTest {

    private DataIngestHandler handler;
    private ObjectMapper objectMapper;
    
    @Mock
    private TelemetryRepository mockRepository;
    
    @Mock
    private Context mockContext;
    
    @Mock
    private LambdaLogger mockLogger;

    @BeforeEach
    void setUp() {
        objectMapper = ObjectMapperFactory.create();
        
        TelemetryService telemetryService = new TelemetryService(
                new ValidationService(),
                new NormalizationService(),
                mockRepository
        );
        
        handler = new DataIngestHandler(objectMapper, telemetryService);
        
        when(mockContext.getAwsRequestId()).thenReturn("test-request-123");
        when(mockContext.getLogger()).thenReturn(mockLogger);
    }

    @Nested
    @DisplayName("Success cases")
    class SuccessCases {

        @Test
        @DisplayName("should return 200 for valid telemetry")
        void validTelemetry() throws Exception {
            APIGatewayProxyRequestEvent event = createValidEvent();
            
            APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
            
            assertEquals(200, response.getStatusCode());
            
            JsonNode body = objectMapper.readTree(response.getBody());
            assertEquals("OK", body.get("status").asText());
            assertEquals("truck-001", body.get("deviceId").asText());
            assertNotNull(body.get("timestamp"));
        }

        @Test
        @DisplayName("should persist telemetry to repository")
        void persistsTelemetry() {
            APIGatewayProxyRequestEvent event = createValidEvent();
            
            handler.handleRequest(event, mockContext);
            
            ArgumentCaptor<ValidatedTelemetry> captor = ArgumentCaptor.forClass(ValidatedTelemetry.class);
            verify(mockRepository).save(captor.capture());
            
            ValidatedTelemetry saved = captor.getValue();
            assertEquals("truck-001", saved.getDeviceId());
            assertEquals(37.77, saved.getLocation().getLatitude(), 0.01);
            assertEquals(-122.42, saved.getLocation().getLongitude(), 0.01);
        }

        @Test
        @DisplayName("should normalize deviceId to lowercase")
        void normalizesDeviceId() {
            APIGatewayProxyRequestEvent event = createEvent("""
                {
                    "deviceId": "TRUCK-001",
                    "timestamp": "%s",
                    "location": {
                        "latitude": 37.7749,
                        "longitude": -122.4194
                    }
                }
                """.formatted(Instant.now().toString()));
            
            handler.handleRequest(event, mockContext);
            
            ArgumentCaptor<ValidatedTelemetry> captor = ArgumentCaptor.forClass(ValidatedTelemetry.class);
            verify(mockRepository).save(captor.capture());
            
            assertEquals("truck-001", captor.getValue().getDeviceId());
        }
    }

    @Nested
    @DisplayName("Validation errors (400)")
    class ValidationErrors {

        @Test
        @DisplayName("should return 400 for missing body")
        void missingBody() throws Exception {
            APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
            event.setBody(null);
            
            APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
            
            assertEquals(400, response.getStatusCode());
            
            JsonNode body = objectMapper.readTree(response.getBody());
            assertEquals("VALIDATION_ERROR", body.get("code").asText());
        }

        @Test
        @DisplayName("should return 400 for invalid JSON")
        void invalidJson() throws Exception {
            APIGatewayProxyRequestEvent event = createEvent("not valid json");
            
            APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
            
            assertEquals(400, response.getStatusCode());
            
            JsonNode body = objectMapper.readTree(response.getBody());
            assertEquals("INVALID_JSON", body.get("code").asText());
        }

        @Test
        @DisplayName("should return 400 for missing required fields")
        void missingRequiredFields() throws Exception {
            APIGatewayProxyRequestEvent event = createEvent("""
                {
                    "deviceId": "truck-001"
                }
                """);
            
            APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
            
            assertEquals(400, response.getStatusCode());
            
            JsonNode body = objectMapper.readTree(response.getBody());
            assertEquals("VALIDATION_ERROR", body.get("code").asText());
            assertTrue(body.has("details"));
        }

        @Test
        @DisplayName("should return 400 for invalid latitude")
        void invalidLatitude() throws Exception {
            APIGatewayProxyRequestEvent event = createEvent("""
                {
                    "deviceId": "truck-001",
                    "timestamp": "%s",
                    "location": {
                        "latitude": 999,
                        "longitude": -122.4194
                    }
                }
                """.formatted(Instant.now().toString()));
            
            APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
            
            assertEquals(400, response.getStatusCode());
            
            JsonNode body = objectMapper.readTree(response.getBody());
            assertEquals("VALIDATION_ERROR", body.get("code").asText());
        }

        @Test
        @DisplayName("should return 400 for invalid deviceId pattern")
        void invalidDeviceIdPattern() throws Exception {
            APIGatewayProxyRequestEvent event = createEvent("""
                {
                    "deviceId": "truck_001!",
                    "timestamp": "%s",
                    "location": {
                        "latitude": 37.7749,
                        "longitude": -122.4194
                    }
                }
                """.formatted(Instant.now().toString()));
            
            APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
            
            assertEquals(400, response.getStatusCode());
        }

        @Test
        @DisplayName("should not persist when validation fails")
        void noPersisteOnValidationError() {
            APIGatewayProxyRequestEvent event = createEvent("""
                {
                    "deviceId": "truck-001",
                    "timestamp": "%s",
                    "location": {
                        "latitude": 999,
                        "longitude": -122.4194
                    }
                }
                """.formatted(Instant.now().toString()));
            
            handler.handleRequest(event, mockContext);
            
            verify(mockRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Response format")
    class ResponseFormat {

        @Test
        @DisplayName("should include Content-Type header")
        void contentTypeHeader() {
            APIGatewayProxyRequestEvent event = createValidEvent();
            
            APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
            
            assertEquals("application/json", response.getHeaders().get("Content-Type"));
        }

        @Test
        @DisplayName("should include requestId in error responses")
        void requestIdInErrors() throws Exception {
            APIGatewayProxyRequestEvent event = createEvent("{}");
            
            APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
            
            JsonNode body = objectMapper.readTree(response.getBody());
            assertEquals("test-request-123", body.get("requestId").asText());
        }
    }

    // Helper methods

    private APIGatewayProxyRequestEvent createValidEvent() {
        return createEvent("""
            {
                "deviceId": "truck-001",
                "timestamp": "%s",
                "location": {
                    "latitude": 37.7749,
                    "longitude": -122.4194,
                    "speedKph": 72.7,
                    "heading": 270
                }
            }
            """.formatted(Instant.now().toString()));
    }

    private APIGatewayProxyRequestEvent createEvent(String body) {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setBody(body);
        return event;
    }
}
