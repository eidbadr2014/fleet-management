package com.example.fleet.service;

import com.example.fleet.model.ErrorResponse.FieldError;
import com.example.fleet.model.IngestResult;
import com.example.fleet.model.SensorDataRequest;
import com.example.fleet.model.ValidatedTelemetry;
import com.example.fleet.repository.TelemetryRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Orchestrates the telemetry ingestion flow.
 * 
 * Flow:
 * 1. Validate incoming data
 * 2. Normalize (clean/transform)
 * 3. Persist to Timestream
 */
@RequiredArgsConstructor
public class TelemetryService {

    private final ValidationService validationService;
    private final NormalizationService normalizationService;
    private final TelemetryRepository telemetryRepository;

    /**
     * Process incoming telemetry data.
     * 
     * @param request The incoming sensor data
     * @return Result containing either validated telemetry or validation errors
     */
    public IngestResult ingest(SensorDataRequest request) {
        // Step 1: Validate
        List<FieldError> validationErrors = validationService.validate(request);
        
        if (!validationErrors.isEmpty()) {
            return IngestResult.failure(validationErrors);
        }

        // Step 2: Normalize
        ValidatedTelemetry telemetry = normalizationService.normalize(request);

        // Step 3: Persist
        telemetryRepository.save(telemetry);

        return IngestResult.success(telemetry);
    }
}
