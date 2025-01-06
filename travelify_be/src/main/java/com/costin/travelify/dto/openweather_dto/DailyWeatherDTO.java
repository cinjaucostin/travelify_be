package com.costin.travelify.dto.openweather_dto;

import lombok.Data;

import java.util.List;

@Data
public class DailyWeatherDTO {
    private long dt;
    private long sunrise;
    private long sunset;
    private int pressure;
    private int humidity;
    private double dew_point;
    private int clouds;
    private double wind_speed;
    private double wind_deg;
    private List<WeatherDescriptionDTO> weather;
    private TemperatureSummaryDTO temp;
    private TemperatureSummaryDTO feels_like;
}
