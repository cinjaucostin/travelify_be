package com.costin.travelify.dto.openweather_dto;

import lombok.Data;

@Data
public class TemperatureSummaryDTO {
    private double day;
    private double night;
    private double eve;
    private double morn;
}
