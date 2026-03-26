package com.example.fleet.repository;

import com.example.fleet.config.TimestreamConfig;
import com.example.fleet.model.LocationData;
import com.example.fleet.model.ValidatedTelemetry;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.timestreamwrite.TimestreamWriteClient;
import software.amazon.awssdk.services.timestreamwrite.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Timestream implementation of TelemetryRepository.
 * 
 * Writes telemetry data to AWS Timestream for time-series storage.
 * 
 * Table schema:
 * - Dimensions: deviceId (partition key)
 * - Measures: location fields + dynamic metrics from Map
 * - Time: telemetry timestamp
 */
@RequiredArgsConstructor
public class TimestreamTelemetryRepository implements TelemetryRepository {

    private final TimestreamWriteClient client;
    private final TimestreamConfig config;

    @Override
    public void save(ValidatedTelemetry telemetry) {
        List<Record> records = buildRecords(telemetry);

        WriteRecordsRequest request = WriteRecordsRequest.builder()
                .databaseName(config.database())
                .tableName(config.table())
                .records(records)
                .build();

        client.writeRecords(request);
    }

    /**
     * Build Timestream records from telemetry.
     * 
     * Uses MULTI measure type to write multiple metrics in a single record.
     */
    private List<Record> buildRecords(ValidatedTelemetry telemetry) {
        List<Record> records = new ArrayList<>();

        // Common dimensions for all measures
        Dimension deviceDimension = Dimension.builder()
                .name("deviceId")
                .value(telemetry.getDeviceId())
                .dimensionValueType(DimensionValueType.VARCHAR)
                .build();

        List<Dimension> dimensions = List.of(deviceDimension);

        // Timestamp in milliseconds
        String timeMillis = String.valueOf(telemetry.getTimestamp().toEpochMilli());

        // Build measure values list
        List<MeasureValue> measureValues = new ArrayList<>();

        // Add location measures
        addLocationMeasures(telemetry.getLocation(), measureValues);

        // Add dynamic metrics from Map
        addMetricMeasures(telemetry.getMetrics(), measureValues);

        // Create MULTI record
        Record record = Record.builder()
                .dimensions(dimensions)
                .measureName("telemetry")
                .measureValueType(MeasureValueType.MULTI)
                .measureValues(measureValues)
                .time(timeMillis)
                .timeUnit(TimeUnit.MILLISECONDS)
                .build();

        records.add(record);
        return records;
    }

    /**
     * Add location-related measures to the list.
     */
    private void addLocationMeasures(LocationData location, List<MeasureValue> measureValues) {
        if (location == null) {
            return;
        }

        // Latitude and longitude are always present in validated location
        measureValues.add(MeasureValue.builder()
                .name("latitude")
                .value(String.valueOf(location.getLatitude()))
                .type(MeasureValueType.DOUBLE)
                .build());

        measureValues.add(MeasureValue.builder()
                .name("longitude")
                .value(String.valueOf(location.getLongitude()))
                .type(MeasureValueType.DOUBLE)
                .build());

        // Optional location fields
        if (location.getSpeedKph() != null) {
            measureValues.add(MeasureValue.builder()
                    .name("speedKph")
                    .value(String.valueOf(location.getSpeedKph()))
                    .type(MeasureValueType.DOUBLE)
                    .build());
        }

        if (location.getHeading() != null) {
            measureValues.add(MeasureValue.builder()
                    .name("heading")
                    .value(String.valueOf(location.getHeading()))
                    .type(MeasureValueType.BIGINT)
                    .build());
        }
    }

    /**
     * Add dynamic metrics from the Map to the measure list.
     * This allows arbitrary metrics to be written to Timestream.
     */
    private void addMetricMeasures(Map<String, Double> metrics, List<MeasureValue> measureValues) {
        if (metrics == null || metrics.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Double> entry : metrics.entrySet()) {
            if (entry.getValue() != null) {
                measureValues.add(MeasureValue.builder()
                        .name(entry.getKey())
                        .value(String.valueOf(entry.getValue()))
                        .type(MeasureValueType.DOUBLE)
                        .build());
            }
        }
    }

    /**
     * Factory method - loads config from environment variables.
     */
    public static TimestreamTelemetryRepository create(TimestreamWriteClient client) {
        return new TimestreamTelemetryRepository(client, TimestreamConfig.fromEnvironment());
    }
}
