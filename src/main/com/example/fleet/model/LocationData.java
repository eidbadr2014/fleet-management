package com.example.fleet.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GPS location data from IoT devices.
 * 
 * All location-related fields are grouped here for cleaner structure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocationData {

    private Double latitude;   // -90 to 90
    private Double longitude;  // -180 to 180
    private Double speedKph;   // >= 0, optional
    private Integer heading;   // 0 to 360, optional
}
