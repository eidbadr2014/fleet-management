package com.example.fleet.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * Normalized and validated telemetry data ready for persistence.
 * 
 * Normalization applied:
 * - deviceId: lowercase, trimmed
 * - numeric values: rounded to 2 decimal places
 * - timestamp: validated Instant
 */
@Getter
@Builder
public class ValidatedTelemetry {

    private final String deviceId;
    private final Instant timestamp;
    private final LocationData location;
    private final Map<String, Double> metrics;  // Extracted values only (no units)
}
