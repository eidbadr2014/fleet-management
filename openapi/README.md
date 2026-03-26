# Fleet Management API - OpenAPI Specification

This directory contains the modular OpenAPI 3.0.3 specification for the Fleet Management IoT Platform API.

## Directory Structure

```
openapi/
├── openapi.yaml                    # Main entry point (references other files)
├── README.md                       # This file
├── paths/
│   ├── devices.yaml                # POST /devices, GET /devices/{id}
│   ├── device-locations.yaml       # Location endpoints
│   └── device-sensors.yaml         # Sensor telemetry endpoints
└── components/
    ├── schemas/
    │   ├── device.yaml             # Device, CreateDeviceRequest
    │   ├── deviceTypes.yaml        # DeviceIdType, DeviceNameType, DeviceMetadataType
    │   ├── location.yaml           # Location, LocationHistory, LatestPosition
    │   ├── sensor.yaml             # SensorReading, SensorHistory
    │   └── error.yaml              # Error response schema
    ├── parameters/
    │   ├── deviceId.yaml           # DeviceId path parameter
    │   ├── time.yaml               # FromTime, ToTime
    │   └── pagination.yaml         # Limit
    ├── responses/
    │   └── errors.yaml             # BadRequest, Unauthorized, NotFound, etc.
    └── securitySchemes.yaml        # BearerAuth (JWT)
```

## Usage

### Preview Documentation
 
```bash
# Using Redocly CLI
npx @redocly/cli preview-docs openapi.yaml

# Build static HTML
npx @redocly/cli build-docs openapi.yaml -o ../docs/api.html
```

### Validate Specification

```bash
npx @redocly/cli lint openapi.yaml
```

### Bundle into Single File

For tools that don't support `$ref` to external files:

```bash
npx @redocly/cli bundle openapi.yaml -o openapi-bundled.yaml
```

### Swagger UI (Docker)

```bash
docker run -p 8080:8080 \
  -e SWAGGER_JSON=/api/openapi.yaml \
  -v $(pwd):/api \
  swaggerapi/swagger-ui
```

Then open http://localhost:8080

## Adding New Endpoints

1. Create or update the appropriate file in `paths/`
2. Add schema definitions to `components/schemas/`
3. Add the path reference to `openapi.yaml`
4. Run `npx @redocly/cli lint openapi.yaml` to validate

## Adding New Components

### New Schema
1. Create file in `components/schemas/your-schema.yaml`
2. Add `$ref` to `openapi.yaml` components section

### New Parameter
1. Add to existing file or create new in `components/parameters/`
2. Reference with `$ref: '../components/parameters/file.yaml#/ParamName'`

### New Response
1. Add to `components/responses/errors.yaml` or create new file
2. Reference with `$ref: '../components/responses/errors.yaml#/ResponseName'`

## Best Practices

- Keep each schema in its own file or group related schemas together
- Use `$ref` for all reusable components
- Include examples in schemas and responses
- Document all parameters with descriptions
- Use consistent naming (kebab-case for files, PascalCase for schemas)
