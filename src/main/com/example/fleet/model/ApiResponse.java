package com.example.fleet.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * Standard API response wrapper.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {

    private final String status;
    private final String message;
    private final String deviceId;
    private final String timestamp;

    /**
     * Factory method for success response.
     */
    public static ApiResponse success(String deviceId, String timestamp) {
        return builder()
                .status("OK")
                .message("Telemetry ingested successfully")
                .deviceId(deviceId)
                .timestamp(timestamp)
                .build();
    }
}
