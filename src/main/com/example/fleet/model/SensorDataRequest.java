package com.example.fleet.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Incoming sensor data from IoT devices.
 * 
 * Generic structure supporting arbitrary metrics.
 * 
 * Expected JSON:
 * {
 *   "deviceId": "truck-001",
 *   "timestamp": "2026-03-25T10:30:00Z",
 *   "location": {
 *     "latitude": 37.7749,
 *     "longitude": -122.4194,
 *     "speedKph": 72.7,
 *     "heading": 270
 *   },
 *   "metrics": {
 *     "fuelLevel": {"value": 85.2, "unit": "percent"},
 *     "engineTemp": {"value": 90.5, "unit": "celsius"},
 *     "batteryVoltage": {"value": 12.8},
 *     "oilPressure": {"value": 3.5},
 *     "tirePressure": {"value": 2.4}
 *   }
 * }
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SensorDataRequest {

    private String deviceId;
    private Instant timestamp;
    private LocationData location;
    private Map<String, MetricValue> metrics;
}
