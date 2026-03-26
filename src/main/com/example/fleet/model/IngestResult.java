package com.example.fleet.model;

import com.example.fleet.model.ErrorResponse.FieldError;

import java.util.List;

/**
 * Result of the telemetry ingest operation.
 * 
 * Uses sealed interface pattern for type-safe success/failure handling.
 */
public sealed interface IngestResult {

    record Success(ValidatedTelemetry telemetry) implements IngestResult {}
    
    record Failure(List<FieldError> errors) implements IngestResult {}

    // Factory methods for cleaner API
    static IngestResult success(ValidatedTelemetry telemetry) {
        return new Success(telemetry);
    }

    static IngestResult failure(List<FieldError> errors) {
        return new Failure(errors);
    }

    // Convenience methods
    default boolean isSuccess() {
        return this instanceof Success;
    }

    default ValidatedTelemetry getTelemetry() {
        return this instanceof Success s ? s.telemetry() : null;
    }

    default List<FieldError> getErrors() {
        return this instanceof Failure f ? f.errors() : null;
    }
}
