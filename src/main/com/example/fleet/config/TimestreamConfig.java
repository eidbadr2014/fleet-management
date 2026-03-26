package com.example.fleet.config;

/**
 * Timestream configuration loaded from environment variables.
 * 
 * Environment variables:
 * - TIMESTREAM_DATABASE: Database name (default: fleet_telemetry)
 * - TIMESTREAM_TABLE: Table name (default: device_metrics)
 * - AWS_REGION: AWS region for Timestream client
 * 
 * In Lambda, these are set via infrastructure (CDK/SAM/Terraform).
 */
public record TimestreamConfig(
    String database,
    String table
) {
    private static final String DEFAULT_DATABASE = "fleet_telemetry";
    private static final String DEFAULT_TABLE = "device_metrics";

    /**
     * Load configuration from environment variables.
     */
    public static TimestreamConfig fromEnvironment() {
        return new TimestreamConfig(
            env("TIMESTREAM_DATABASE", DEFAULT_DATABASE),
            env("TIMESTREAM_TABLE", DEFAULT_TABLE)
        );
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
