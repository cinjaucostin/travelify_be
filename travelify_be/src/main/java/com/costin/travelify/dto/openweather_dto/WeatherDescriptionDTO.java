package com.costin.travelify.dto.openweather_dto;

import lombok.Data;

@Data
public class WeatherDescriptionDTO {
    private String main;
    private String description;
    private String icon;
}
