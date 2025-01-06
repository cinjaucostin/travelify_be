package com.costin.travelify.dto.arcgis_dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class RouteAttributesDTO {
    @JsonProperty("ObjectID")
    private Integer objectId;
    @JsonProperty("Name")
    private String name;
    @JsonProperty("FirstStopID")
    private Integer firstStopId;
    @JsonProperty("LastStopID")
    private Integer lastStopId;
    @JsonProperty("StopCount")
    private Integer stopCount;
    @JsonProperty("Total_TravelTime")
    private Double totalTravelTime;
    @JsonProperty("Total_Miles")
    private Double totalMiles;
    @JsonProperty("Total_Kilometers")
    private Double totalKilometers;
    @JsonProperty("Shape_Length")
    private Double shapeLength;
}
