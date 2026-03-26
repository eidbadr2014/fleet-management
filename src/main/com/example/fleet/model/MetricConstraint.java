package com.example.fleet.model;

/**
 * Defines validation constraints for a known metric.
 * 
 * Immutable record with fluent factory method for clean DSL-style configuration.
 */
public record MetricConstraint(double min, double max, String unit) {

    /**
     * Creates a constraint with the given range and unit.
     */
    public static MetricConstraint range(double min, double max, String unit) {
        return new MetricConstraint(min, max, unit);
    }

    /**
     * Creates a constraint with the given range (no unit).
     */
    public static MetricConstraint range(double min, double max) {
        return new MetricConstraint(min, max, null);
    }

    /**
     * Checks if the value is within the valid range.
     */
    public boolean isValid(double value) {
        return value >= min && value <= max;
    }

    /**
     * Returns a human-readable error message for invalid values.
     */
    public String errorMessage() {
        if (unit != null) {
            return String.format("Must be between %.0f and %.0f %s", min, max, unit);
        }
        return String.format("Must be between %.0f and %.0f", min, max);
    }
}
