package com.costin.travelify.dto.response_dto;

import lombok.Data;

@Data
public class WeatherDTO {
    private String destinationName;
    private int temperature;
    private String weatherDescription;
    private String weatherIcon;
    private String sunrise;
    private String sunset;
}
