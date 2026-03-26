package com.example.fleet.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single metric value with optional unit.
 * 
 * Example JSON formats:
 * - {"value": 85.2}
 * - {"value": 85.2, "unit": "percent"}
 * 
 * Known metrics and their expected units:
 * - fuelLevel: percent (0-100)
 * - engineTemp: celsius (-40 to 200)
 * - batteryVoltage: volts (0-48)
 * - oilPressure: bar (0-10)
 * - tirePressure: bar (0-10)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetricValue {

    private Double value;
    private String unit;  // optional, for informational purposes
    
    /**
     * Convenience constructor for value-only metrics.
     */
    public MetricValue(Double value) {
        this.value = value;
        this.unit = null;
    }
}
