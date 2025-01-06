package com.costin.travelify.dto.openweather_dto;

import lombok.Data;

import java.util.List;

@Data
public class WeatherForecastDTO {
    private double lat;
    private double lon;
    private String timezone;
    private int timezone_offset;
    private List<WeatherDetailsDTO> data;
}
