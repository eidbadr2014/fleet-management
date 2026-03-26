package com.example.fleet.repository;

import com.example.fleet.model.ValidatedTelemetry;

/**
 * Repository interface for telemetry persistence.
 * 
 * Abstracts the underlying storage (Timestream) from business logic.
 */
public interface TelemetryRepository {

    /**
     * Persist telemetry data point.
     * 
     * @param telemetry Validated and normalized telemetry data
     */
    void save(ValidatedTelemetry telemetry);
}
