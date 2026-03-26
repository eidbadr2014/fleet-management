package com.example.fleet.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Standard error response following OpenAPI spec.
 * 
 * Matches:
 * {
 *   "code": "VALIDATION_ERROR",
 *   "message": "Invalid request parameters",
 *   "details": [
 *     { "field": "latitude", "error": "Must be between -90 and 90" }
 *   ],
 *   "requestId": "req-12345"
 * }
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String code;
    private final String message;
    private final List<FieldError> details;
    private final String requestId;

    /**
     * Field-level validation error.
     */
    @Getter
    @AllArgsConstructor
    public static class FieldError {
        private final String field;
        private final String error;
    }

    /**
     * Factory method for validation errors.
     */
    public static ErrorResponse validationError(List<FieldError> errors, String requestId) {
        return builder()
                .code("VALIDATION_ERROR")
                .message("Invalid request parameters")
                .details(errors)
                .requestId(requestId)
                .build();
    }

    /**
     * Factory method for internal errors.
     */
    public static ErrorResponse internalError(String requestId) {
        return builder()
                .code("INTERNAL_ERROR")
                .message("An unexpected error occurred. Please try again later.")
                .requestId(requestId)
                .build();
    }
}
