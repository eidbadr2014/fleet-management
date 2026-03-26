package com.example.fleet.service;

import com.example.fleet.model.ErrorResponse.FieldError;
import com.example.fleet.model.LocationData;
import com.example.fleet.model.MetricValue;
import com.example.fleet.model.SensorDataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ValidationService.
 * 
 * Tests validation rules aligned with OpenAPI spec.
 */
class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }

    @Nested
    @DisplayName("Valid requests")
    class ValidRequests {

        @Test
        @DisplayName("should accept valid telemetry with all fields")
        void validFullPayload() {
            SensorDataRequest request = createValidRequest();
            
            List<FieldError> errors = validationService.validate(request);
            
            assertTrue(errors.isEmpty(), "Expected no validation errors but got: " + errors);
        }

        @Test
        @DisplayName("should accept valid telemetry with only required fields")
        void validMinimalPayload() {
            SensorDataRequest request = new SensorDataRequest();
            request.setDeviceId("truck-001");
            request.setTimestamp(Instant.now().minusSeconds(10));
            request.setLocation(LocationData.builder()
                    .latitude(37.7749)
                    .longitude(-122.4194)
                    .build());
            
            List<FieldError> errors = validationService.validate(request);
            
            assertTrue(errors.isEmpty(), "Expected no validation errors but got: " + errors);
        }
    }

    @Nested
    @DisplayName("DeviceId validation")
    class DeviceIdValidation {

        @Test
        @DisplayName("should reject null deviceId")
        void nullDeviceId() {
            SensorDataRequest request = createValidRequest();
            request.setDeviceId(null);
            
            List<FieldError> errors = validationService.validate(request);
            
            assertFieldError(errors, "deviceId", "deviceId is required");
        }

        @Test
        @DisplayName("should reject empty deviceId")
        void emptyDeviceId() {
            SensorDataRequest request = createValidRequest();
            request.setDeviceId("  ");
            
            List<FieldError> errors = validationService.validate(request);
            
            assertFieldError(errors, "deviceId", "deviceId is required");
        }

        @Test
        @DisplayName("should reject deviceId with invalid characters")
        void invalidCharactersInDeviceId() {
            SensorDataRequest request = createValidRequest();
            request.setDeviceId("truck_001!");
            
            List<FieldError> errors = validationService.validate(request);
            
            assertFieldError(errors, "deviceId", "Must match pattern");
        }

        @Test
        @DisplayName("should reject deviceId that is too short")
        void tooShortDeviceId() {
            SensorDataRequest request = createValidRequest();
            request.setDeviceId("ab");
            
            List<FieldError> errors = validationService.validate(request);
            
            assertFieldError(errors, "deviceId", "Must match pattern");
        }

        @Test
        @DisplayName("should accept valid deviceId patterns")
        void validDeviceIdPatterns() {
            String[] validIds = {"truck-001", "abc", "device-123-test", "a1b2c3"};
            
            for (String deviceId : validIds) {
                SensorDataRequest request = createValidRequest();
                request.setDeviceId(deviceId);
                
                List<FieldError> errors = validationService.validate(request);
                
                assertTrue(errors.isEmpty(), "Expected " + deviceId + " to be valid");
            }
        }
    }

    @Nested
    @DisplayName("Timestamp validation")
    class TimestampValidation {

        @Test
        @DisplayName("should reject null timestamp")
        void nullTimestamp() {
            SensorDataRequest request = createValidRequest();
            request.setTimestamp(null);
            
            List<FieldError> errors = validationService.validate(request);
            
            assertFieldError(errors, "timestamp", "timestamp is required");
        }

        @Test
        @DisplayName("should reject future timestamp beyond allowed skew")
        void futureTimestamp() {
            SensorDataRequest request = createValidRequest();
            request.setTimestamp(Instant.now().plusSeconds(120));
            
            List<FieldError> errors = validationService.validate(request);
            
            assertFieldError(errors, "timestamp", "cannot be in the future");
        }

        @Test
        @DisplayName("should accept timestamp within allowed clock skew")
        void timestampWithinSkew() {
            SensorDataRequest request = createValidRequest();
            request.setTimestamp(Instant.now().plusSeconds(30));
            
            List<FieldError> errors = validationService.validate(request);
            
            assertTrue(errors.isEmpty(), "Expected timestamp within 1 min skew to be valid");
        }
    }

    @Nested
    @DisplayName("Location validation")
    class LocationValidation {

        @Test
        @DisplayName("should reject null location")
        void nullLocation() {
            SensorDataRequest request = createValidRequest();
            request.setLocation(null);
            
            List<FieldError> errors = validationService.validate(request);
            
            assertFieldError(errors, "location", "location is required");
        }

        @Test
        @DisplayName("should reject null latitude")
        void nullLatitude() {
            SensorDataRequest request = createValidRequest();
            request.getLocation().setLatitude(null);
            
            List<FieldError> errors = validationService.validate(request);
            
            assertFieldError(errors, "location.latitude", "latitude is required");
        }

        @Test
        @DisplayName("should reject latitude below -90")
        void latitudeTooLow() {
            SensorDataRequest request = createValidRequest();
            request.getLocation().setLatitude(-91.0);
            
            List<FieldError> errors = validationService.validate(request);
            
            assertFieldError(errors, "location.latitude", "Must be between -90 and 90");
        }

        @Test
        @DisplayName("should reject latitude above 90")
        void latitudeTooHigh() {
            SensorDataRequest request = createValidRequest();
            request.getLocation().setLatitude(91.0);
            
            List<FieldError> errors = validationService.validate(request);
            
            assertFieldError(errors, "location.latitude", "Must be between -90 and 90");
        }

        @Test
        @DisplayName("should reject null longitude")
        void nullLongitude() {
            SensorDataRequest request = createValidRequest();
            request.getLocation().setLongitude(null);
            
            List<FieldError> errors = validationService.validate(request);
            
            assertFieldError(errors, "location.longitude", "longitude is required");
        }

        @Test
        @DisplayName("should reject longitude below -180")
        void longitudeTooLow() {
            SensorDataRequest request = createValidRequest();
            request.getLocation().setLongitude(-181.0);
            
            List<FieldError> errors = validationService.validate(request);
            
            assertFieldError(errors, "location.longitude", "Must be between -180 and 180");
        }

        @Test
        @DisplayName("should reject longitude above 180")
        void longitudeTooHigh() {
            SensorDataRequest request = createValidRequest();
            request.getLocation().setLongitude(181.0);
            
            List<FieldError> errors = validationService.validate(request);
            
            assertFieldError(errors, "location.longitude", "Must be between -180 and 180");
        }

        @Test
        @DisplayName("should reject negative speedKph")
        void negativeSpeed() {
            SensorDataRequest request = createValidRequest();
            request.getLocation().setSpeedKph(-10.0);
            
            List<FieldError> errors = validationService.validate(request);
            
            assertFieldError(errors, "location.speedKph", "Must be >= 0");
        }

        @Test
        @DisplayName("should reject heading outside 0-360")
        void invalidHeading() {
            SensorDataRequest request = createValidRequest();
            request.getLocation().setHeading(361);
            
            List<FieldError> errors = validationService.validate(request);
            
            assertFieldError(errors, "location.heading", "Must be between 0 and 360");
        }

        @Test
        @DisplayName("should accept boundary latitude values")
        void boundaryLatitude() {
            double[] validLatitudes = {-90.0, 0.0, 90.0};
            
            for (double lat : validLatitudes) {
                SensorDataRequest request = createValidRequest();
                request.getLocation().setLatitude(lat);
                
                List<FieldError> errors = validationService.validate(request);
                
                assertTrue(errors.isEmpty(), "Expected latitude " + lat + " to be valid");
            }
        }
    }

    @Nested
    @DisplayName("Metrics validation")
    class MetricsValidation {

        @Test
        @DisplayName("should accept null metrics")
        void nullMetrics() {
            SensorDataRequest request = createValidRequest();
            request.setMetrics(null);
            
            List<FieldError> errors = validationService.validate(request);
            
            assertTrue(errors.isEmpty(), "Expected null metrics to be valid");
        }

        @Test
        @DisplayName("should reject fuelLevel outside 0-100")
        void invalidFuelLevel() {
            SensorDataRequest request = createValidRequest();
            request.setMetrics(Map.of("fuelLevel", new MetricValue(150.0)));
            
            List<FieldError> errors = validationService.validate(request);
            
            assertFieldError(errors, "metrics.fuelLevel", "Must be between 0 and 100");
        }

        @Test
        @DisplayName("should reject engineTemp outside -40 to 200")
        void invalidEngineTemp() {
            SensorDataRequest request = createValidRequest();
            request.setMetrics(Map.of("engineTemp", new MetricValue(250.0)));
            
            List<FieldError> errors = validationService.validate(request);
            
            assertFieldError(errors, "metrics.engineTemp", "Must be between -40 and 200");
        }

        @Test
        @DisplayName("should accept valid metrics")
        void validMetrics() {
            SensorDataRequest request = createValidRequest();
            Map<String, MetricValue> metrics = new HashMap<>();
            metrics.put("fuelLevel", new MetricValue(85.0, "percent"));
            metrics.put("engineTemp", new MetricValue(90.0, "celsius"));
            metrics.put("batteryVoltage", new MetricValue(12.5, "volts"));
            request.setMetrics(metrics);
            
            List<FieldError> errors = validationService.validate(request);
            
            assertTrue(errors.isEmpty(), "Expected valid metrics to pass validation but got: " + errors);
        }

        @Test
        @DisplayName("should accept unknown metrics (extensibility)")
        void unknownMetrics() {
            SensorDataRequest request = createValidRequest();
            request.setMetrics(Map.of("customMetric", new MetricValue(42.0)));
            
            List<FieldError> errors = validationService.validate(request);
            
            assertTrue(errors.isEmpty(), "Expected unknown metrics to be allowed");
        }
    }

    @Nested
    @DisplayName("Multiple validation errors")
    class MultipleErrors {

        @Test
        @DisplayName("should return all validation errors at once")
        void multipleErrors() {
            SensorDataRequest request = new SensorDataRequest();
            // Missing all required fields
            
            List<FieldError> errors = validationService.validate(request);
            
            assertTrue(errors.size() >= 3, "Expected multiple validation errors");
        }
    }

    // Helper methods

    private SensorDataRequest createValidRequest() {
        SensorDataRequest request = new SensorDataRequest();
        request.setDeviceId("truck-001");
        request.setTimestamp(Instant.now().minusSeconds(10));
        request.setLocation(LocationData.builder()
                .latitude(37.7749)
                .longitude(-122.4194)
                .speedKph(72.7)
                .heading(270)
                .build());
        return request;
    }

    private void assertFieldError(List<FieldError> errors, String expectedField, String expectedMessageContains) {
        boolean found = errors.stream()
                .anyMatch(e -> e.getField().equals(expectedField) && 
                              e.getError().contains(expectedMessageContains));
        
        assertTrue(found, String.format(
                "Expected error for field '%s' containing '%s', but got: %s",
                expectedField, expectedMessageContains, 
                errors.stream().map(e -> e.getField() + ":" + e.getError()).toList()
        ));
    }
}
