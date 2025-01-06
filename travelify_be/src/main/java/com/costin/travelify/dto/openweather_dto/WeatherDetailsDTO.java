package com.costin.travelify.dto.openweather_dto;

import lombok.Data;

import java.util.List;

@Data
public class WeatherDetailsDTO {
    private long dt;
    private long sunrise;
    private long sunset;
    private double temp;
    private double feels_like;
    private int pressure;
    private int humidity;
    private double dew_point;
    private int clouds;
    private double wind_speed;
    private double wind_deg;
    private List<WeatherDescriptionDTO> weather;
}
