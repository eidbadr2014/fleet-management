package com.example.fleet.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.example.fleet.model.ApiResponse;
import com.example.fleet.model.ErrorResponse;
import com.example.fleet.model.SensorDataRequest;
import com.example.fleet.repository.TimestreamTelemetryRepository;
import com.example.fleet.service.NormalizationService;
import com.example.fleet.service.TelemetryService;
import com.example.fleet.model.IngestResult;
import com.example.fleet.service.ValidationService;
import com.example.fleet.util.ObjectMapperFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.timestreamwrite.TimestreamWriteClient;

import java.util.Map;

/**
 * AWS Lambda handler for telemetry data ingestion.
 * 
 * Entry point: IoT Core Rules → Lambda (or API Gateway for testing)
 * 
 * Flow:
 * 1. Parse JSON body
 * 2. Validate and normalize
 * 3. Persist to Timestream
 * 4. Return response
 * 
 * HTTP Responses:
 * - 200: Success
 * - 400: Validation error
 * - 500: Internal error
 * 
 * Note: No CORS headers needed - this handler receives requests from IoT devices
 * via IoT Core, not from browsers.
 */
public class DataIngestHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Map<String, String> RESPONSE_HEADERS = Map.of(
            "Content-Type", "application/json"
    );

    private final ObjectMapper objectMapper;
    private final TelemetryService telemetryService;

    /**
     * Default constructor for Lambda runtime.
     * Initializes dependencies using environment variables.
     */
    public DataIngestHandler() {
        this.objectMapper = ObjectMapperFactory.create();
        
        TimestreamWriteClient timestreamClient = TimestreamWriteClient.create();
        TimestreamTelemetryRepository repository = TimestreamTelemetryRepository.create(timestreamClient);
        
        this.telemetryService = new TelemetryService(
                new ValidationService(),
                new NormalizationService(),
                repository
        );
    }

    /**
     * Constructor for testing (dependency injection).
     */
    public DataIngestHandler(ObjectMapper objectMapper, TelemetryService telemetryService) {
        this.objectMapper = objectMapper;
        this.telemetryService = telemetryService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        String requestId = context != null ? context.getAwsRequestId() : "test-request-id";
        
        try {
            // Parse request body
            String body = event.getBody();
            if (body == null || body.isBlank()) {
                return badRequest(
                        ErrorResponse.validationError(
                                java.util.List.of(new ErrorResponse.FieldError("body", "Request body is required")),
                                requestId
                        )
                );
            }

            SensorDataRequest request = objectMapper.readValue(body, SensorDataRequest.class);

            // Process telemetry
            IngestResult result = telemetryService.ingest(request);

            if (!result.isSuccess()) {
                return badRequest(ErrorResponse.validationError(result.getErrors(), requestId));
            }

            // Success response
            ApiResponse response = ApiResponse.success(
                    result.getTelemetry().getDeviceId(),
                    result.getTelemetry().getTimestamp().toString()
            );

            return success(response);

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return badRequest(
                    ErrorResponse.builder()
                            .code("INVALID_JSON")
                            .message("Invalid JSON format: " + e.getOriginalMessage())
                            .requestId(requestId)
                            .build()
            );
        } catch (Exception e) {
            logError(context, "Unexpected error", e);
            return internalError(ErrorResponse.internalError(requestId));
        }
    }

    private APIGatewayProxyResponseEvent success(ApiResponse response) {
        return buildResponse(200, response);
    }

    private APIGatewayProxyResponseEvent badRequest(ErrorResponse error) {
        return buildResponse(400, error);
    }

    private APIGatewayProxyResponseEvent internalError(ErrorResponse error) {
        return buildResponse(500, error);
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, Object body) {
        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(statusCode)
                    .withHeaders(RESPONSE_HEADERS)
                    .withBody(objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            // Fallback if serialization fails
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(RESPONSE_HEADERS)
                    .withBody("{\"code\":\"INTERNAL_ERROR\",\"message\":\"Response serialization failed\"}");
        }
    }

    private void logError(Context context, String message, Exception e) {
        if (context != null) {
            context.getLogger().log(message + ": " + e.getMessage());
        } else {
            System.err.println(message + ": " + e.getMessage());
        }
    }
}
