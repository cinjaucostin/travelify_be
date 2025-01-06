package com.costin.travelify.dto.openweather_dto;

import lombok.Data;

import java.util.List;

@Data
public class WeatherCurrentDTO {
    private double lat;
    private double lon;
    private String timezone;
    private int timezone_offset;
    private WeatherDetailsDTO current;
    private List<DailyWeatherDTO> daily;
}
