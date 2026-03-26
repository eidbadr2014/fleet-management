package com.example.fleet.service;

import com.example.fleet.model.LocationData;
import com.example.fleet.model.MetricValue;
import com.example.fleet.model.SensorDataRequest;
import com.example.fleet.model.ValidatedTelemetry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Normalizes sensor data before persistence.
 * 
 * Normalization rules:
 * - deviceId: lowercase, trimmed
 * - location: coordinates rounded to 6 decimals, speed to 2 decimals
 * - metrics: values extracted (units stripped), precision preserved
 */
public class NormalizationService {

    /**
     * Normalizes a validated request into a clean telemetry object.
     * 
     * @param request The validated sensor data request
     * @return Normalized telemetry ready for persistence
     */
    public ValidatedTelemetry normalize(SensorDataRequest request) {
        return ValidatedTelemetry.builder()
                .deviceId(normalizeDeviceId(request.getDeviceId()))
                .timestamp(request.getTimestamp())
                .location(normalizeLocation(request.getLocation()))
                .metrics(normalizeMetrics(request.getMetrics()))
                .build();
    }

    /**
     * Normalize deviceId: lowercase, trimmed.
     */
    private String normalizeDeviceId(String deviceId) {
        if (deviceId == null) {
            return null;
        }
        return deviceId.toLowerCase().trim();
    }

    /**
     * Normalize location data: round coordinates to 6 decimal places (11cm precision),
     * round speed to 2 decimal places.
     */
    private LocationData normalizeLocation(LocationData location) {
        if (location == null) {
            return null;
        }
        return LocationData.builder()
                .latitude(roundToDecimals(location.getLatitude(), 6))
                .longitude(roundToDecimals(location.getLongitude(), 6))
                .speedKph(roundOptional(location.getSpeedKph(), 2))
                .heading(location.getHeading())
                .build();
    }

    /**
     * Normalize metrics: extract values, preserve original precision.
     * 
     * Unlike location data (which has well-defined precision requirements),
     * arbitrary metrics keep their original precision since different sensors
     * may require different levels of accuracy.
     */
    private Map<String, Double> normalizeMetrics(Map<String, MetricValue> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return null;
        }

        Map<String, Double> normalized = new HashMap<>();
        for (Map.Entry<String, MetricValue> entry : metrics.entrySet()) {
            MetricValue metricValue = entry.getValue();
            if (metricValue != null && metricValue.getValue() != null) {
                normalized.put(entry.getKey(), metricValue.getValue());
            }
        }
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * Round to specified decimal places.
     */
    private double roundToDecimals(Double value, int decimals) {
        if (value == null) {
            return 0.0;
        }
        return BigDecimal.valueOf(value)
                .setScale(decimals, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Round optional value to specified decimal places.
     */
    private Double roundOptional(Double value, int decimals) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value)
                .setScale(decimals, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
