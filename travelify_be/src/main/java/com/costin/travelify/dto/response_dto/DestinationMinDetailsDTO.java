package com.costin.travelify.dto.response_dto;

import lombok.Data;

@Data
public class DestinationMinDetailsDTO {
    private int id;
    private String name;
    private String addressPath;
    private double rating;
    private int reviews;
    private String imageSource;
    private double popularity;
    private int temperature;
}
