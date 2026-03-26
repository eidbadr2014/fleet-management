package com.example.fleet.service;

import com.example.fleet.model.ErrorResponse.FieldError;
import com.example.fleet.model.LocationData;
import com.example.fleet.model.MetricConstraint;
import com.example.fleet.model.MetricValue;
import com.example.fleet.model.SensorDataRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.example.fleet.model.MetricConstraint.range;

/**
 * Validates incoming sensor data against schema constraints.
 * 
 * Validation rules (aligned with OpenAPI spec):
 * - deviceId: required, pattern ^[a-z0-9-]{3,64}$
 * - timestamp: required, valid Instant, not in future
 * - location: required object with lat/lng required, speedKph/heading optional
 * - metrics: optional map of known metrics with value ranges
 * 
 * Known metrics and their valid ranges:
 * - fuelLevel: 0 to 100 (percent)
 * - engineTemp: -40 to 200 (celsius)
 * - batteryVoltage: 0 to 48 (volts)
 * - oilPressure: 0 to 10 (bar)
 * - tirePressure: 0 to 10 (bar)
 */
public class ValidationService {

    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("^[a-z0-9-]{3,64}$");
    private static final long MAX_FUTURE_SKEW_MS = 60_000; // Allow 1 minute clock skew

    /**
     * Known metrics with their validation constraints.
     * Unknown metrics are allowed through (extensibility).
     */
    private static final Map<String, MetricConstraint> KNOWN_METRICS = Map.of(
            "fuelLevel",      range(0, 100, "percent"),
            "engineTemp",     range(-40, 200, "celsius"),
            "batteryVoltage", range(0, 48, "volts"),
            "oilPressure",    range(0, 10, "bar"),
            "tirePressure",   range(0, 10, "bar")
    );

    /**
     * Validates the request and returns a list of field errors.
     * Empty list means validation passed.
     */
    public List<FieldError> validate(SensorDataRequest request) {
        List<FieldError> errors = new ArrayList<>();

        if (request == null) {
            errors.add(new FieldError("body", "Request body is required"));
            return errors;
        }

        validateDeviceId(request.getDeviceId(), errors);
        validateTimestamp(request.getTimestamp(), errors);
        validateLocation(request.getLocation(), errors);
        validateMetrics(request.getMetrics(), errors);

        return errors;
    }

    private void validateDeviceId(String deviceId, List<FieldError> errors) {
        if (deviceId == null || deviceId.isBlank()) {
            errors.add(new FieldError("deviceId", "deviceId is required"));
            return;
        }

        String normalized = deviceId.toLowerCase().trim();
        if (!DEVICE_ID_PATTERN.matcher(normalized).matches()) {
            errors.add(new FieldError("deviceId", 
                    "Must match pattern ^[a-z0-9-]{3,64}$"));
        }
    }

    private void validateTimestamp(Instant timestamp, List<FieldError> errors) {
        if (timestamp == null) {
            errors.add(new FieldError("timestamp", "timestamp is required"));
            return;
        }

        Instant now = Instant.now();
        if (timestamp.toEpochMilli() > now.toEpochMilli() + MAX_FUTURE_SKEW_MS) {
            errors.add(new FieldError("timestamp", 
                    "timestamp cannot be in the future (max 1 minute skew allowed)"));
        }
    }

    private void validateLocation(LocationData location, List<FieldError> errors) {
        if (location == null) {
            errors.add(new FieldError("location", "location is required"));
            return;
        }

        // Latitude is required
        if (location.getLatitude() == null) {
            errors.add(new FieldError("location.latitude", "latitude is required"));
        } else if (location.getLatitude() < -90 || location.getLatitude() > 90) {
            errors.add(new FieldError("location.latitude", "Must be between -90 and 90"));
        }

        // Longitude is required
        if (location.getLongitude() == null) {
            errors.add(new FieldError("location.longitude", "longitude is required"));
        } else if (location.getLongitude() < -180 || location.getLongitude() > 180) {
            errors.add(new FieldError("location.longitude", "Must be between -180 and 180"));
        }

        // SpeedKph is optional but must be >= 0
        if (location.getSpeedKph() != null && location.getSpeedKph() < 0) {
            errors.add(new FieldError("location.speedKph", "Must be >= 0"));
        }

        // Heading is optional but must be 0-360
        if (location.getHeading() != null && (location.getHeading() < 0 || location.getHeading() > 360)) {
            errors.add(new FieldError("location.heading", "Must be between 0 and 360"));
        }
    }

    private void validateMetrics(Map<String, MetricValue> metrics, List<FieldError> errors) {
        if (metrics == null || metrics.isEmpty()) {
            return; // Metrics are optional
        }

        for (Map.Entry<String, MetricValue> entry : metrics.entrySet()) {
            String metricName = entry.getKey();
            MetricValue metricValue = entry.getValue();

            if (metricValue == null || metricValue.getValue() == null) {
                errors.add(new FieldError("metrics." + metricName, "value is required"));
                continue;
            }

            // Validate known metrics against their constraints
            MetricConstraint constraint = KNOWN_METRICS.get(metricName);
            if (constraint != null && !constraint.isValid(metricValue.getValue())) {
                errors.add(new FieldError("metrics." + metricName, constraint.errorMessage()));
            }
            // Unknown metrics are allowed (extensibility)
        }
    }
}
