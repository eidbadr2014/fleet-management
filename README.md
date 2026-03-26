# Data Ingest Lambda (Java)

IoT telemetry ingestion service for the Fleet Management Platform.

## Overview

This Lambda function:
1. **Validates** incoming sensor data against schema constraints
2. **Normalizes** data (lowercase deviceId, location precision)
3. **Persists** to AWS Timestream for time-series storage


## Quick Start

### Build
```bash
mvn clean package
```

### Run Tests
```bash
mvn test
```

### Deploy to AWS Lambda
```bash
# Upload target/data-ingest-1.0.0.jar to Lambda
# Handler: com.example.fleet.handler.DataIngestHandler::handleRequest
# Runtime: Java 17
```

## Ingestion Flow

### How Data Arrives


The Lambda is triggered by **SQS**, which receives messages from the **Rule Engine**.

### Input Payload (from SQS → Lambda)

```json
{
  "deviceId": "truck-001",
  "timestamp": "2026-03-25T10:30:00Z",
  "location": {
    "latitude": 37.7749,
    "longitude": -122.4194,
    "speedKph": 72.7,
    "heading": 270
  },
  "metrics": {
    "fuelLevel": {"value": 85.2, "unit": "percent"},
    "engineTemp": {"value": 90.5, "unit": "celsius"},
    "batteryVoltage": {"value": 12.8},
    "oilPressure": {"value": 3.5},
    "tirePressure": {"value": 2.4}
  }
}
```

### Output (Lambda Response)

Success:
```json
{
  "status": "OK",
  "message": "Telemetry ingested successfully",
  "deviceId": "truck-001",
  "timestamp": "2026-03-25T10:30:00Z"
}
```

Validation Error:
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request parameters",
  "details": [
    { "field": "location.latitude", "error": "Must be between -90 and 90" }
  ],
  "requestId": "req-12345"
}
```

## Validation Rules

### Required Fields

| Field | Constraint |
|-------|------------|
| deviceId | Pattern: `^[a-z0-9-]{3,64}$` |
| timestamp | ISO 8601 Instant, not in future (1 min skew allowed) |
| location | Required object |
| location.latitude | -90 to 90 |
| location.longitude | -180 to 180 |

### Optional Fields

| Field | Constraint |
|-------|------------|
| location.speedKph | >= 0 |
| location.heading | 0 to 360 |
| metrics | Optional map of metric values |

### Known Metrics (with validation)

| Metric | Range | Unit |
|--------|-------|------|
| fuelLevel | 0-100 | percent |
| engineTemp | -40 to 200 | celsius |
| batteryVoltage | 0-48 | volts |
| oilPressure | 0-10 | bar |
| tirePressure | 0-10 | bar |

> Unknown metrics are allowed through for extensibility.

## Normalization

| Field | Transformation |
|-------|----------------|
| deviceId | lowercase, trimmed |
| location.latitude/longitude | rounded to 6 decimals (11cm precision) |
| location.speedKph | rounded to 2 decimals |
| location.heading | unchanged (integer) |
| metrics.* | precision preserved (no rounding) |

## Data Model

```
SensorDataRequest
├── deviceId: String
├── timestamp: Instant
├── location: LocationData
│   ├── latitude: Double
│   ├── longitude: Double
│   ├── speedKph: Double (optional)
│   └── heading: Integer (optional)
└── metrics: Map<String, MetricValue>
    └── MetricValue
        ├── value: Double
        └── unit: String (optional)
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `TIMESTREAM_DATABASE` | `fleet_telemetry` | Timestream database name |
| `TIMESTREAM_TABLE` | `device_metrics` | Timestream table name |